# Java Maven method usage demo

Requires JDK 17+ and Maven 3.6.3+. This local learning/scanner fixture deliberately pins the dependency coordinates supplied in the attachment: Jackson Databind 2.18.0 and PostgreSQL JDBC 42.7.4. These are historical versions associated with the supplied vulnerability report; do not use this fixture as a production dependency baseline.

## Run

From this directory:

```sh
mvn clean test
mvn compile exec:java
mvn compile exec:java -Dexec.args=single
mvn compile exec:java -Dexec.args=cross
```

The default runs both implementations. No PostgreSQL server, credentials, network calls, custom filesystem provider, or external input is required at runtime. Maven initially downloads dependencies. The program uses a local file URI, a short XML duration, one fallback event, and four padding bytes in a new in-memory buffer. It never opens the file path.

## Selected methods and call paths

| Method from supplied report | Single-file application path | Cross-file application path | Invocation |
|---|---|---|---|
| `com.fasterxml.jackson.databind.ext.NioPathDeserializer.deserialize` | `SingleFileExamples.readPath` → `NioPathDeserializer.deserialize` | `CrossFileExamples.readPath` → `JsonGateway.deserializePath` → `NioPathDeserializer.deserialize` | Direct invocation with a positioned parser and initialized context |
| `com.fasterxml.jackson.databind.ext.CoreXMLDeserializers.Std._deserialize` | `SingleFileExamples.readDuration` → `ObjectMapper.readValue` | `CrossFileExamples.readDuration` → `JsonGateway.read` → `ObjectMapper.readValue` | Jackson dispatches to the XML Duration deserializer |
| `com.fasterxml.jackson.databind.jsontype.impl.TypeDeserializerBase._findDeserializer` | `SingleFileExamples.readEvent` → `ObjectMapper.readValue`, using nested `Event` | `CrossFileExamples.readEvent` → `JsonGateway.read`, using `Event.java` | NAME polymorphism selects the default implementation |
| `org.postgresql.util.internal.PgBufferedOutputStream.writeZeros` | `SingleFileExamples.writePadding` → `writeZeros` | `CrossFileExamples.writePadding` → `BufferGateway.writePadding` → `writeZeros` | Direct driver method call |

The Path examples directly invoke `new NioPathDeserializer().deserialize(parser, context)` after advancing the parser to its first token and creating a configured per-call `DefaultDeserializationContext`. The XML and polymorphic examples reach their library hooks through `ObjectMapper.readValue`. No library implementations are copied, mocked, or called through reflection. The PostgreSQL method is public but belongs to an internal package and may change between versions. Single-file means the application logic and nested model reside in one source file; third-party library code still lives in dependencies. Cross-file means the input passes between application classes in separate files before reaching the dependency.

This is method-reachability demonstration, not verification or reproduction of the CVE claims in the attachment. It selects six methods rather than covering every entry. Jackson 3 `tools.jackson.*` methods are excluded because the supplied dependency is Jackson 2. Tests check observable behavior for both layouts and confirm `_parseName2` entry through exception stack frames; they do not instrument library method entry or attempt resource exhaustion, arbitrary class loading, or stale-buffer disclosure.

## Files

- `src/main/java/demo/Main.java`: runs either or both layouts.
- `src/main/java/demo/single/SingleFileExamples.java`: all six same-file examples.
- `src/main/java/demo/cross/CrossFileExamples.java`: cross-file service entry points.
- `src/main/java/demo/cross/JsonGateway.java`: Jackson call site.
- `src/main/java/demo/cross/Event.java`: separately declared polymorphic model.
- `src/main/java/demo/cross/BufferGateway.java`: PostgreSQL call site.
- `src/test/java/demo/ExamplesTest.java`: behavior checks for both layouts.

For manual runtime confirmation, set method breakpoints on the selected library methods in your IDE, attach Maven dependency sources, and run `demo.Main`.

Sources: [Jackson source at 2.18.0](https://github.com/FasterXML/jackson-databind/tree/jackson-databind-2.18.0/src/main/java/com/fasterxml/jackson/databind), [pgJDBC source at 42.7.4](https://github.com/pgjdbc/pgjdbc/tree/REL42.7.4/pgjdbc/src/main/java/org/postgresql), [pgJDBC release notes](https://github.com/pgjdbc/pgjdbc/blob/master/CHANGELOG.md).

## ReaderBasedJsonParser._parseName2 example

`com.fasterxml.jackson.core:jackson-core:2.18.0` is now an explicit Maven dependency, within the affected range supplied for CVE-2026-68498. This project uses the Jackson 2 artifact; it does not add the alternative Jackson 3 artifact. The CVE identification and affected ranges are taken from the user-provided payload, not independently validated here.

- Single-file: `SingleFileExamples.readFieldNames` → reader parser `nextToken()` → `_parseName()` → `_parseName2(...)`.
- Cross-file: `CrossFileExamples.readFieldNames` → `JsonGateway.readFieldNames` → the same library path.

The runnable sample uses a short escaped field name (`{"display\"name":1}`), forcing the name parser's escape-handling slow path. `StringReader` explicitly selects `ReaderBasedJsonParser`. The method `_parseName2(int, int, int)` is private in 2.18.0, so this example reaches the real method through parsing rather than claiming a direct Java invocation. Existing direct `NioPathDeserializer.deserialize` calls remain in place. No CPU-exhaustion payload is generated.

Tests cover escaped quote/newline decoding, malformed field-name escape rejection in both layouts, and the runtime Jackson Core version. The malformed-escape tests also assert that the exception stack includes the actual `ReaderBasedJsonParser._parseName2` method, confirming method entry in both layouts. To observe method entry, set an IDE method breakpoint on `com.fasterxml.jackson.core.json.ReaderBasedJsonParser._parseName2` and run `demo.Main`.

## Direct collection forward-reference resolution

GAV: `com.fasterxml.jackson.core:jackson-databind:2.18.0` (the existing pinned Jackson 2 dependency). This example targets the method identified in the supplied CVE-2026-91777 report; it demonstrates API usage, not resource exhaustion or independent verification of affected-version claims.

Both implementations explicitly import the public nested class `CollectionDeserializer.CollectionReferringAccumulator` and execute:

```java
accumulator.resolveForwardReference(id, value);
```

Before the direct call, the example constructs a `ReadableObjectId`, registers an `UnresolvedForwardReference` with `handleUnresolvedReference`, and adds a trailing item. Resolution places the resolved value before the trailing item, producing `[resolved, after-reference]`. It uses one pending reference, with no stress input. All constructors and method calls are public; no reflection, private access, package impersonation, or copied library code is used.

- Single-file: `SingleFileExamples.resolveCollectionReference` constructs and invokes the accumulator in the same source file.
- Cross-file: `CrossFileExamples.resolveCollectionReference` calls `CollectionReferenceGateway.resolveCollectionReference`, which invokes the library method directly.

Tests check the resulting order and that the resolved object retains identity in both layouts. The application prints both results.

Visibility correction: in the actual 2.18.0 artifact, `CollectionReferringAccumulator` is **public static**, while `MapReferringAccumulator` is **package-private static final**. Although the map method itself is public, its enclosing class cannot be directly accessed by ordinary application code. The map variant is therefore excluded under the user's public-direct-call requirement. Jackson 3 `tools.jackson.*` methods are excluded because this project uses the Jackson 2 GAV.
