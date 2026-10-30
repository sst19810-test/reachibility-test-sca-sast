package demo;

import demo.single.SingleFileExamples;
import demo.cross.CrossFileExamples;
import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "both" : args[0];
        if (!mode.equals("single") && !mode.equals("cross") && !mode.equals("both")) {
            throw new IllegalArgumentException("Usage: single | cross | both");
        }
        // Local file URI only; no file is opened or written.
        String pathJson = "\"" + Path.of("sample.txt").toAbsolutePath().toUri() + "\"";
        // JSON contains an escaped quote in its field name: {"display\"name":1}.
        String fieldNameJson = "{\"display\\\"name\":1}";
        String durationJson = "\"P2DT3H\"";
        String eventJson = "{\"kind\":\"example\",\"message\":\"hello\"}";
        if (mode.equals("single") || mode.equals("both")) {
            SingleFileExamples e = new SingleFileExamples();
            System.out.println("single resolved collection: " + e.resolveCollectionReference(1, "resolved"));
            System.out.println("single field names: " + e.readFieldNames(fieldNameJson));
            System.out.println("single path: " + e.readPath(pathJson));
            System.out.println("single duration: " + e.readDuration(durationJson));
            System.out.println("single event: " + e.readEvent(eventJson).message);
            System.out.println("single padding bytes: " + e.writePadding(4).length);
        }
        if (mode.equals("cross") || mode.equals("both")) {
            CrossFileExamples e = new CrossFileExamples();
            System.out.println("cross resolved collection: " + e.resolveCollectionReference(1, "resolved"));
            System.out.println("cross field names: " + e.readFieldNames(fieldNameJson));
            System.out.println("cross path: " + e.readPath(pathJson));
            System.out.println("cross duration: " + e.readDuration(durationJson));
            System.out.println("cross event: " + e.readEvent(eventJson).message);
            System.out.println("cross padding bytes: " + e.writePadding(4).length);
        }
    }
}
