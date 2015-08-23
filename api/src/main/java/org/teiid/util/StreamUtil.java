package org.teiid.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Helpers for interacting with streams.
 */
public class StreamUtil {
    private static final int BUFFER_SIZE = 1024 * 4;

    public static String readToString(InputStream is, Charset cs) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytes;
        do {
            bytes = is.read(buffer);
            baos.write(buffer, 0, bytes);
        } while (bytes != -1);
        return baos.toString(cs.name());
    }

    public static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                // nothing
            }
        }
    }

}
