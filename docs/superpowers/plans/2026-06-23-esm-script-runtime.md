# ESM Script Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Support native ESM game scripts with named-export callbacks and per-invocation `ctx`, while every existing legacy Script-mode script continues to run unchanged.

**Architecture:** Replace manager-facing `ScriptEngine`/`Invocable` usage with a `ScriptHandle` abstraction. `LegacyScriptHandle` retains the current JSR-223 behavior; `EsmScriptHandle` uses a Graal Polyglot `Context`, evaluates a module `Source`, and executes named exports. The abstract manager selects the handle from the entry extension and lexical module syntax, preserving the present cache scope of each manager.

**Tech Stack:** Java 21, Maven, JUnit 5, GraalJS JSR-223 (legacy), Graal Polyglot API (ESM).

---

## File Structure

| File | Responsibility |
| --- | --- |
| `pom.xml` | Align GraalJS artifacts to one release and expose Polyglot classes. |
| `src/main/java/scripting/ScriptMode.java` | Distinguish legacy Script and ESM entry modes. |
| `src/main/java/scripting/ScriptClassifier.java` | Classify entry files without false positives in comments/strings/templates. |
| `src/main/java/scripting/ScriptPathResolver.java` | Preserve extensionless `.js` lookup and support explicit `.js`/`.mjs` names. |
| `src/main/java/scripting/ScriptHandle.java` | Manager-facing invocation/lifecycle contract. |
| `src/main/java/scripting/ScriptInvocationContext.java` | Builds the ESM-only per-invocation `ctx` object. |
| `src/main/java/scripting/LegacyScriptHandle.java` | Adapt current ScriptEngine globals/functions to `ScriptHandle`. |
| `src/main/java/scripting/EsmScriptHandle.java` | Evaluate an ESM entry and invoke module exports. |
| `src/main/java/scripting/SynchronizedScriptHandle.java` | Serialize shared Event-context calls. |
| `src/main/java/scripting/AbstractScriptManager.java` | Select/load/cache handles instead of exposing ScriptEngine. |
| `src/main/java/client/Client.java` | Own and close client-scoped handles. |
| `src/main/java/scripting/{npc,quest,reactor,portal,map,event}/...` | Pass `ctx`, retain each manager's existing caching scope, and close handles on reset. |
| `src/main/java/client/command/commands/gm6/DevtestCommand.java` | Load `devtest.js` or explicit `devtest.mjs` through the common runtime. |
| `src/test/java/scripting/...` | Unit and integration coverage for classification, paths, modules, context, lifecycle, and legacy compatibility. |
| `docs/scripting-esm.md` | Script-author migration and API guide. |

### Task 1: Align GraalJS and lock down entry classification/path rules

**Files:**
- Modify: `pom.xml:66-67,264-273`
- Create: `src/main/java/scripting/ScriptMode.java`
- Create: `src/main/java/scripting/ScriptClassifier.java`
- Create: `src/main/java/scripting/ScriptPathResolver.java`
- Create: `src/test/java/scripting/ScriptClassifierTest.java`
- Create: `src/test/java/scripting/ScriptPathResolverTest.java`

- [ ] **Step 1: Write classification tests before adding classifier code**

```java
@ParameterizedTest
@MethodSource("entryModes")
void classifiesEntries(String filename, String source, ScriptMode expected) {
    assertEquals(expected, ScriptClassifier.classify(Path.of(filename), source));
}

private static Stream<Arguments> entryModes() {
    return Stream.of(
        Arguments.of("entry.mjs", "const x = 1;", ScriptMode.ESM),
        Arguments.of("entry.js", "export function start(ctx) {}", ScriptMode.ESM),
        Arguments.of("entry.js", "import { reward } from './reward.js';", ScriptMode.ESM),
        Arguments.of("entry.js", "const text = 'import from a merchant';", ScriptMode.LEGACY),
        Arguments.of("entry.js", "// export function start(ctx) {}", ScriptMode.LEGACY),
        Arguments.of("entry.js", "const text = `export`;", ScriptMode.LEGACY),
        Arguments.of("entry.js", "import('./later.js');", ScriptMode.LEGACY));
}
```

- [ ] **Step 2: Write path resolution tests before adding resolver code**

