package org.teiid.translator.wfs.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;
import org.teiid.resource.adapter.ws.WSConnectionImpl;
import org.teiid.resource.adapter.ws.WSManagedConnectionFactory;
import org.teiid.resource.spi.BasicConnectionFactory;
import org.teiid.translator.WSConnection;

public class HttpClientTest {

    private HttpClient http;

    public HttpClientTest() throws Exception {
        WSManagedConnectionFactory mcf = new WSManagedConnectionFactory();
        mcf.setEndPoint("http://demo.opengeo.org/geoserver/ows?service=wfs&version=1.1.0");
        BasicConnectionFactory<WSConnectionImpl> cf = mcf.createConnectionFactory();
        WSConnection conn = cf.getConnection();
        http = new HttpClient(conn);
    }

    @Test
    public void clientOkay() throws Exception {
        
        String capsXml = http.get("?request=GetCapabilities").success().body();
        Document caps = Jsoup.parse(capsXml);

        for (Element feature : caps.select("FeatureType")) {
            System.out.println(feature.select("Name").text());
            System.out.println(feature.select("ows|Keyword").size());
        }

    }

}
