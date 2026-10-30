package demo;

import demo.single.SingleFileExamples;
import demo.cross.CrossFileExamples;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ExamplesTest {
    private final SingleFileExamples single = new SingleFileExamples();
    private final CrossFileExamples cross = new CrossFileExamples();

    @Test void directCollectionResolutionPreservesForwardReferenceOrder() throws Exception {
        var expected = java.util.List.of("resolved", "after-reference");
        assertEquals(expected, single.resolveCollectionReference(1, "resolved"));
        assertEquals(expected, cross.resolveCollectionReference(1, "resolved"));
    }

    @Test void directCollectionResolutionKeepsObjectIdentity() throws Exception {
        Object value = new Object();
        assertSame(value, single.resolveCollectionReference(2, value).get(0));
        assertSame(value, cross.resolveCollectionReference(2, value).get(0));
    }

    @Test void escapedNamesAreDecodedByReaderParserInBothLayouts() throws Exception {
        String json = "{\"display\\\"name\":1,\"nested\":{\"line\\nname\":2}}";
        var expected = java.util.List.of("display\"name", "nested", "line\nname");
        assertEquals(expected, single.readFieldNames(json));
        assertEquals(expected, cross.readFieldNames(json));
    }

    @Test void malformedEscapedNamesFailInBothLayouts() {
        String json = "{\"bad\\q\":1}";
        var singleError = assertThrows(com.fasterxml.jackson.core.JsonParseException.class,
                () -> single.readFieldNames(json));
        var crossError = assertThrows(com.fasterxml.jackson.core.JsonParseException.class,
                () -> cross.readFieldNames(json));
        for (var error : java.util.List.of(singleError, crossError)) {
            assertTrue(java.util.Arrays.stream(error.getStackTrace()).anyMatch(frame ->
                    frame.getClassName().equals(
                            "com.fasterxml.jackson.core.json.ReaderBasedJsonParser")
                    && frame.getMethodName().equals("_parseName2")),
                    "The exception must originate through the selected library method");
        }
    }

    @Test void jacksonCoreVersionIsPinnedToSuppliedAffectedRange() {
        assertEquals("2.18.0", new com.fasterxml.jackson.core.JsonFactory().version().toString());
    }

    @Test void fileUriRoundTripsInBothLayouts() throws Exception {
        Path expected = Path.of("sample.txt").toAbsolutePath();
        String json = "\"" + expected.toUri() + "\"";
        assertEquals(expected, single.readPath(json));
        assertEquals(expected, cross.readPath(json));
    }

    @Test void xmlDurationHasExpectedComponentsInBothLayouts() throws Exception {
        for (var duration : new javax.xml.datatype.Duration[]{
                single.readDuration("\"P2DT3H\""), cross.readDuration("\"P2DT3H\"")}) {
            assertEquals(2, duration.getDays());
            assertEquals(3, duration.getHours());
        }
    }

    @Test void unknownTypeNameUsesFallbackInBothLayouts() throws Exception {
        String json = "{\"kind\":\"example\",\"message\":\"hello\"}";
        assertEquals("hello", single.readEvent(json).message);
        assertEquals("hello", cross.readEvent(json).message);
    }

    @Test void freshBufferProducesExpectedPaddingInBothLayouts() throws Exception {
        assertArrayEquals(new byte[4], single.writePadding(4));
        assertArrayEquals(new byte[4], cross.writePadding(4));
        assertEquals(0, single.writePadding(0).length);
        assertEquals(0, cross.writePadding(0).length);
    }

    @Test void directPathDeserializerRejectsNonStringTokensInBothLayouts() {
        assertThrows(com.fasterxml.jackson.databind.exc.MismatchedInputException.class,
                () -> single.readPath("123"));
        assertThrows(com.fasterxml.jackson.databind.exc.MismatchedInputException.class,
                () -> cross.readPath("123"));
    }

    @Test void malformedInputsAreRejectedInBothLayouts() {
        assertThrows(Exception.class, () -> single.readDuration("\"invalid\""));
        assertThrows(Exception.class, () -> cross.readDuration("\"invalid\""));
        assertThrows(IllegalArgumentException.class, () -> single.writePadding(65));
        assertThrows(IllegalArgumentException.class, () -> cross.writePadding(-1));
    }
}