```java
@Test
void extensionlessEntryDefaultsToJs() {
    assertEquals(Path.of("scripts", "npc", "9000000.js"),
        new ScriptPathResolver(Path.of("scripts")).resolveEntry("npc", "9000000"));
}

@ParameterizedTest
@ValueSource(strings = {"9000000.js", "9000000.mjs"})
void explicitJavaScriptExtensionIsNotAppended(String name) {
    assertEquals(Path.of("scripts", "npc", name),
        new ScriptPathResolver(Path.of("scripts")).resolveEntry("npc", name));
}
```

- [ ] **Step 3: Run the new tests and confirm they fail because the production types do not yet exist**

Run: `./mvnw -Dtest=ScriptClassifierTest,ScriptPathResolverTest test`

Expected: compilation failure naming `ScriptClassifier`, `ScriptMode`, and `ScriptPathResolver`.

- [ ] **Step 4: Add the minimal entry-mode types and lexer**

Create `ScriptMode`:

```java
package scripting;

public enum ScriptMode {
    LEGACY,
    ESM
}
```

Implement `ScriptClassifier.classify(Path entryPath, String source)` with this contract:

```java
if (entryPath.getFileName().toString().endsWith(".mjs")) {
    return ScriptMode.ESM;
}
if (!entryPath.getFileName().toString().endsWith(".js")) {
    throw new IllegalArgumentException("Script entries must end in .js or .mjs: " + entryPath);
}
return hasTopLevelStaticModuleDeclaration(source) ? ScriptMode.ESM : ScriptMode.LEGACY;
```

`hasTopLevelStaticModuleDeclaration` must advance through line comments, block comments, quoted strings, template literals (including `${...}` nesting), and balanced `{}`, `()`, `[]`. It returns true only for a top-level `export` token or a top-level `import` token whose next significant token is not `(` and whose declaration syntax is static.

Implement the resolver:

```java
public Path resolveEntry(String directory, String identifier) {
    String filename = identifier.endsWith(".js") || identifier.endsWith(".mjs")
        ? identifier
        : identifier + ".js";
    return scriptsRoot.resolve(directory).resolve(filename).normalize();
}
```

- [ ] **Step 5: Align Graal dependencies**

Replace the two distinct version properties with one property in `pom.xml` and make both script dependencies use it:

```xml
<graalvm-js.version>24.0.1</graalvm-js.version>
```

```xml
<dependency>
    <groupId>org.graalvm.js</groupId>
    <artifactId>js</artifactId>
    <version>${graalvm-js.version}</version>
</dependency>
<dependency>
    <groupId>org.graalvm.js</groupId>
    <artifactId>js-scriptengine</artifactId>
    <version>${graalvm-js.version}</version>
</dependency>
```

- [ ] **Step 6: Run focused tests and full dependency resolution**

Run: `./mvnw -Dtest=ScriptClassifierTest,ScriptPathResolverTest test`

Expected: PASS.

Run: `./mvnw -DskipTests compile`

Expected: `BUILD SUCCESS` on Java 21.

- [ ] **Step 7: Commit the classification and dependency baseline**

```bash
git add pom.xml src/main/java/scripting/ScriptMode.java src/main/java/scripting/ScriptClassifier.java src/main/java/scripting/ScriptPathResolver.java src/test/java/scripting/ScriptClassifierTest.java src/test/java/scripting/ScriptPathResolverTest.java
git commit -m "feat: classify ESM script entries"
```

### Task 2: Add the handle contract and preserve legacy behavior through it

**Files:**
- Create: `src/main/java/scripting/ScriptHandle.java`
- Create: `src/main/java/scripting/ScriptInvocationContext.java`
- Create: `src/main/java/scripting/LegacyScriptHandle.java`
- Create: `src/test/java/scripting/LegacyScriptHandleTest.java`

- [ ] **Step 1: Write legacy adapter tests**

```java
@Test
void passesOnlyLegacyArgumentsAndInjectsContextValues() throws Exception {
    ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
    engine.eval("function action(mode) { return cm.get('prefix') + mode; }");
    ScriptHandle handle = new LegacyScriptHandle((Invocable) engine, engine);

    assertEquals("npc:2", handle.invoke("action", ScriptInvocationContext.of("cm", Map.of("prefix", "npc:")), (byte) 2));
}

@Test
void missingCallbackRetainsNoSuchMethodException() {
    assertThrows(NoSuchMethodException.class,
        () -> handle.invoke("missing", ScriptInvocationContext.empty()));
}
```

