# ESM script authoring

Cosmic supports three JavaScript script modes:

- Legacy Script mode for existing `.js` files with global callbacks.
- ECMAScript module mode (ESM) for `.mjs` files and `.js` files that opt in with top-level static `import` or `export`.
- CommonJS mode for `.cjs` files and `.js` files inside a nearest `package.json` with `"type": "commonjs"`.

Dynamic `import()`, top-level `await`, WASM imports, URL imports, asynchronous Node APIs, and promise-based Node APIs are not supported. The only importable or require-able Node built-ins are `fs`/`node:fs`, `path`/`node:path`, and `module`/`node:module`. A Node-like `process` object is provided globally and through `process.getBuiltinModule("process")`.

## Entry classification

An entry script is classified by its filename and top-level syntax:

- `.mjs` is always ESM.
- `.cjs` is always CommonJS.
- When a nearest parent `package.json` exists, `.js` follows Node's `"type"` field: `"module"` means ESM, `"commonjs"` means CommonJS.
- When no parent `package.json` exists, `.js` keeps the original Cosmic behavior: top-level static `import` or `export` means ESM, otherwise legacy Script mode.

Comments, strings, template literals, object property names, and nested function/class bodies do not opt a `.js` file into ESM. A `.js` file classified as ESM is loaded as ESM and will not fall back to legacy mode if it has a syntax or import error.

Existing callers that pass an extensionless script id still resolve to `.js`. For example, `9000000` resolves to `9000000.js`. To select an `.mjs` or `.cjs` entry, pass the explicit filename such as `9000000.mjs` or `9000000.cjs`.

## Legacy Script mode

Legacy scripts keep the existing global objects and callback signatures. Existing scripts do not need to change.

```js
// Legacy scripts/npc/9000000.js
function start() {
  cm.sendOk("Hello.");
}
```

In legacy mode, managers inject globals such as `cm`, `im`, `qm`, `rm`, `pi`, `msm`, and `em` as they did before.

Legacy scripts also expose REPL-like `globalThis.require` and `globalThis.module` globals. This is only for legacy scripts; ESM does not receive global `require` or `module`, and CommonJS receives only its normal local `require` and `module` wrapper variables. The legacy global `require` can load the supported built-in modules described below.

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

## CommonJS Mode

CommonJS callbacks are exported properties. The first argument is the same `ctx` object used by ESM callbacks.

```js
// CommonJS scripts/npc/9000000.cjs
const rewards = require("../lib/rewards.cjs");

exports.start = function (ctx) {
  if (rewards.canReward(ctx.cm)) {
    ctx.cm.sendOk("Hello.");
  }
};
```

## Static imports

ESM supports JavaScript static imports with explicit file paths and bare package specifiers:

```js
import { reward } from "./reward.js";
import { shared } from "../shared/helpers.mjs";
import { absoluteHelper } from "/opt/cosmic/scripts/helpers/absolute-helper.mjs";
import { helper } from "my-helper-package";
```

Rules:

- `./` and `../` resolve from the importing file.
- Filesystem absolute paths are allowed.
- Bare package imports resolve through `node_modules` by walking up from the importing file.
- Package `exports`, `main`, and `index.js`/`index.mjs`/`index.cjs` are supported for JavaScript packages.
- Imported `.js` files follow nearest `package.json` `"type"` when present. Without `package.json`, they keep Cosmic's syntax-based classification.
- The imported file path may omit `.js`, `.json`, `.mjs`, or `.cjs` in CommonJS-style resolution.
- ESM JSON imports must use import attributes: `import data from "./data.json" with { type: "json" };`.
- Supported built-ins may be imported with either bare or `node:` specifiers, such as `import fs from "fs"` or `import path from "node:path"`.
- Unsupported built-ins and other `node:` specifiers are rejected.

Side-effect-only modules are allowed:

```js
// scripts/lib/register-drop-rates.js
globalThis.dropRateBootCount = (globalThis.dropRateBootCount ?? 0) + 1;
```

## CommonJS Interop

CommonJS scripts use `exports`, `module.exports`, synchronous `require(...)`, `__filename`, and `__dirname`:

```js
// scripts/npc/example.cjs
const rewards = require("../lib/rewards.cjs");
const packageName = "some-package";
const helper = require(packageName);

exports.start = function (ctx) {
  ctx.cm.sendOk(rewards.message + helper.suffix);
};
```

Interop rules:

