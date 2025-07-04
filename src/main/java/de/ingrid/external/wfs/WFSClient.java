/*
 * **************************************************-
 * InGrid external-service-wfs
 * ==================================================
 * Copyright (C) 2014 - 2025 wemove digital solutions GmbH
 * ==================================================
 * Licensed under the EUPL, Version 1.2 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * 
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * **************************************************#
 */
package de.ingrid.external.wfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import de.ingrid.external.GazetteerService.MatchingType;

public class WFSClient {
    
    private Logger log = LogManager.getLogger( WFSClient.class );

    private static final String PROPERTY_ID = "gn:nnid";
    private static final String PROPERTY_NAME = "gn:hatEndonym/gn:Endonym/gn:name";
    private static final String PROPERTY_OBJECT_TYPE = "gn:hatObjektart/gn:Objektart/gn:objektart";

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
        this.properties = new ArrayList<String>(); // Arrays.asList( properties ); // convertPropertiesToNames(
                                                       // properties );
        this.types = types;
        this.marshaller = WFSMarshallerPool.getInstance().acquireMarshaller();
    }

    public InputStream getLocation(String locationId, Locale locale) {

        PropertyIsLikeType idFilter = new PropertyIsLikeType( PROPERTY_ID, locationId, wildcard, singleChar, escapeChar );

        FilterType filterType = new FilterType( idFilter );

        List<QName> qNames = new ArrayList<QName>();
        qNames.add( qName );

        QueryType qType = new QueryType( filterType, qNames, "1.1.0", null, null, properties );
        List<QueryType> qTypes = new ArrayList<QueryType>();
        qTypes.add( qType );

        GetFeatureType gft = new GetFeatureType( "WFS", "1.1.0", null, null, qTypes, ResultTypeType.RESULTS, null, "*", null );

        try {
            return sendRequest( gft );
        } catch (Exception e) {
            log.error( "Error getting location from WFS Service", e );
        }

        return null;
    }

    public InputStream findLocation(String term, MatchingType matching, Locale locale) {

        // Default to EXACT search
        String query = term;
        if (matching == MatchingType.BEGINS_WITH) {
            query += "*";
        } else if (matching == null || matching == MatchingType.CONTAINS) {
            query = "*" + term + "*";
        }
        
        // setup Filter
        PropertyIsLikeType termFilter = new PropertyIsLikeType( PROPERTY_NAME, query, wildcard, singleChar, escapeChar );
        termFilter = new PropertyIsLikeType( termFilter.getExpression(), query, wildcard, singleChar, escapeChar, false );

        // add filter for requested object types
        List<Object> filter = new ArrayList<Object>();
        for (String type : this.types) {
            filter.add( new PropertyIsEqualToType( new LiteralType( type ),
                    new PropertyNameType( PROPERTY_OBJECT_TYPE ), true ) );
        }
        
        // TODO: add filter for language

        // combine object type filter by OR
        OrType typeFilter = new OrType( filter.toArray() );

        // combine filter for term and object types by AND
        FilterType filterType = new FilterType( new AndType( termFilter, typeFilter ) );

        List<QName> qNames = new ArrayList<QName>();
        qNames.add( qName );

        QueryType qType = new QueryType( filterType, qNames, "1.1.0", null, null, properties );
        List<QueryType> qTypes = new ArrayList<QueryType>();
        qTypes.add( qType );

        GetFeatureType gft = new GetFeatureType( "WFS", "1.1.0", null, null, qTypes, ResultTypeType.RESULTS, null, "*", null );

        try {
            return sendRequest( gft );
        } catch (Exception e) {
            log.error( "Error searching location in WFS Service", e );
        }

        return null;

    }

    private InputStream sendRequest(GetFeatureType gft) throws HttpException, IOException, JAXBException {
        PostMethod pm = new PostMethod( url );
        StringWriter filterWriter = new StringWriter();
        marshaller.marshal( gft, filterWriter );
        pm.setRequestEntity( new StringRequestEntity( filterWriter.toString(), "application/xml", "UTF8" ) );
        HttpClient client = new HttpClient();
        if (System.getProperty("http.proxyHost") != null && System.getProperty("http.proxyPort") != null) {
            client.getHostConfiguration().setProxy(System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort")));
        }
        client.executeMethod( pm );
        return pm.getResponseBodyAsStream();
    }

}