- [ ] **Step 2: Run the test and confirm missing production classes**

Run: `./mvnw -Dtest=LegacyScriptHandleTest test`

Expected: compilation failure naming `ScriptHandle` and `ScriptInvocationContext`.

- [ ] **Step 3: Implement the handle contract and `ctx` model**

```java
public interface ScriptHandle extends AutoCloseable {
    Object invoke(String callback, ScriptInvocationContext context, Object... arguments)
        throws ScriptException, NoSuchMethodException;

    boolean hasCallback(String callback);

    @Override
    void close();
}
```

`ScriptInvocationContext` is immutable and has `empty()` and `of(String key, Object value)` factory methods. Its `toProxyObject()` returns `ProxyObject.fromMap(new HashMap<>(values))`, so ESM receives normal property access (`ctx.cm`) and cannot mutate the Java-side map.

Implement `LegacyScriptHandle` with a `ScriptEngine` and `Invocable`. Before invocation, put every context entry into the engine scope, then call `invocable.invokeFunction(callback, arguments)` without passing the `ScriptInvocationContext`. `close()` is a no-op because JSR-223 has no context-close operation. `hasCallback` checks the ScriptEngine binding for a callable function without executing it.

- [ ] **Step 4: Run the adapter tests**

Run: `./mvnw -Dtest=LegacyScriptHandleTest test`

Expected: PASS.

- [ ] **Step 5: Commit the legacy compatibility seam**

```bash
git add src/main/java/scripting/ScriptHandle.java src/main/java/scripting/ScriptInvocationContext.java src/main/java/scripting/LegacyScriptHandle.java src/test/java/scripting/LegacyScriptHandleTest.java
git commit -m "refactor: add script handle abstraction"
```

### Task 3: Implement native ESM handles and module behavior

**Files:**
- Create: `src/main/java/scripting/EsmScriptHandle.java`
- Create: `src/main/java/scripting/ScriptLoadException.java`
- Create: `src/test/java/scripting/EsmScriptHandleTest.java`

- [ ] **Step 1: Write ESM execution and host-context tests using temporary files**

```java
@Test
void invokesNamedExportWithCtxBeforeBusinessArguments(@TempDir Path tempDir) throws Exception {
    Path entry = write(tempDir.resolve("npc.mjs"),
        "export function action(ctx, mode) { return ctx.cm.get('prefix') + mode; }");
    try (ScriptHandle handle = EsmScriptHandle.load(entry)) {
        assertEquals("npc:3", handle.invoke("action", ScriptInvocationContext.of("cm", Map.of("prefix", "npc:")), 3));
    }
}

@Test
void importedJsWithoutExportsRunsForSideEffects(@TempDir Path tempDir) throws Exception {
    write(tempDir.resolve("setup.js"), "globalThis.counter = (globalThis.counter ?? 0) + 1;");
    Path entry = write(tempDir.resolve("entry.mjs"),
        "import './setup.js'; export function count() { return globalThis.counter; }");
    try (ScriptHandle handle = EsmScriptHandle.load(entry)) {
        assertEquals(1, handle.invoke("count", ScriptInvocationContext.empty()));
    }
}
```

- [ ] **Step 2: Add tests for resolver, cache, and rejected syntax**

