package org.monarchinitiative.loinc2hpo.loinc;

import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class Hpo2LoincTermIdTest {

    private static TermPrefix pref = new ImmutableTermPrefix("HP");

    @Test
    public void testNotNegated() {
        TermId tid = new ImmutableTermId(pref,"0000001");
        Hpo2LoincTermId h2l = new Hpo2LoincTermId(tid);
        assertFalse(h2l.isNegated());
    }

    @Test
    public void testNegated() {
        TermId tid = new ImmutableTermId(pref,"0000001");
        boolean negated=true;
        Hpo2LoincTermId h2l = new Hpo2LoincTermId(tid,negated);
        assertTrue(h2l.isNegated());
    }
}
