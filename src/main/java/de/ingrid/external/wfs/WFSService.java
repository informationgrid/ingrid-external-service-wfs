/*
 * **************************************************-
 * InGrid external-service-wfs
 * ==================================================
 * Copyright (C) 2014 - 2022 wemove digital solutions GmbH
 * ==================================================
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * 
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * **************************************************#
 */
package de.ingrid.external.wfs;

import java.io.InputStream;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import de.ingrid.external.GazetteerService;
import de.ingrid.external.om.Location;

public class WFSService implements GazetteerService {
    
    private Logger log = Logger.getLogger( WFSService.class );
    
    WFSClient wfsClient;
    WFSMapper wfsMapper;
    
    // Init Method is called by the Spring Framework on initialization
    public void init() throws Exception {
        ResourceBundle wfsProps = ResourceBundle.getBundle("wfs");
        String url = wfsProps.getString( "url" );
        String[] types = wfsProps.getString( "objectTypes" ).split( "," );
        String[] properties = wfsProps.getString( "properties" ).split( "," );
        wfsClient = new WFSClient( url, properties, types );
        wfsMapper = new WFSMapper( wfsProps );
    }

    @Override
    public Location[] findLocationsFromQueryTerm(String term, QueryType typeOfQuery, MatchingType matching, Locale locale) {
        InputStream response = wfsClient.findLocation(term, matching, locale);
        Location[] locations = wfsMapper.mapReponseToLocations( response );
        return locations;
    }

    @Override
    public Location getLocation(String locationId, Locale locale) {
        InputStream response = wfsClient.getLocation(locationId, locale);
        Location[] locations = wfsMapper.mapReponseToLocations( response );
        return (locations != null && locations.length > 0) ? locations[0] : null;
    }

    @Override
    public Location[] getLocationsFromText(String text, int analyzeMaxWords, boolean ignoreCase, Locale locale) {
        log.warn( "This function is not supported! -> getLocationsFromText(...) -> using findLocationsFromQueryTerm instead" );
        return findLocationsFromQueryTerm( text, QueryType.ALL_LOCATIONS, MatchingType.CONTAINS, locale );
    }

    @Override
    public Location[] getRelatedLocationsFromLocation(String locationId, boolean includeFrom, Locale locale) {
        log.warn( "This function is not supported! -> getRelatedLocationsFromLocation(...)" );
        return new Location[0];
    }

}
