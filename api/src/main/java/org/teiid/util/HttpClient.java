package org.teiid.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.DataSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;
import org.teiid.core.types.ClobImpl;
import org.teiid.core.types.InputStreamFactory;
import org.teiid.translator.WSConnection;

/**
 * Simple fluid-style wrapper around Teiid WS connection.
 */
public class HttpClient {
    private static final Pattern CONTENT_TYPE = Pattern.compile("charset=([A-z0-9\\-]+)");
    private final WSConnection conn;

    public HttpClient(WSConnection conn) {
        this.conn = conn;
    }

    public interface ResponseParser<T> {
        T parse(InputStream is, Charset charset) throws IOException;
    }

    public static class Response implements Closeable {
        private final int status;
        private final InputStream body;
        private final Charset charset;

        protected Response(int status, InputStream body, Charset charset) {
            this.status = status;
            this.body = body;
            this.charset = charset;
        }
        
        public int status() {
            return status;
        }

        public String body() throws IOException {
            return body(new ResponseParser<String>() {
                @Override
                public String parse(InputStream is, Charset charset) throws IOException {
                    return StreamUtil.readToString(body, charset);
                }
            });
        }
        
        public <T> T body(ResponseParser<T> delegate) throws IOException {
            try {
                return delegate.parse(body, charset);
            } finally {
                close();
            }
        }
        
        public Response success() throws IOException {
            if (status < 200 || status >= 300) {
                throw new IOException("Expected success but status was " + status + "; body: " + body);
            }
            return this;
        }

        @Override
        public void close() throws IOException {
            StreamUtil.closeQuietly(body);
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
        
        // Determine CS from mime type.
        Charset charset = null;
        try {
            String contentType = response.getContentType();
            Matcher m = CONTENT_TYPE.matcher(contentType);
            if (m.find()) {
                charset = Charset.forName(m.group());
            }
        } catch (Exception e) {
            charset = Charset.defaultCharset();
        }

        return new Response(status, response.getInputStream(), charset);
    }
}