```java
@Test
void permitsRelativeAndAbsoluteStaticImports(@TempDir Path tempDir) throws Exception {
    write(tempDir.resolve("relative.js"), "export const relative = 'relative:';");
    Path absolute = write(tempDir.resolve("absolute.mjs"), "export const absolute = 'absolute';");
    Path entry = write(tempDir.resolve("entry.mjs"), """
        import { relative } from './relative.js';
        import { absolute } from '%s';
        export function result() { return relative + absolute; }
        """.formatted(absolute.toRealPath()));

    try (ScriptHandle handle = EsmScriptHandle.load(entry)) {
        assertEquals("relative:absolute", handle.invoke("result", ScriptInvocationContext.empty()));
    }
}

@Test
void evaluatesOneModuleOnlyOncePerHandle(@TempDir Path tempDir) throws Exception {
    write(tempDir.resolve("counter.js"), "globalThis.count = (globalThis.count ?? 0) + 1; export const count = globalThis.count;");
    Files.createDirectory(tempDir.resolve("nested"));
    Path entry = write(tempDir.resolve("entry.mjs"), """
        import { count as first } from './counter.js';
        import { count as second } from './nested/../counter.js';
        export function result() { return `${first}:${second}`; }
        """);

    try (ScriptHandle handle = EsmScriptHandle.load(entry)) {
        assertEquals("1:1", handle.invoke("result", ScriptInvocationContext.empty()));
    }
}

@Test
void supportsCyclicStaticImports(@TempDir Path tempDir) throws Exception {
    write(tempDir.resolve("a.mjs"), "import { b } from './b.mjs'; export const a = 'a'; export function value() { return a + b; }");
    write(tempDir.resolve("b.mjs"), "import { a } from './a.mjs'; export const b = 'b'; export function peer() { return a + b; }");

    try (ScriptHandle handle = EsmScriptHandle.load(tempDir.resolve("a.mjs"))) {
        assertEquals("ab", handle.invoke("value", ScriptInvocationContext.empty()));
    }
}

@ParameterizedTest
@ValueSource(strings = {
    "export const load = import('./later.mjs');",
    "await Promise.resolve(); export function start(ctx) {}",
    "import value from 'lodash'; export { value };",
    "import value from 'node:fs'; export { value };"
})
void rejectsUnsupportedModuleFeatures(String source, @TempDir Path tempDir) {
    Path entry = write(tempDir.resolve("entry.mjs"), source);
    ScriptLoadException exception = assertThrows(ScriptLoadException.class, () -> EsmScriptHandle.load(entry));
    assertTrue(exception.getMessage().contains(entry.toString()));
}
```

- [ ] **Step 3: Run the new ESM test class and confirm it fails**

Run: `./mvnw -Dtest=EsmScriptHandleTest test`

Expected: compilation failure naming `EsmScriptHandle` and `ScriptLoadException`.

- [ ] **Step 4: Implement `EsmScriptHandle` with Polyglot Context**

Create each handle from exactly one entry path:

```java
Context context = Context.newBuilder("js")
    .allowHostAccess(HostAccess.ALL)
    .allowHostClassLookup(className -> true)
    .allowIO(IOAccess.ALL)
    .option("js.esm-eval-returns-exports", "true")
    .build();

Source source = Source.newBuilder("js", entryPath.toFile())
    .mimeType("application/javascript+module")
    .build();
Value namespace = context.eval(source);
```

Before evaluating, validate every static module specifier recursively: allow relative or absolute JavaScript file paths, reject bare/`node:`/URL specifiers, and report an import chain. Scan module tokens to reject `import(` and top-level `await` with file and line/column. Normalize each resolved file with `toRealPath()`; pass the resulting file-backed source to Graal so relative imports preserve file URIs and Graal performs native linking/caching/cycle handling.

`invoke` must obtain `namespace.getMember(callback)`, reject absent/non-executable exports with `NoSuchMethodException`, then execute it as:

```java
Value result = export.execute(context.asValue(invocationContext.toProxyObject()), arguments);
return result.isNull() ? null : result.as(Object.class);
```

Convert `PolyglotException` to `ScriptLoadException`/`ScriptException` while retaining entry path, failing source path, line/column, and import chain. `close()` calls `context.close()` exactly once.

- [ ] **Step 5: Run focused ESM tests**

Run: `./mvnw -Dtest=EsmScriptHandleTest test`

Expected: PASS.

- [ ] **Step 6: Commit ESM loading support**

```bash
git add src/main/java/scripting/EsmScriptHandle.java src/main/java/scripting/ScriptLoadException.java src/test/java/scripting/EsmScriptHandleTest.java
git commit -m "feat: load ESM game scripts"
```

### Task 4: Refactor the abstract loader and client-scoped cache

**Files:**
- Modify: `src/main/java/scripting/AbstractScriptManager.java`
- Modify: `src/main/java/client/Client.java:130,1194-1203`
- Modify: `src/test/java/scripting/ScriptEvaluationTest.java`
- Create: `src/test/java/scripting/AbstractScriptManagerTest.java`

- [ ] **Step 1: Write loader/cache tests**

```java
@Test
void clientCacheReusesOneHandleAndClosesItOnReset(@TempDir Path tempDir) throws Exception {
    TestScriptManager manager = new TestScriptManager(tempDir);
    Client client = mock(Client.class);
    ScriptHandle handle = manager.loadForClient("npc", "entry.mjs", client);

    assertSame(handle, manager.loadForClient("npc", "entry.mjs", client));
    manager.reset("npc", "entry.mjs", client);
    assertTrue(((EsmScriptHandle) handle).isClosedForTesting());
}
```

