package org.teiid.translator.wfs.util;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpServerUtil.class);

    private static final int PORT_START = 38000;
    private static final int PORT_END = PORT_START + 20;
    
    public static HttpServer getTestServer() {
        for (int i = 0; i < PORT_END; ++i) {
            int port = PORT_START + i;
            try {
                return HttpServer.create(new InetSocketAddress("localhost", port), 0);
            } catch (BindException e) {
                logger.warn("Failed to bind to port {}; trying next.", port);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("Failed to bind port in range " + PORT_START + "-" + PORT_END);
    }
}
