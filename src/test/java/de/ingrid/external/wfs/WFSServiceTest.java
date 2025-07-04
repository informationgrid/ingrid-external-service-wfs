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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.ingrid.external.GazetteerService.MatchingType;
import de.ingrid.external.om.Location;

public class WFSServiceTest {

    private static WFSService service;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        service = new WFSService();
        service.init();
    }

    // see:
    // https://svn.kenai.com/svn/envision~portal/common/discovery-csw/src/test/java/at/sti2/envision/discovery/geotoolkit/GeoToolkitTest_v110.java

    @Test
    void findLocation() {

        Location[] result = service.findLocationsFromQueryTerm("Berlin", null, null, null);

        assertThat(result, is(not(nullValue())));
        assertThat(result.length, greaterThan(10));
        for (Location location : result) {
            assertThat(location.getBoundingBox(), is(not(nullValue())));
            assertThat(location.getId(), is(not(nullValue())));
            assertThat(location.getName(), is(not(nullValue())));
            //assertThat( location.getNativeKey(), is( not( nullValue() ) ) ); // not all locations have an AGS
            assertThat(location.getTypeId(), is(not(nullValue())));
            assertThat(location.getTypeName(), is(not(nullValue())));

        }

        result = service.findLocationsFromQueryTerm("Berlin", null, MatchingType.BEGINS_WITH, null);
        assertThat(result, is(not(nullValue())));
        assertThat(result.length, greaterThan(6));
        result = service.findLocationsFromQueryTerm("Berlin", null, MatchingType.CONTAINS, null);
        assertThat(result, is(not(nullValue())));
        assertThat(result.length, greaterThan(10));
        result = service.findLocationsFromQueryTerm("Berlin", null, MatchingType.EXACT, null);
        assertThat(result, is(not(nullValue())));
        assertThat(result.length, greaterThan(2));

        result = service.findLocationsFromQueryTerm("Niedersachsen*", null, MatchingType.EXACT, null);
        assertThat(result, is(not(nullValue())));
        assertThat(result.length, is(1));
        assertThat(result[0].getName(), is("Niedersachsen"));
    }

    @Test
    void getLocation() {
        Location location = service.getLocation("DEBKGGND00001GFQ", null); // Berlin (Bundesland)
        assertThat(location, is(not(nullValue())));
        assertThat(String.valueOf(location.getBoundingBox()[0]), startsWith("13.0"));
        assertThat(String.valueOf(location.getBoundingBox()[1]), startsWith("52.3"));
        assertThat(String.valueOf(location.getBoundingBox()[2]), startsWith("13.7"));
        assertThat(String.valueOf(location.getBoundingBox()[3]), startsWith("52.6"));
        assertThat(location.getId(), is("DEBKGGND00001GFQ"));
        assertThat(location.getName(), is("Berlin"));
        assertThat(location.getNativeKey(), is("11"));
        assertThat(location.getTypeId(), is("use2Type"));
        assertThat(location.getTypeName(), is("Bundesland"));
    }

    @Test
    void testMapping() {
        // check if mapping is not interrupted by single incorrect featureMember
        Location[] result = service.findLocationsFromQueryTerm("*Hannover*", null, null, null);

        assertThat(result, is(not(nullValue())));
        assertThat(result.length, equalTo(2));
        for (Location location : result) {
            assertThat(location.getBoundingBox(), is(not(nullValue())));
            assertThat(location.getId(), is(not(nullValue())));
            assertThat(location.getName(), is(not(nullValue())));
            assertThat(location.getTypeId(), is(not(nullValue())));
            assertThat(location.getTypeName(), is(not(nullValue())));

        }
    }
    

}
