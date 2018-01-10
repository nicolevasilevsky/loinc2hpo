package org.monarchinitiative.loinc2hpo.fhir;

import org.junit.Test;
import org.monarchinitiative.loinc2hpo.exception.MalformedFhirObservationException;
import org.monarchinitiative.loinc2hpo.exception.MalformedLoincScaleException;

import static org.junit.Assert.assertEquals;

public class FhirObservationCodeTest {

    @Test
    public void testAbnormal() throws MalformedFhirObservationException {
        FhirObservationCode code = FhirObservationCode.string2enum("A");
        assertEquals(FhirObservationCode.ABNORMAL,code);
    }

    @Test
    public void testLow() throws MalformedFhirObservationException {
        FhirObservationCode code = FhirObservationCode.string2enum("L");
        assertEquals(FhirObservationCode.LOW,code);
    }

    @Test
    public void testHigh() throws MalformedFhirObservationException {
        FhirObservationCode code = FhirObservationCode.string2enum("H");
        assertEquals(FhirObservationCode.HIGH,code);
    }

    @Test(expected = MalformedFhirObservationException.class)
    public void testMalformed() throws MalformedFhirObservationException {
        FhirObservationCode code = FhirObservationCode.string2enum("malformed");
        assertEquals(FhirObservationCode.HIGH,code);
    }

}
