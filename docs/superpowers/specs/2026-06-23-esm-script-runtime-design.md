# ESM Script Runtime Design

## Goal

Add standards-based ECMAScript module (ESM) support to the game script runtime while preserving every existing JavaScript Script-mode entry point, callback signature, cache scope, and lifecycle.

## Scope

This design applies to scripts loaded by the NPC/item, quest, reactor, portal, map, event, and `devtest` script managers.

Included:

- Native ESM entry points and static imports.
- ESM named exports as the callback contract.
- Relative and filesystem-absolute JavaScript imports.
- ESM modules that contain no import/export declarations and run only for side effects.
- Existing legacy Script-mode scripts without source changes.
- Context lifecycle and caching that exactly mirrors the current manager-specific behavior.

Explicitly excluded:

- CommonJS (`require`, `module.exports`).
- Node.js built-ins, `node:` specifiers, npm/bare-package resolution, and `node_modules`.
- Dynamic `import()`.
- Top-level `await`.
- JSON, WASM, and import-attribute loaders.
- Automatic conversion of existing scripts to ESM.

## Compatibility Contract

### Entry classification

The runtime classifies only an entry script; a file reached through ESM static import is always evaluated as an ESM module.

1. A `.mjs` entry is always ESM.
2. A `.js` entry is ESM only when its top-level lexical structure includes a static `import` declaration or an `export` declaration.
3. Every other `.js` entry is legacy Script mode.

Classification must use a lexer aware of comments, strings, template literals, and nesting. It must not use a regular expression: player-facing text, comments, or property names containing `import`/`export` must not change execution mode. A `.js` entry that is classified as ESM but fails to parse must fail as ESM; it must never silently fall back to legacy mode.

`import()` is not a static import declaration and therefore does not classify a `.js` entry as ESM. It is rejected when present in an ESM module. Top-level `await` is also rejected in ESM modules so script invocation remains synchronous.

### Entry path resolution

Existing callers commonly pass an extensionless logical script name and managers currently append `.js`. The new shared `ScriptPathResolver` preserves that behavior:

- An extensionless identifier `foo` resolves to `foo.js`.
- An identifier ending in `.js` or `.mjs` is used exactly as supplied.

Thus all existing data/configuration continues to select its legacy `foo.js` script. New `.mjs` entry scripts are selected by explicitly using `foo.mjs`; a `.js` entry can opt into ESM in-place by adding static module syntax.

### ESM import resolution

ESM follows host-provided file resolution, with no project-root sandbox:

- `./` and `../` specifiers resolve from the importing module's directory, including paths outside `scripts/`.
- Filesystem-absolute specifiers resolve directly.
- Specifiers must name a JavaScript file explicitly; this release does not add extension inference or package resolution.
- Bare specifiers, `node:` specifiers, JSON, WASM, and URL/network schemes are rejected.
- A module imported from ESM is parsed as ESM whether it is named `.js` or `.mjs`; it may legally contain no imports or exports.
- Module identity is its normalized real absolute filesystem path. The same file is instantiated once per Polyglot context, including when reached through syntactically different paths. Native ESM linking handles cyclic dependencies.

No `scripts/` traversal restriction is imposed. This is an intentional host policy chosen for script flexibility and portability of standard relative module structure.

## Callback API

### Legacy Script mode

Legacy scripts keep their exact current behavior. Managers inject `cm`, `qm`, `rm`, and `em` into the ScriptEngine global scope as today, and invoke global callbacks with the current argument lists. No legacy script must be edited.

### ESM mode

ESM callbacks are named exports. The first parameter is always `ctx`; remaining parameters retain the old callback's business arguments.

```js
// NPC
export function start(ctx) {
  ctx.cm.sendOk("Hello.");
}

export function action(ctx, mode, type, selection) {
  ctx.cm.dispose();
}
```

`ctx` is a fresh host-provided JavaScript object for each callback invocation. Its members are:

| Script kind | Context property | ESM callback shape |
| --- | --- | --- |
| NPC | `ctx.cm` | `start(ctx)`, `action(ctx, mode, type, selection)` |
| Item | `ctx.im` or `ctx.cm`, matching the existing item flow | Existing callback name with `ctx` first |
| Quest | `ctx.qm` | `start(ctx, mode, type, selection)`, `end(ctx, mode, type, selection)`, `raiseOpen(ctx)` |
| Reactor | `ctx.rm` | `hit(ctx)`, `act(ctx)`, `touch(ctx)`, `untouch(ctx)` |
| Portal | `ctx.pi` | `enter(ctx)` |
| Map | `ctx.msm` | `start(ctx)` |
| Event | `ctx.em` | `init(ctx)` and existing event callback names with `ctx` first |
| devtest | no host members | `run(ctx)` |

There are no implicit ESM globals for `cm`, `qm`, `rm`, `em`, portal interaction, or map methods. Imported helpers receive `ctx` or the precise host object explicitly. This prevents hidden dependencies and makes modules reusable.

## Architecture

### Unified invocation boundary

Managers must stop exposing `ScriptEngine` / `Invocable` as their execution boundary and use a common `ScriptHandle` abstraction instead:

```java
interface ScriptHandle extends AutoCloseable {
    Object invoke(String callback, ScriptInvocationContext context, Object... arguments)
            throws ScriptException, NoSuchMethodException;

    boolean hasCallback(String callback);

    @Override
    void close();
}
```

