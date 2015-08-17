package org.teiid.translator.wfs.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import javax.activation.DataSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;
import org.apache.commons.io.IOUtils;
import org.teiid.core.types.ClobImpl;
import org.teiid.core.types.InputStreamFactory;
import org.teiid.translator.WSConnection;

/**
 * Simple wrapper around Teiid WS connection.
 */
public class HttpClient {
    private WSConnection conn;

    public HttpClient(WSConnection conn) {
        this.conn = conn;
    }

    public static class Response {
        private int status;
        private String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public int status() {
            return status;
        }

        public String body() {
            return body;
        }
        
        public Response success() throws IOException {
            if (status < 200 || status >= 300) {
                throw new IOException("Expected success but status was " + status + "; body: " + body);
            }
            return this;
        }
    }

    public Response get(String url) throws IOException {
        return request("GET", url, null);
    }
    
    public Response post(String url, String body) throws IOException {
        return request("POST", url, body);
    }

    protected Response request(String method, String url, String body)
            throws IOException {
        Dispatch<DataSource> dispatch = conn.createDispatch(HTTPBinding.HTTP_BINDING, url, DataSource.class, null);
        Map<String,Object> reqCtx = dispatch.getRequestContext();

        reqCtx.put(MessageContext.HTTP_REQUEST_METHOD, method);

        DataSource msg = null;
        if (body != null) {
            msg = new InputStreamFactory.ClobInputStreamFactory(new ClobImpl(body));
        }
        
        // TODO Change to async so we can cancel.
        DataSource response = dispatch.invoke(msg);
        Map<String,Object> respCtx = dispatch.getResponseContext();

        int status = (Integer) respCtx.get(WSConnection.STATUS_CODE);
        String respBody = null;
        
        InputStream is = response.getInputStream();
        try {
            respBody = IOUtils.toString(is);
        } finally {
            IOUtils.closeQuietly(is);
        }

        return new Response(status, respBody);
    }
}
