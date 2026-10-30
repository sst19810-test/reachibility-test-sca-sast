package demo.cross;

import javax.xml.datatype.Duration;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Input -> application service -> separate gateway -> library implementation. */
public final class CrossFileExamples {
    private final CollectionReferenceGateway referenceGateway = new CollectionReferenceGateway();
    private final JsonGateway jsonGateway = new JsonGateway();
    private final BufferGateway bufferGateway = new BufferGateway();

    public List<Object> resolveCollectionReference(Object id, Object value) throws IOException {
        return referenceGateway.resolveCollectionReference(id, value);
    }

    public List<String> readFieldNames(String json) throws IOException {
        return jsonGateway.readFieldNames(json);
    }

    public Path readPath(String json) throws IOException {
        return jsonGateway.deserializePath(json);
    }

    public Duration readDuration(String json) throws IOException {
        return jsonGateway.read(json, Duration.class);
    }

    public Event readEvent(String json) throws IOException {
        return jsonGateway.read(json, Event.class);
    }

    public byte[] writePadding(int count) throws IOException {
        return bufferGateway.writePadding(count);
    }
}
