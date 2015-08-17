package org.teiid.translator.wfs;

import java.util.Properties;
import org.junit.Test;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.query.metadata.SystemMetadata;
import org.teiid.resource.adapter.ws.WSConnectionImpl;
import org.teiid.resource.adapter.ws.WSManagedConnectionFactory;
import org.teiid.resource.spi.BasicConnectionFactory;
import org.teiid.translator.WSConnection;

public class WfsMetadataProcessorTest {
    @Test
    public void parseCapabilitiesOkay() throws Exception {
        MetadataFactory mf = new MetadataFactory("vdb", 1, "northwind", SystemMetadata.getInstance().getRuntimeTypeMap(), new Properties(), null);

        WSManagedConnectionFactory mcf = new WSManagedConnectionFactory();
        mcf.setEndPoint("http://demo.opengeo.org/geoserver/ows?service=wfs&version=1.1.0");
        BasicConnectionFactory<WSConnectionImpl> cf = mcf.createConnectionFactory();
        WSConnection conn = cf.getConnection();

        WfsMetadataProcessor mp = new WfsMetadataProcessor();
        mp.process(mf, conn);

        for (Table t : mf.getSchema().getTables().values()) {
            System.out.println(t.getName());
            for (Column c : t.getColumns()) {
                System.out.printf("\t%-10s%s\n", c.getDatatype().getName(), c.getName());
            }
        }
    }
}
