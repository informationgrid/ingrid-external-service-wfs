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
        wfsMapper = new WFSMapper();
    }

    @Override
    public Location[] findLocationsFromQueryTerm(String term, QueryType typeOfQuery, MatchingType matching, Locale locale) {
        InputStream response = wfsClient.findLocation(term, locale);
        Location[] locations = wfsMapper.mapReponseToLocations( response );
        return locations;
    }

    @Override
    public Location getLocation(String locationId, Locale locale) {
        InputStream response = wfsClient.getLocation(locationId, locale);
        Location[] locations = wfsMapper.mapReponseToLocations( response );
        return locations[0];
    }

    @Override
    public Location[] getLocationsFromText(String text, int analyzeMaxWords, boolean ignoreCase, Locale locale) {
        log.warn( "This function is not supported! -> getLocationsFromText(...)" );
        return null;
    }

    @Override
    public Location[] getRelatedLocationsFromLocation(String locationId, boolean includeFrom, Locale locale) {
        log.warn( "This function is not supported! -> getRelatedLocationsFromLocation(...)" );
        return null;
    }

}
