# ESM script authoring

Cosmic supports two JavaScript script modes:

- Legacy Script mode for existing `.js` files with global callbacks.
- ECMAScript module mode (ESM) for `.mjs` files and `.js` files that opt in with top-level static `import` or `export`.

CommonJS is not supported. Do not use `require`, `module.exports`, Node built-ins, `node:` specifiers, dynamic `import()`, top-level `await`, bare package imports, `node_modules`, JSON imports, WASM imports, or URL imports.

## Entry classification

An entry script is classified by its filename and top-level syntax:

- `.mjs` is always ESM.
- `.js` is ESM only when the entry file contains a top-level static `import` declaration or `export` declaration.
- `.js` without module syntax stays legacy Script mode.

Comments, strings, template literals, object property names, and nested function/class bodies do not opt a `.js` file into ESM. A `.js` file classified as ESM is loaded as ESM and will not fall back to legacy mode if it has a syntax or import error.

Existing callers that pass an extensionless script id still resolve to `.js`. For example, `9000000` resolves to `9000000.js`. To select an `.mjs` entry, pass the explicit filename such as `9000000.mjs`.

## Legacy Script mode

Legacy scripts keep the existing global objects and callback signatures. Existing scripts do not need to change.

```js
// Legacy scripts/npc/9000000.js
function start() {
  cm.sendOk("Hello.");
}
```

In legacy mode, managers inject globals such as `cm`, `im`, `qm`, `rm`, `pi`, `msm`, and `em` as they did before.

## ESM mode

ESM callbacks are named exports. The first argument is always `ctx`, and host objects live under `ctx`.

```js
// ESM scripts/npc/9000000.mjs
import { canReward } from "../lib/rewards.js";

export function start(ctx) {
  if (canReward(ctx.cm)) {
    ctx.cm.sendOk("Hello.");
  }
}
```

Typical callback shapes:

| Script kind | Host property | ESM callbacks |
| --- | --- | --- |
| NPC | `ctx.cm` | `start(ctx)`, `start(ctx, chr)`, `action(ctx, mode, type, selection)` |
| Item | `ctx.im` | `start(ctx)`, `action(ctx, mode, type, selection)` |
| Quest | `ctx.qm` | `start(ctx, mode, type, selection)`, `end(ctx, mode, type, selection)`, `raiseOpen(ctx)` |
| Reactor | `ctx.rm` | `hit(ctx)`, `act(ctx)`, `touch(ctx)`, `untouch(ctx)` |
| Portal | `ctx.pi` | `enter(ctx)` returning `true` or `false` |
| Map | `ctx.msm` | `start(ctx)` |
| Event | `ctx.em` | `init(ctx, value)`, scheduled callbacks as `callback(ctx, eim)`, plus existing event callback names with `ctx` first |
| devtest | none by default | `run(ctx, player)` |

There are no implicit ESM globals for `cm`, `qm`, `rm`, `em`, portal interaction, or map methods. Pass `ctx` or a specific host object into helper functions explicitly.

## Static imports

ESM supports JavaScript static imports with explicit file paths:

```js
import { reward } from "./reward.js";
import { shared } from "../shared/helpers.mjs";
import { absoluteHelper } from "/opt/cosmic/scripts/helpers/absolute-helper.mjs";
```

Rules:

- `./` and `../` resolve from the importing file.
- Filesystem absolute paths are allowed.
- Imported `.js` files are parsed as ESM, even if they contain no `import` or `export`.
- The imported file path must explicitly end in `.js` or `.mjs`.
- Bare specifiers such as `"lodash"` and `node:` specifiers are rejected.

Side-effect-only modules are allowed:

```js
// scripts/lib/register-drop-rates.js
globalThis.dropRateBootCount = (globalThis.dropRateBootCount ?? 0) + 1;
```

```js
// scripts/npc/example.mjs
import "../lib/register-drop-rates.js";

export function start(ctx) {
  ctx.cm.sendOk("Loaded.");
}
```

## State and cache scope

ESM module state follows the same scope as the old script engine for each manager:

| Script kind | Scope |
| --- | --- |
| NPC, item, quest, reactor | One script context per `Client` |
| Portal | Shared process-wide portal script cache |
| Map | Shared process-wide map script cache |
| Event | Per channel event instance |
| devtest | Per command invocation |

Within a script context, each imported module is instantiated once. Client-scoped scripts do not share module state between players.

## Migration checklist

1. Rename the entry to `.mjs`, or add a top-level `export` to an existing `.js` entry.
2. Export the callback names the manager expects.
3. Replace globals with `ctx` properties.
4. Move shared helpers into imported `.js` or `.mjs` files with explicit relative or absolute paths.
5. Keep callback execution synchronous; do not use top-level `await` or dynamic `import()`.

Legacy:

```js
function start() {
  cm.sendOk("Hello.");
}

function action(mode, type, selection) {
  cm.dispose();
}
```

ESM:

```js
export function start(ctx) {
  ctx.cm.sendOk("Hello.");
}

export function action(ctx, mode, type, selection) {
  ctx.cm.dispose();
}
```
