package org.monarchinitiative.loinc2hpo.loinc;

import com.github.phenomics.ontolib.ontology.data.TermId;

/**
 * This class wraps an HPO id by adding an additional field that specifies whether the HPO term is negated, i.e., there
 * is an assertion that the HPO term does <b>not</b> apply. For instance, if a normal blood glucose level is measured,
 * we might annotate to NOT <i>Abnormality of blood glucose concentration</i> (HP:0011015).
 */
public class Hpo2LoincTermId  {


    private boolean isNegated=false;
    private final TermId tid;

    public Hpo2LoincTermId(TermId id) {
        this.tid=id;
    }

    public Hpo2LoincTermId(TermId id, boolean negated) {
        this(id);
        isNegated=negated;
    }


    public TermId getId() {return tid; }

    public boolean isNegated() {
        return isNegated;
    }





}
