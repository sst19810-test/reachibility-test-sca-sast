package demo.cross;

import org.postgresql.util.internal.PgBufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class BufferGateway {
    public byte[] writePadding(int count) throws IOException {
        if (count < 0 || count > 64) throw new IllegalArgumentException("count must be 0..64");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PgBufferedOutputStream stream = new PgBufferedOutputStream(bytes, 128)) {
            stream.writeZeros(count);
        }
        return bytes.toByteArray();
    }
}
