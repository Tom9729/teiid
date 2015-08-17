package org.teiid.translator.wfs;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teiid.core.types.DataTypeManager;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.translator.MetadataProcessor;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.TypeFacility;
import org.teiid.translator.WSConnection;
import org.teiid.translator.wfs.util.HttpClient;

/**
 * http://demo.opengeo.org/geoserver/ows?service=wfs&version=&request=GetCapabilities
 * http://demo.opengeo.org/geoserver/ows?service=wfs&version=1.1.0&request=DescribeFeatureType&typeName=osm:ne_10m_populated_places
 */
public class WfsMetadataProcessor implements MetadataProcessor<WSConnection> {

    private Logger logger = LoggerFactory.getLogger(WfsMetadataProcessor.class);
    
    @Override
    public void process(MetadataFactory metadataFactory, 
                        WSConnection connection)
            throws TranslatorException {
        try {
            HttpClient http = new HttpClient(connection);
            
            // Fetch capabilities document.
            Document capabilities = Jsoup.parse(
                    http.get("?request=GetCapabilities").success().body()
            );

            // Get list of feature types
            Set<String> typeNames = new LinkedHashSet();
            for (Element feature : capabilities.select("FeatureType")) {
                String name = feature.select("Name").text();
                typeNames.add(name);
            }

            System.out.println("Found " + typeNames.size() + " types.");

            for (String typeName : typeNames) {
                // Fetch XSD for type.
                Document typeDesc = Jsoup.parse(
                        http.get("?request=DescribeFeatureType&typeName=" + typeName).success().body()
                );

                Elements fields = typeDesc.select("xsd|complexType xsd|element");
                if (fields.isEmpty()) {
                    System.out.printf("Skipping type [%s] with no fields.\n", typeName);
                    continue;
                }

                String mangledTypeName = typeName.replace(':', '_');
                Table table = metadataFactory.addTable(mangledTypeName);
                table.setProperty("WFS:typeName", typeName);

                for (Element type : fields) {
                    String columnName = type.attr("name");
                    String mangledColumnName = columnName.toLowerCase();
                    String dataTypeName = parseDataType(type.attr("type"));
                    Column column = metadataFactory.addColumn(mangledColumnName, dataTypeName, table);
                    column.setProperty("WFS:propertyName", columnName);
                }
            }
            
        } catch (IOException e) {
            throw new TranslatorException(e);
        }
    }

    protected String parseDataType(String xsdType) {
        if (xsdType.startsWith("gml:")) {
            return TypeFacility.RUNTIME_NAMES.GEOMETRY;
        }

        String shortType = xsdType.replaceFirst(".*:", "");

        if (shortType.equals("int")) {
            shortType = "integer";
        }
        
        Class<?> clazz = DataTypeManager.getDataTypeClass(shortType);

        if (clazz == Object.class) {
            System.out.printf("Unrecognize type [%s].\n", xsdType);
        }

        return DataTypeManager.getDataTypeName(clazz);
    }
    
}
