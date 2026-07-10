package caprou.app.impl.render.util;

import java.nio.ByteBuffer;

public final class BufferUtil {

    public static float readF2Dot14(ByteBuffer buffer) {
        return buffer.getShort() / 16384.0f;
    }

}
