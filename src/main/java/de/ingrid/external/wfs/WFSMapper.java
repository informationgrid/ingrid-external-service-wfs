/*
 * **************************************************-
 * InGrid external-service-wfs
 * ==================================================
 * Copyright (C) 2014 - 2018 wemove digital solutions GmbH
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.geotoolkit.gml.xml.v311.FeaturePropertyType;
import org.geotoolkit.wfs.xml.v110.FeatureCollectionType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.ingrid.external.om.Location;
import de.ingrid.external.om.impl.LocationImpl;

/**
 * This class is used for the mapping of the WFS search responses to a location object.
 * @author André Wallat
 *
 */
public class WFSMapper {
    
    private Logger log = Logger.getLogger( WFSMapper.class ); 
    
    private ResourceBundle bundle;


    public WFSMapper(ResourceBundle wfsProps) {
        this.bundle = wfsProps;
    }

    /**
     * Extract the locations from a given search result contained in an InputStream.
     * @param response is the result of the WFS request
     * @return an array of locations
     */
    @SuppressWarnings("unchecked")
    public Location[] mapReponseToLocations(InputStream response) {
        List<Location> locations = new ArrayList<Location>();
        Map<String,String[]> typeMap = new HashMap<String, String[]>();
        try {
            
            JAXBContext context = JAXBContext.newInstance(FeatureCollectionType.class);
            Unmarshaller um = context.createUnmarshaller();
            FeatureCollectionType value = (FeatureCollectionType)um.unmarshal(response);
            
/*            Object unmarshal = WFSMarshallerPool.getInstance().acquireUnmarshaller().unmarshal( response );

            JAXBElement<FeatureCollectionType> fc = (JAXBElement<FeatureCollectionType>) unmarshal;
            FeatureCollectionType value = fc.getValue();
*/

            List<FeaturePropertyType> featureMember = value.getFeatureMember();
            for (FeaturePropertyType member : featureMember) {
                Location loc = new LocationImpl();
                Element f = (Element) member.getUnknowFeature();
                loc.setId( getIdFromFeature( f ) );
                loc.setName( getNameFromFeature( f ) );
                float[] bbox = getBBoxFromFeature( f );
                loc.setBoundingBox( bbox[0], bbox[1], bbox[2],bbox[3] );
                // NOT SUPPORTED: loc.setIsExpired( arg0 );
                loc.setNativeKey( getNativeKeyFromFeature( f ) );
//                loc.setQualifier( arg0 );
                // get the type name from the ID through localization instead of possible value in document
                setTypeFromFeature( loc, f, typeMap );
                
                locations.add( loc );
            }
            
            // check for typeIds that are references and resolve those correctly
            for (Location l : locations) {
                String id = l.getTypeId(); 
                if (id != null && id.startsWith( "#" )) {
                    // remove reference char '#' to look in map for
                    String[] typeInfo = typeMap.get( id.substring( 1 ) );
                    l.setTypeId( typeInfo[0] );
                    l.setTypeName( typeInfo[1] );
                }
            }
            
            return locations.toArray( new LocationImpl[0] );
            
        } catch (JAXBException e) {
            log.error( "Error mapping response to location.", e );
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
    private float[] getBBoxFromFeature(Element f) {
        NodeList coords = f.getElementsByTagName( "gml:pos" );
        
        float[] box = null;
        
        if (coords.item( 0 ) != null) {
            box = getBBoxFromPosElement( coords );
        } else {
            coords = f.getElementsByTagName( "gml:posList" );
            box = getBBoxFromPosListElement( coords );
        }
        
        return box;
    }
    
    private float[] getBBoxFromPosElement(NodeList coords) {
        String[] coord1 = coords.item( 0 ).getTextContent().split( " " );
        String[] coord2 = coords.item( 1 ).getTextContent().split( " " );
        
        float[] box = new float[4];
        box[0] = Float.valueOf( coord1[0] );
        box[1] = Float.valueOf( coord1[1] );
        box[2] = Float.valueOf( coord2[0] );
        box[3] = Float.valueOf( coord2[1] );
        
        return box;
    }
    
    private float[] getBBoxFromPosListElement(NodeList coords) {
        String[] coordsSplitted = coords.item( 0 ).getTextContent().split( " " );
        
        float[] box = new float[4];
      
        // determine bounding box from polygon by getting min/max values
        for (int pos=0; pos < coordsSplitted.length; pos+=2) {
            
            if (box[1] == 0.0f || box[0] > Float.valueOf( coordsSplitted[pos] )) {
                box[1] = Float.valueOf( coordsSplitted[pos] );
            }
            if (box[0] == 0.0f || box[1] > Float.valueOf( coordsSplitted[pos+1] )) {
                box[0] = Float.valueOf( coordsSplitted[pos+1] );
            }
            
            if (box[3] < Float.valueOf( coordsSplitted[pos] )) {
                box[3] = Float.valueOf( coordsSplitted[pos] );
            }
            if (box[2] < Float.valueOf( coordsSplitted[pos+1] )) {
                box[2] = Float.valueOf( coordsSplitted[pos+1] );
            }
        }
        
        return box;
    }
    
    /**
     * Extract the ID from the document and try to determine the type name. First
     * use the localization through ResourceBundle and otherwise the value inside
     * the document.
     * @param loc is the object where the type id and name shall be added to
     * @param f is the document fragment representing the location
     * @param typeMap is a Map to store references to types
     */
    private void setTypeFromFeature( Location loc, Element f, Map<String, String[]> typeMap ) {
        NodeList types = f.getElementsByTagName( "gn:Objektart" );
        Element item = (Element) types.item( 0 );
        
        String tId = null,
                realTypeId = null,
                tName = null;
        
        // get the type ID first
        if (item != null) {
            // 
            NodeList key = item.getElementsByTagName( "gn:schluessel" );
            realTypeId = key.item( 0 ).getTextContent();
            try {
                tId = bundle.getString( "map.id.key." + realTypeId );
            } catch (MissingResourceException e) {
                tId = realTypeId;
            }
            
        } else {
            // try to find out if it has a reference to an already defined type
            NodeList hasTypes = f.getElementsByTagName( "gn:hatObjektart" );
            Element hasItem = (Element) hasTypes.item( 0 );
            String link = hasItem.getAttribute( "xlink:href" );
            if (link != null) {
                tId = link;
            }
        }
        loc.setTypeId( tId );
        
        
        // get now the type name if it's not a reference (will be handled later with!)
        if (tId != null && !tId.startsWith( "#" )) {
            try {
                tName = bundle.getString( "gazetteer.de." + realTypeId );
            } catch (MissingResourceException e) {
                log.warn( "Type name of location not found in ResourceBundle ... id=" + realTypeId );
                tName = getTypeNameFromFeature( item );
            }
            String[] typeInfo = new String[] { tId, tName };
            typeMap.put( item.getAttribute( "gml:id" ), typeInfo );
            loc.setTypeName( tName );
        }
    }

    /**
     * Get the name of the type of the location from a search result. The name
     * will be translated according to the used resource bundle.
     * @param f
     * @return the type name
     */
    private String getTypeNameFromFeature(Element f) {
        NodeList types = f.getElementsByTagName( "gn:objektart" );
        // TODO: check if it exists and try to look for "gn:wert" otherwise
        Element item = (Element) types.item( 0 );
        if (item != null) {
            try {
                return bundle.getString( "gazetteer.de." + item.getTextContent() );
            } catch (MissingResourceException e) {
                return item.getTextContent();
            }
        }
        return null;
    }
    
    /**
     * Get the native key (AGS) of the location from a search result.
     * @param f
     * @return the key
     */
    private String getNativeKeyFromFeature(Element f) {
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
    private String getIdFromFeature(Element f) {
        return f.getElementsByTagName( "gn:nnid" ).item( 0 ).getTextContent();
    }

    /**
     * Get the name of the location from a search result.
     * @param f
     * @return the name
     */
    private String getNameFromFeature(Element f) {
        NodeList endonyms = f.getElementsByTagName( "gn:Endonym" );
        
        // TODO: check for language!
        Element item = (Element) endonyms.item( 0 );
        return item.getElementsByTagName( "gn:name" ).item( 0 ).getTextContent();
        
    }

}