- [ ] **Step 2: Run the tests and confirm current API mismatch**

Run: `./mvnw -Dtest=AbstractScriptManagerTest,ScriptEvaluationTest test`

Expected: compilation failure because the common loader and client handle cache do not exist.

- [ ] **Step 3: Replace ScriptEngine-specific abstract methods**

In `AbstractScriptManager`, replace both `getInvocableScriptEngine` overloads with `loadScript(directory, identifier)` and `loadScript(directory, identifier, Client)`. Each method must use `ScriptPathResolver`, read source once for classification, create a `LegacyScriptHandle` or `EsmScriptHandle`, and log/return `null` on load failure just as the current manager does.

The client overload uses one stable normalized path key. `resetContext` removes that exact handle and closes it.

In `Client`, replace:

```java
private Map<String, ScriptEngine> engines = new HashMap<>();
```

with:

```java
private Map<String, ScriptHandle> scriptHandles = new HashMap<>();
```

Provide `setScriptHandle`, `getScriptHandle`, and `removeScriptHandle`; `removeScriptHandle` removes then calls `close()` when non-null. Update imports accordingly.

- [ ] **Step 4: Extend full script evaluation coverage**

Update `ScriptEvaluationTest` to call the new protected loader through its test subclass. Preserve the existing directory parameterization for every legacy file and add temporary `.mjs`/ESM `.js` cases. Assert the returned handle is non-null and close it in test teardown.

- [ ] **Step 5: Run loader and existing script tests**

Run: `./mvnw -Dtest=AbstractScriptManagerTest,ScriptEvaluationTest test`

Expected: PASS; all current scripts still evaluate as legacy scripts.

- [ ] **Step 6: Commit common loader migration**

```bash
git add src/main/java/scripting/AbstractScriptManager.java src/main/java/client/Client.java src/test/java/scripting/AbstractScriptManagerTest.java src/test/java/scripting/ScriptEvaluationTest.java
git commit -m "refactor: load scripts through unified handles"
```

### Task 5: Migrate client-scoped managers without changing their isolation

**Files:**
- Modify: `src/main/java/scripting/npc/NPCScriptManager.java`
- Modify: `src/main/java/scripting/quest/QuestScriptManager.java`
- Modify: `src/main/java/scripting/reactor/ReactorScriptManager.java`
- Modify: `src/main/java/scripting/reactor/ReactorActionManager.java`
- Create: `src/test/java/scripting/ClientScopedScriptManagerTest.java`

- [ ] **Step 1: Write callback-shape and scope tests against handles**

```java
@Test
void esmNpcActionReceivesCmAsCtxProperty() throws Exception {
    // ESM export action(ctx, mode, type, selection) returns ctx.cm.getNpc() + selection.
    // Load once for a mocked Client and assert the manager supplies ctx.cm.
}

@Test
void sameEntryHasIndependentModuleStateForTwoClients() throws Exception {
    // ESM export start(ctx) increments module-local counter.
    // First invocation for each Client returns 1, proving separate client contexts.
}

@Test
void legacyNpcActionStillUsesGlobalCmAndThreeArguments() throws Exception {
    // Legacy action(mode, type, selection) reads cm and returns its arguments unchanged.
}
```

- [ ] **Step 2: Run the tests and confirm the managers still require Invocable**

Run: `./mvnw -Dtest=ClientScopedScriptManagerTest test`

Expected: FAIL because managers still store `Invocable` and inject engine globals directly.

- [ ] **Step 3: Convert NPC and item flows**

Change `scripts` from `Map<Client, Invocable>` to `Map<Client, ScriptHandle>`. Resolve file names through `loadScript("npc", fileName, c)` / `loadScript("item", fileName, c)` so extensionless callers remain compatible and explicit `.mjs` names work. Invoke ESM/legacy callbacks using:

```java
handle.invoke("start", ScriptInvocationContext.of(engineName, cm));
handle.invoke("action", ScriptInvocationContext.of(engineName, cm), mode, type, selection);
```

For the existing NPC `start()` then `start(chr)` fallback, retry the same handle with `chr` after `NoSuchMethodException`; do not change legacy behavior. Keep `cm` for normal NPCs and `im` for item scripts, exactly matching existing global names. Dispose must remove and close the matching client cache key through `resetContext`.