- CommonJS `__filename` is the resolved absolute file path, and `__dirname` is its parent directory.
- CommonJS `require` and `module` are local wrapper variables, not `globalThis` properties.
- ESM can import CommonJS default exports and statically recognizable named exports such as `exports.foo = ...` or `module.exports.foo = ...`.
- CommonJS `require()` can load CommonJS modules and synchronous ESM modules. ESM modules with top-level `await` are rejected before load.
- CommonJS `require()` can load JSON files, including extensionless paths that resolve to `.json`.
- `require()` resolves both literal and computed string specifiers at runtime, using the requesting module's path and the same `node_modules`/`package.json` rules described above.
- `require.resolve(specifier)` returns the resolved real file path.
- `require.cache` exposes loaded CommonJS module records keyed by resolved real file path.
- CommonJS and JSON modules are cached by resolved real path, so repeated `require()` calls return the same `module.exports` object. ESM JSON imports share the JSON cache object.
- Cosmic does not add a script-visible CJS runtime global.
- Built-in Node modules other than `fs`/`node:fs`, `path`/`node:path`, and `module`/`node:module` are not available through `require()` or ESM `import`.

All script modes define a Node-like `process` global with `version`, `versions.node`, `arch`, `platform`, `cwd`, `env`, `argv`, and `getBuiltinModule(specifier)`. `process.arch` uses Node's supported architecture names: `arm`, `arm64`, `ia32`, `loong64`, `mips`, `mipsel`, `ppc64`, `riscv64`, `s390x`, or `x64`. `process.platform` uses Node's supported platform names: `aix`, `android`, `darwin`, `freebsd`, `linux`, `openbsd`, `sunos`, or `win32`. Unsupported JVM architecture or OS names fail clearly instead of falling back to a fake value. `process.argv` is an array shaped like Node's process arguments: index `0` is the Java executable path and index `1` is the current entry script path when Cosmic knows it. `process.getBuiltinModule()` supports `process`/`node:process`, `fs`/`node:fs`, `path`/`node:path`, and `module`/`node:module`, returning `undefined` for unsupported built-ins. This is also available in legacy scripts, so legacy code can use built-ins without `require()`.

The built-in `fs` module is available through `require("fs")`, `require("node:fs")`, `import fs from "fs"`, `import fs from "node:fs"`, `import * as fs from "fs"`, and `import * as fs from "node:fs"`. Only synchronous file APIs are provided.

The built-in `path` module is available through `require("path")`, `require("node:path")`, `import path from "path"`, `import path from "node:path"`, `import * as path from "path"`, and `import * as path from "node:path"`. Built-in modules are Java-consumed CommonJS resources under `scripting/builtins/`, with `path.cjs` bundled from `@std/path@1.1.5` by Rollup.

The built-in `module` module is available through `require("module")`, `require("node:module")`, `import { createRequire } from "module"`, and `import { createRequire } from "node:module"`. `createRequire(import.meta.url)` creates a CommonJS `require` bound to the source module path. As in Node.js, passing a directory URL or directory path creates the require relative to that value as a filename, so relative lookups resolve from its parent directory.

ESM modules provide Node-style local metadata:

```js
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const data = require("./data.json");

export function start(ctx) {
  ctx.cm.sendOk(import.meta.filename + " " + import.meta.dirname + " " + import.meta.resolve("./data.json"));
}
```

- `import.meta.filename` is the resolved absolute file path.
- `import.meta.dirname` is the parent directory of `import.meta.filename`.
- `import.meta.url` is the `file:` URL for the source module.
- `import.meta.resolve(specifier)` returns the resolved `file:` URL string, or a `node:` URL for supported built-ins.
- `import { createRequire } from "module"` and `import { createRequire } from "node:module"` are supported.

```js
// scripts/npc/example.mjs
import "../lib/register-drop-rates.js";

export function start(ctx) {
  ctx.cm.sendOk("Loaded.");
}
```

## State and cache scope

Module state follows the same scope as the old script engine for each manager:

| Script kind | Scope |
| --- | --- |
| NPC, item, quest, reactor | One script context per `Client` |
| Portal | Shared process-wide portal script cache |
| Map | Shared process-wide map script cache |
| Event | Per channel event instance |
| devtest | Per command invocation |

Within a script context, each imported ESM module is instantiated once, and each CommonJS or JSON module is cached by resolved real file path. Client-scoped scripts do not share module state between players.

## Migration checklist

1. Rename the entry to `.mjs`, or add a top-level `export` to an existing `.js` entry.
2. Export the callback names the manager expects.
3. Replace globals with `ctx` properties.
4. Move shared helpers into imported `.js`, `.mjs`, or `.cjs` files with explicit relative or absolute paths, or publish them under `node_modules`.
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
