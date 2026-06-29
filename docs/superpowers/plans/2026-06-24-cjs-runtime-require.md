# CJS Runtime Require Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make CommonJS `require(specifier)` resolve dynamic strings like Node.js while preserving existing legacy Script behavior.

**Architecture:** Keep ESM on Graal module evaluation, but stop compiling CommonJS dependency loading entirely into static generated ESM imports. Generate ESM bridge modules for CommonJS that delegate to a Java-backed runtime CJS loader with a per-context module cache, Node-style resolution, `exports`, `module.exports`, `require`, `__filename`, and `__dirname`.

**Tech Stack:** Java 21, Graal Polyglot JS, JUnit 5.

---

### Task 1: Capture Node-like Dynamic Require Behavior

**Files:**
- Modify: `src/test/java/scripting/ModuleScriptHandleTest.java`

- [ ] **Step 1: Write failing tests**

Add tests for `require("./" + name)`, cache reuse, nested relative dynamic require, and dynamic bare package require.

- [ ] **Step 2: Run targeted test**

Run: `./mvnw -q -Dtest=ModuleScriptHandleTest test`
Expected: FAIL because computed specifiers are not pre-materialized.

### Task 2: Add CommonJS Runtime Loader

**Files:**
- Modify: `src/main/java/scripting/NodeModuleGraph.java`
- Modify: `src/main/java/scripting/ModuleScriptHandle.java`

- [ ] **Step 1: Generate CJS ESM bridges**

For CommonJS modules, generate a small ESM module that calls a Java-owned runtime by id and exports default plus statically recognizable named exports. The runtime must not be exposed through a script-visible global.

- [ ] **Step 2: Implement Java-backed runtime loader**

Add a per-context loader that resolves runtime specifiers from the requesting module path, caches modules by real path, evaluates CJS wrappers synchronously, and reuses existing Node-style path/package resolution.

- [ ] **Step 3: Keep ESM require restrictions clear**

Dynamic CJS `require()` may load CJS at runtime. Requiring ESM remains limited to modules already represented by the generated ESM bridge, and unsupported synchronous ESM cases throw a clear `ScriptLoadException`/JS error.

### Task 3: Verify Existing Behavior

**Files:**
- Modify: `docs/scripting-esm.md`

- [ ] **Step 1: Update docs**

Remove the “literal only” limitation and document dynamic CommonJS `require()`.

- [ ] **Step 2: Run verification**

Run:
`./mvnw -q -Dtest=ModuleScriptHandleTest,AbstractScriptManagerTest,ScriptClassifierTest,ScriptPathResolverTest,ScriptEvaluationTest test`
`git diff --check`
`./mvnw -q test`

Expected: all pass; only existing Mockito/Graal warnings may appear.