The manager always calls `invoke(callback, ctx, args...)`.

- `LegacyScriptHandle` wraps the existing `ScriptEngine`/`Invocable`, binds legacy globals, drops `ctx`, and invokes the legacy global callback with only `args`.
- `EsmScriptHandle` owns a Graal Polyglot `Context`, evaluates the entry as module `Source`, and invokes the named export with the JavaScript `ctx` object followed by `args`.

`ScriptInvocationContext` creates the ESM `ctx` value with `ProxyObject.fromMap(...)`; property lookup such as `ctx.cm` returns the existing Java host object. The map is newly created for each invocation and is not a global binding.

Portal must replace its `Invocable.getInterface(PortalScript.class)` use with `ScriptHandle.invoke("enter", ctx)`, validating the return value remains boolean. `EventManager`, which currently receives an `Invocable` and invokes callbacks itself, must receive the equivalent handle/callback adapter so all event calls follow the same ESM/legacy contract.

### ESM runtime

`EsmScriptHandle` uses the Graal Polyglot API rather than JSR-223:

- Build a `Context` for language `"js"` with host access and host class lookup matching the current `polyglot.js.allowHostAccess` and `polyglot.js.allowHostClassLookup` behavior.
- Load the entry using a file-backed `Source` marked as `application/javascript+module`, preserving its absolute URI for native relative-module resolution.
- Enable `js.esm-eval-returns-exports=true`; the result is the entry module namespace used to locate named exports.
- Validate and reject unsupported dynamic import and top-level await before evaluation. Produce a source location in the diagnostic.
- Use Graal's ESM linker/cache within that context for all static imports and cycles.

The project must align `org.graalvm.js:js` and `org.graalvm.js:js-scriptengine` to one tested GraalJS release compatible with Java 21 before adding the Polyglot runtime. The current 23.0.4/24.0.1 mismatch is not retained.

## Scope, State, and Lifecycle

ESM contexts copy the scope of the corresponding legacy engine; they are not universally per-player:

| Script kind | Current legacy scope | Required ESM scope |
| --- | --- | --- |
| NPC, item, quest, reactor | One context per `Client` | One Polyglot context per `Client` |
| Portal | Process-wide manager cache | Process-wide manager cache |
| Map | Process-wide manager cache | Process-wide manager cache |
| Event | Per channel event instance | Per channel event instance |
| devtest | Per command invocation | Per command invocation |

Consequently, an ESM module and every dependency are instantiated once in the lifetime of that script context. NPC/item/quest/reactor module state is never visible to another client; portal/map/event state has exactly the current shared scope.

Every cache-clear/reload/dispose path must call `ScriptHandle.close()` before dropping its entry. This closes ESM Polyglot contexts and their module graphs at the same moment current ScriptEngine caches are cleared: client disposal/reset for client-scoped scripts, manager reload for portal/map, and reload/cancel/dispose for events.

## Error Handling

Errors must identify the script category, entry path, failing module path, line/column when available, and static import chain.

- Missing module: fail load with the importing module and specifier.
- Parse/link/evaluation error: fail load as the classified mode; no fallback between ESM and legacy.
- Unsupported specifier or feature: fail load with a specific diagnostic (`node:`/bare specifier, dynamic import, top-level await, unsupported file kind).
- Missing exported callback: present the same optional-versus-required behavior as the existing manager. Optional reactor callbacks remain optional; mandatory callbacks retain existing error behavior.
- Export with a non-callable value: fail with a callback-specific diagnostic before host invocation.

## Documentation and Migration

Add a script-author guide that contains:

- Classification and path-resolution rules.
- The legacy global-callback contract and the ESM named-export/`ctx` contract side by side.
- ESM static-import examples, including an absolute import and a side-effect-only `.js` module.
- The unsupported-feature list.
- A migration example for one legacy NPC script.

No game script is converted as part of this runtime feature. A contributor can migrate one entry at a time by adding named exports and changing implicit globals to `ctx` properties.

## Verification

The implementation must add focused tests for:

1. `.mjs` classification; `.js` static import/export classification; comments, strings, and template literals not causing false ESM classification.
2. Extensionless legacy entry resolution and explicit `.js`/`.mjs` entry resolution.
3. A named ESM export receiving `ctx` and remaining callback arguments in the agreed order.
4. Legacy callbacks retaining their original global host objects and argument signatures.
5. Relative imports, absolute imports, and import of a no-export side-effect `.js` module.
6. Normalized-path module caching, cyclic ESM imports, and context-local state isolation.
7. Per-client ESM isolation for NPC/item/quest/reactor and existing shared scope for portal/map/event.
8. Diagnostics for unsupported specifiers, `import()`, top-level await, a missing import, a malformed module, an absent callback export, and a non-function callback export.
9. Context closure after reload/dispose.
10. The full existing `ScriptEvaluationTest` corpus, plus the Maven test suite, on Java 21.

## Acceptance Criteria

- Every existing Script-mode script loads and runs without source or configuration changes.
- An ESM entry can export the callback expected by its manager and access host APIs solely through `ctx`.
- ESM supports static relative and absolute JavaScript imports, including no-export side-effect modules outside `scripts/`.
- ESM does not support CommonJS, Node resolution, `node:`, dynamic import, or top-level await.
- State sharing and release follow exactly the current manager-specific scope and lifecycle.
- Failures are actionable and do not silently switch script modes.
