package de.ingrid.external.wfs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import org.junit.BeforeClass;
import org.junit.Test;

import de.ingrid.external.om.Location;

public class WFSServiceTest {

    private static WFSService service;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        service = new WFSService();
        service.init();
    }

    // see:
    // https://svn.kenai.com/svn/envision~portal/common/discovery-csw/src/test/java/at/sti2/envision/discovery/geotoolkit/GeoToolkitTest_v110.java

    @Test
    public void findLocation() throws Exception {
        
        Location[] result = service.findLocationsFromQueryTerm( "Berlin", null, null, null );
        
        assertThat( result, is( not( nullValue() ) ));
        assertThat( result.length, is( 9 ));
        for (Location location : result) {
            assertThat( location.getBoundingBox(), is( not( nullValue() ) ) );
            assertThat( location.getId(), is( not( nullValue() ) ) );
            assertThat( location.getName(), is( not( nullValue() ) ) );
            //assertThat( location.getNativeKey(), is( not( nullValue() ) ) ); // not all locations have an AGS
            assertThat( location.getTypeId(), is( not( nullValue() ) ) );
            assertThat( location.getTypeName(), is( not( nullValue() ) ) );
            
        }
        
    }
    
    @Test
    public void getLocation() {
        Location location = service.getLocation( "DEBKGGND00001GFQ", null ); // Berlin (Bundesland)
        assertThat( location, is( not( nullValue() ) ));
        assertThat( location.getBoundingBox(), is( new float[] { 13.0883332179289f, 52.3382418357021f, 13.760469283944f, 52.6749171494323f } ) );
        assertThat( location.getId(), is( "DEBKGGND00001GFQ" ) );
        assertThat( location.getName(), is( "Berlin" ) );
        assertThat( location.getNativeKey(), is( "11000000" ) );
        assertThat( location.getTypeId(), is( "Obj_1666" ) );
        assertThat( location.getTypeName(), is( "AX_Bundesland" ) );
    }
    

}