- [ ] **Step 4: Convert quest and reactor flows**

Quest manager stores `ScriptHandle` and invokes `start`, `end`, and `raiseOpen` with `ScriptInvocationContext.of("qm", qm)` before existing mode/type/selection arguments. Preserve the medal quest fallback and all current dispose/reset paths.

Reactor manager obtains a client-scoped handle, constructs `ReactorActionManager(c, reactor)` without the unused `Invocable` constructor argument, then invokes `hit`, `act`, `touch`, and `untouch` with `ScriptInvocationContext.of("rm", rm)`. Remove the unused `iv` field/import/constructor parameter from `ReactorActionManager`.

- [ ] **Step 5: Run client-scoped tests and existing script tests**

Run: `./mvnw -Dtest=ClientScopedScriptManagerTest,ScriptEvaluationTest test`

Expected: PASS.

- [ ] **Step 6: Commit client-scoped manager support**

```bash
git add src/main/java/scripting/npc/NPCScriptManager.java src/main/java/scripting/quest/QuestScriptManager.java src/main/java/scripting/reactor/ReactorScriptManager.java src/main/java/scripting/reactor/ReactorActionManager.java src/test/java/scripting/ClientScopedScriptManagerTest.java
git commit -m "feat: support ESM client-scoped scripts"
```

### Task 6: Migrate shared Portal, Map, Event, and devtest call sites

**Files:**
- Modify: `src/main/java/scripting/portal/PortalScriptManager.java`
- Delete: `src/main/java/scripting/portal/PortalScript.java`
- Modify: `src/main/java/scripting/map/MapScriptManager.java`
- Modify: `src/main/java/scripting/event/EventScriptManager.java`
- Modify: `src/main/java/scripting/event/EventManager.java`
- Modify: `src/main/java/scripting/event/EventInstanceManager.java`
- Modify: `src/main/java/client/command/commands/gm6/DevtestCommand.java`
- Create: `src/main/java/scripting/SynchronizedScriptHandle.java`
- Create: `src/test/java/scripting/SharedScriptManagerTest.java`

- [ ] **Step 1: Write shared-scope behavior tests**

```java
@Test
void portalEsmStateIsSharedAcrossCalls() throws Exception {
    // ESM enter(ctx) increments a module-local counter and returns true.
    // Two portal calls through the cached handle observe 1 then 2.
}

@Test
void mapEsmStartReceivesMapMethodsThroughCtx() throws Exception {
    // ESM start(ctx) calls ctx.msm.dropMessage(...); verify the mock.
}

@Test
void eventEsmScheduledCallbackReceivesEmAsFirstArgument() throws Exception {
    // ESM export scheduled(ctx, eim) records ctx.em and eim.
}
```

- [ ] **Step 2: Run shared-manager tests and confirm direct Invocable references prevent them from compiling/passing**

Run: `./mvnw -Dtest=SharedScriptManagerTest test`

Expected: FAIL because Portal/Map/Event still use `Invocable` and `PortalScript`.

- [ ] **Step 3: Convert portal and map caches**

Store `Map<String, ScriptHandle>` instead of interfaces/invocables. Portal calls:

```java
Object result = handle.invoke("enter", ScriptInvocationContext.of("pi", new PortalPlayerInteraction(c, portal)));
return result instanceof Boolean entered && entered;
```

Preserve the current false result/logging behavior for missing or failing scripts. Remove `PortalScript.java` only after no production code imports it.

Map calls `handle.invoke("start", ScriptInvocationContext.of("msm", new MapScriptMethods(c)))`; retain the existing `firstUser` bookkeeping. `reloadScripts()` must close every cached handle before clearing the map.

- [ ] **Step 4: Convert EventManager and preserve synchronization**

Create `SynchronizedScriptHandle` as a synchronized delegating `ScriptHandle`; use it when EventScriptManager constructs an event entry, replacing `SynchronizedInvocable.of(...)`.

Change `EventManager` to hold a `ScriptHandle` and an `eventContext()` helper:

```java
private ScriptInvocationContext eventContext() {
    return ScriptInvocationContext.of("em", this);
}
```

