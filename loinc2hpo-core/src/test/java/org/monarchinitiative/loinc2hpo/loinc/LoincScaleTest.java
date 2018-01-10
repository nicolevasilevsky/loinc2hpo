package org.monarchinitiative.loinc2hpo.loinc;

import org.junit.Test;
import org.monarchinitiative.loinc2hpo.exception.MalformedLoincScaleException;

import static org.junit.Assert.assertEquals;

public class LoincScaleTest {


    @Test
    public void testOrdinal() throws MalformedLoincScaleException {
        LoincScale scale = LoincScale.string2enum("Ord");
        assertEquals(LoincScale.ORDINAL,scale);
    }

    @Test
    public void testNominal()throws MalformedLoincScaleException {
        LoincScale scale = LoincScale.string2enum("Nom");
        assertEquals(LoincScale.NOMINAL,scale);
    }

    @Test
    public void testNarrative()throws MalformedLoincScaleException {
        LoincScale scale = LoincScale.string2enum("Nar");
        assertEquals(LoincScale.NARRATIVE,scale);
    }

    @Test
    public void testQuantitative() throws MalformedLoincScaleException{
        LoincScale scale = LoincScale.string2enum("Qn");
        assertEquals(LoincScale.QUANTITATIVE,scale);
    }

    @Test(expected =  MalformedLoincScaleException.class)
    public void testMalformedLoincScale() throws MalformedLoincScaleException{
        LoincScale scale = LoincScale.string2enum("Malformed");
    }


}
