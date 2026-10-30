package demo.cross;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.ReaderBasedJsonParser;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.ext.NioPathDeserializer;
import java.nio.file.Path;
import java.io.IOException;

/** Shared Jackson sink: the caller and target model live in other source files. */
public final class JsonGateway {
    private final ObjectMapper mapper = new ObjectMapper();

    /** Escaped field names enter ReaderBasedJsonParser's private _parseName2 method. */
    public List<String> readFieldNames(String json) throws IOException {
        List<String> names = new ArrayList<>();
        // Reader input selects ReaderBasedJsonParser rather than the UTF-8 byte parser.
        try (JsonParser parser = new JsonFactory().createParser(new StringReader(json))) {
            if (!(parser instanceof ReaderBasedJsonParser)) {
                throw new IllegalStateException("Expected ReaderBasedJsonParser");
            }
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                    names.add(parser.currentName());
                }
            }
        }
        return names;
    }

    public Path deserializePath(String json) throws IOException {
        try (JsonParser parser = mapper.createParser(json)) {
            parser.nextToken();
            DefaultDeserializationContext context =
                    ((DefaultDeserializationContext) mapper.getDeserializationContext())
                            .createInstance(mapper.getDeserializationConfig(), parser,
                                    mapper.getInjectableValues());
            return new NioPathDeserializer().deserialize(parser, context);
        }
    }

    public <T> T read(String json, Class<T> type) throws IOException {
        return mapper.readValue(json, type);
    }
}
