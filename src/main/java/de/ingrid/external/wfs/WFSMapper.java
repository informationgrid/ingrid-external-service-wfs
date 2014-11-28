package de.ingrid.external.wfs;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.geotoolkit.gml.xml.v311.FeaturePropertyType;
import org.geotoolkit.wfs.xml.WFSMarshallerPool;
import org.geotoolkit.wfs.xml.v110.FeatureCollectionType;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;

import de.ingrid.external.om.Location;
import de.ingrid.external.om.impl.LocationImpl;

/**
 * This class is used for the mapping of the WFS search responses to a location object.
 * @author André Wallat
 *
 */
public class WFSMapper {
    
//    private XPath xpath;

    public WFSMapper() {
//        this.xpath = XPathFactory.newInstance().newXPath();
    }

    /**
     * Extract the locations from a given search result contained in an InputStream.
     * @param response is the result of the WFS request
     * @return an array of locations
     */
    @SuppressWarnings("unchecked")
    public Location[] mapReponseToLocations(InputStream response) {
        List<Location> locations = new ArrayList<Location>();
        Map<String,String> typeMap = new HashMap<String, String>();
        try {
            Object unmarshal = WFSMarshallerPool.getInstance().acquireUnmarshaller().unmarshal( response );

            JAXBElement<FeatureCollectionType> fc = (JAXBElement<FeatureCollectionType>) unmarshal;
            FeatureCollectionType value = fc.getValue();

            List<FeaturePropertyType> featureMember = value.getFeatureMember();
            for (FeaturePropertyType member : featureMember) {
                Location loc = new LocationImpl();
                ElementNSImpl f = (ElementNSImpl) member.getUnknowFeature();
                loc.setId( getIdFromFeature( f ) );
                loc.setName( getNameFromFeature( f ) );
                float[] bbox = getBBoxFromFeature( f );
                loc.setBoundingBox( bbox[0], bbox[1], bbox[2],bbox[3] );
                // NOT SUPPORTED: loc.setIsExpired( arg0 );
                loc.setNativeKey( getNativeKeyFromFeature( f ) );
//                loc.setQualifier( arg0 );
                loc.setTypeId( getTypeIdFromFeature( f ) );
                loc.setTypeName( getTypeNameFromFeature( f ) );
                
                if (loc.getTypeName() != null) {
                    typeMap.put( loc.getTypeId(), loc.getTypeName() );
                }
                
                locations.add( loc );
            }
            
            // check for typeIds that are references and resolve those correctly
            for (Location l : locations) {
                if (l.getTypeId() != null && l.getTypeId().startsWith( "#" )) {
                    String id = l.getTypeId().substring( 1 );
                    l.setTypeId( id );
                    l.setTypeName( typeMap.get( id ) );
                }
            }
            
            return locations.toArray( new LocationImpl[0] );
            
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        return null;
    }

    
    /****************************************
     * HELPER FUNCTIONS
     ****************************************/
    
    /**
     * Extract the bounding box of of two given coordinates, represented
     * in a document
     * @param f is the feature node in the document
     * @return an array of four floats representing the bounding box
     */
    private float[] getBBoxFromFeature(ElementNSImpl f) {
        NodeList coords = f.getElementsByTagName( "gml:pos" );
        String[] coord1 = coords.item( 0 ).getTextContent().split( " " );
        String[] coord2 = coords.item( 1 ).getTextContent().split( " " );
        
        float[] box = new float[4];
        box[0] = Float.valueOf( coord1[0] );
        box[1] = Float.valueOf( coord1[1] );
        box[2] = Float.valueOf( coord2[0] );
        box[3] = Float.valueOf( coord2[1] );
        
        return box;
    }

    /**
     * Get the name of the type of the location from a search result.
     * @param f
     * @return the type name
     */
    private String getTypeNameFromFeature(ElementNSImpl f) {
        NodeList types = f.getElementsByTagName( "gn:objektart" );
        // TODO: check if it exists and try to look for "gn:wert" otherwise
        ElementNSImpl item = (ElementNSImpl) types.item( 0 );
        if (item != null) {
            return item.getTextContent();
        }
        return null;
    }

    /**
     * Get the id of the type of the location from a search result.
     * @param f
     * @return the type id
     */
    private String getTypeIdFromFeature(ElementNSImpl f) {
        NodeList types = f.getElementsByTagName( "gn:Objektart" );
        ElementNSImpl item = (ElementNSImpl) types.item( 0 );
        if (item != null) {
            return item.getAttribute( "gml:id" );
            
        } else {
            // try to find out if it has a reference to an already defined type
            NodeList hasTypes = f.getElementsByTagName( "gn:hatObjektart" );
            ElementNSImpl hasItem = (ElementNSImpl) hasTypes.item( 0 );
            String link = hasItem.getAttribute( "xlink:href" );
            if (link != null) {
                return link;
            }
        }
        return null;
    }

    /**
     * Get the native key (AGS) of the location from a search result.
     * @param f
     * @return the key
     */
    private String getNativeKeyFromFeature(ElementNSImpl f) {
        // TODO: add switch for RS-key configured by property
        NodeList ags = f.getElementsByTagName( "gn:ags" );
        if (ags.getLength() > 0) {
            return ags.item( 0 ).getTextContent();
        }
        return null;
    }

    /**
     * Get the ID of the location from a search result.
     * @param f
     * @return the ID
     */
    private String getIdFromFeature(ElementNSImpl f) {
        return f.getElementsByTagName( "gn:nnid" ).item( 0 ).getTextContent();
    }

    /**
     * Get the name of the location from a search result.
     * @param f
     * @return the name
     */
    private String getNameFromFeature(ElementNSImpl f) {
        NodeList endonyms = f.getElementsByTagName( "gn:Endonym" );
        
        // TODO: check for language!
        ElementNSImpl item = (ElementNSImpl) endonyms.item( 0 );
        return item.getElementsByTagName( "gn:name" ).item( 0 ).getTextContent();
        
    }

}
