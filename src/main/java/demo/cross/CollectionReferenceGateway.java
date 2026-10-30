package demo.cross;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.deser.UnresolvedForwardReference;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer.CollectionReferringAccumulator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class CollectionReferenceGateway {
    /** Directly completes a registered forward reference using public Jackson APIs. */
    public List<Object> resolveCollectionReference(Object id, Object value) throws IOException {
        List<Object> result = new ArrayList<>();
        CollectionReferringAccumulator accumulator =
                new CollectionReferringAccumulator(Object.class, result);
        ReadableObjectId objectId = new ReadableObjectId(new ObjectIdGenerator.IdKey(
                ObjectIdGenerators.IntSequenceGenerator.class, Object.class, id));
        UnresolvedForwardReference reference = new UnresolvedForwardReference(
                (JsonParser) null, "Demo pending reference", JsonLocation.NA, objectId);
        accumulator.handleUnresolvedReference(reference);
        accumulator.add("after-reference");
        // Actual direct invocation of the selected library method (no reflection).
        accumulator.resolveForwardReference(id, value);
        return result;
    }

}