Replace every `iv.invokeFunction(name, args)` call in `EventManager` and `EventInstanceManager` with `handle.invoke(name, eventContext(), args)`. This includes `init`, `cancelSchedule`, `getMaxLobbies`, scheduled functions, `setup`, `getEligibleParty`, `clearPQ`, `createInstance`, and `EventInstanceManager.invokeScriptFunction`. Keep their existing exception handling and return coercions. On event reload/cancel/dispose, close the old handle after all scheduled work is cancelled.

- [ ] **Step 5: Convert devtest**

Replace the inner public ScriptEngine bridge with a handle loader. It loads the legacy default `devtest.js`; if the file is renamed/referenced explicitly as `devtest.mjs`, it invokes `run(ctx, client.getPlayer())` through `ScriptInvocationContext.empty()`. Legacy `run(player)` remains unchanged because `LegacyScriptHandle` strips `ctx`.

- [ ] **Step 6: Run shared-manager tests and all scripting tests**

Run: `./mvnw -Dtest=SharedScriptManagerTest,ClientScopedScriptManagerTest,ScriptEvaluationTest test`

Expected: PASS.

- [ ] **Step 7: Commit shared-manager support**

```bash
git add src/main/java/scripting/portal/PortalScriptManager.java src/main/java/scripting/portal/PortalScript.java src/main/java/scripting/map/MapScriptManager.java src/main/java/scripting/event/EventScriptManager.java src/main/java/scripting/event/EventManager.java src/main/java/scripting/event/EventInstanceManager.java src/main/java/client/command/commands/gm6/DevtestCommand.java src/main/java/scripting/SynchronizedScriptHandle.java src/test/java/scripting/SharedScriptManagerTest.java
git commit -m "feat: support ESM shared game scripts"
```

### Task 7: Document the authoring contract and verify the full feature

**Files:**
- Create: `docs/scripting-esm.md`
- Modify: `README.md` (add one link in the scripting/contributor documentation area)
- Modify: `src/test/java/scripting/ScriptEvaluationTest.java`

- [ ] **Step 1: Add an executable ESM fixture to the script-evaluation tests**

```java
@Test
void esmScriptWithContextAndStaticImportEvaluates(@TempDir Path tempDir) throws Exception {
    write(tempDir.resolve("helpers.js"), "export const message = 'ok';");
    Path entry = write(tempDir.resolve("entry.mjs"),
        "import { message } from './helpers.js'; export function start(ctx) { return message; }");
    try (ScriptHandle handle = EsmScriptHandle.load(entry)) {
        assertEquals("ok", handle.invoke("start", ScriptInvocationContext.empty()));
    }
}
```

- [ ] **Step 2: Write the script-author guide**

`docs/scripting-esm.md` must include these exact examples:

```js
// Legacy scripts/npc/9000000.js
function start() {
  cm.sendOk("Hello.");
}
```

```js
// ESM scripts/npc/9000000.mjs
import { canReward } from "../lib/rewards.js";

export function start(ctx) {
  if (canReward(ctx.cm)) {
    ctx.cm.sendOk("Hello.");
  }
}
```

Document `.mjs`, `.js` classification, explicit versus extensionless entry names, named exports, the `ctx` table, relative/absolute import behavior, imported side-effect files, cache scopes, migration guidance, and all excluded features. Add a README link labelled `ESM script authoring`.

- [ ] **Step 3: Run feature-focused and full test suites**

Run: `./mvnw -Dtest='scripting.*' test`

Expected: PASS.

Run: `./mvnw test`

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Inspect the final worktree**

Run: `git diff --check && git status --short`

Expected: no whitespace errors; only the intended ESM runtime, tests, and documentation files are listed.

- [ ] **Step 5: Commit documentation and final verification fixture**

```bash
git add docs/scripting-esm.md README.md src/test/java/scripting/ScriptEvaluationTest.java
git commit -m "docs: explain ESM game scripts"
```

## Plan Self-Review

- Spec coverage: Tasks 1-3 cover classification, ESM loading/import restrictions, callback exports, `ctx`, and Graal alignment. Tasks 4-6 preserve all specified cache scopes and dispose paths across every manager. Task 7 covers author documentation and complete verification.
- Placeholder scan: the document contains no unresolved work markers, deferred steps, or unspecified error-handling step.
- Type consistency: every manager calls `ScriptHandle.invoke(callback, ScriptInvocationContext, args...)`; `LegacyScriptHandle` strips the context while `EsmScriptHandle` passes it as the first callback argument.
