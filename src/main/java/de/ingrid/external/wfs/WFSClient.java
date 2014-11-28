package de.ingrid.external.wfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.geotoolkit.ogc.xml.v110.AndType;
import org.geotoolkit.ogc.xml.v110.FilterType;
import org.geotoolkit.ogc.xml.v110.LiteralType;
import org.geotoolkit.ogc.xml.v110.OrType;
import org.geotoolkit.ogc.xml.v110.PropertyIsEqualToType;
import org.geotoolkit.ogc.xml.v110.PropertyIsLikeType;
import org.geotoolkit.ogc.xml.v110.PropertyNameType;
import org.geotoolkit.wfs.xml.ResultTypeType;
import org.geotoolkit.wfs.xml.WFSMarshallerPool;
import org.geotoolkit.wfs.xml.v110.GetFeatureType;
import org.geotoolkit.wfs.xml.v110.QueryType;

public class WFSClient {

    private static final String PROPERTY_ID = "gn:GnObjekt/gn:nnid";
    private static final String PROPERTY_NAME = "gn:GnObjekt/gn:hatEndonym/gn:Endonym/gn:name";
    private static final String PROPERTY_OBJECT_TYPE = "gn:GnObjekt/gn:hatObjektart/gn:Objektart/gn:objektart";

    // the URL to the service
    private String url;

    private List<String> properties;
    private String[] types;
    private Marshaller marshaller;
    
    private static String wildcard = "*";
    private static String singleChar = "?";
    private static String escapeChar = "\\";
    
    private static QName qName = new QName( "http://www.geodatenzentrum.de/gnde", "GnObjekt", "gn" );

    public WFSClient(String wfsUrl, String[] properties, String[] types) throws JAXBException {
        this.url = wfsUrl;
        this.properties = Arrays.asList( properties ); //convertPropertiesToNames( properties );
        this.types = types;
        this.marshaller = WFSMarshallerPool.getInstance().acquireMarshaller();
    }

//    private Name[] convertPropertiesToNames(String[] properties) {
//        Name[] names = new Name[ properties.length ];
//        for (int i=0; i<properties.length; i++) {
//            names[i] = new DefaultName(properties[i]);
//        }
//        return names;
//    }

    public InputStream getLocation(String locationId, Locale locale) {
        
        PropertyIsLikeType idFilter = new PropertyIsLikeType( PROPERTY_ID, locationId, wildcard, singleChar, escapeChar );
        
        FilterType filterType = new FilterType( idFilter );
        
        List<QName> qNames = new ArrayList<QName>();
        qNames.add( qName );
        
        QueryType qType = new QueryType( filterType, qNames, "1.1.0", null, null, properties );
        List<QueryType> qTypes = new ArrayList<QueryType>();
        qTypes.add( qType );

        GetFeatureType gft = new GetFeatureType( "WFS", "1.1.0", null, null, qTypes, ResultTypeType.RESULTS, null );
        
        try {
            return sendRequest( gft );
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    public InputStream findLocation(String term, Locale locale) {

        // setup Filter
        PropertyIsLikeType termFilter = new PropertyIsLikeType( PROPERTY_NAME, "*"+term+"*", wildcard, singleChar, escapeChar );

        // add filter for requested object types
        List<Object> filter = new ArrayList<Object>();
        for (String type : this.types) {
            filter.add( new PropertyIsEqualToType( new LiteralType( type ), new PropertyNameType( PROPERTY_OBJECT_TYPE ), true ) );
        }
        
        // combine object type filter by OR
        OrType typeFilter = new OrType( filter.toArray() );
        
        // combine filter for term and object types by AND
        FilterType filterType = new FilterType( new AndType( termFilter, typeFilter ) );
                
        List<QName> qNames = new ArrayList<QName>();
        qNames.add( qName );
        
        QueryType qType = new QueryType( filterType, qNames, "1.1.0", null, null, properties );
        List<QueryType> qTypes = new ArrayList<QueryType>();
        qTypes.add( qType );

        GetFeatureType gft = new GetFeatureType( "WFS", "1.1.0", null, null, qTypes, ResultTypeType.RESULTS, null );
        
        try {
            return sendRequest( gft );
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
        
    }

    private InputStream sendRequest(GetFeatureType gft) throws HttpException, IOException, JAXBException {
        PostMethod pm = new PostMethod( url );
        StringWriter filterWriter = new StringWriter();
        marshaller.marshal( gft, filterWriter );
        pm.setRequestEntity( new StringRequestEntity(filterWriter.toString(), "application/xml", "UTF8") );
        HttpClient client = new HttpClient();
        client.executeMethod( pm );
        return pm.getResponseBodyAsStream();
    }

}
