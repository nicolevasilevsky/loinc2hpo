package org.monarchinitiative.loinc2hpo.loinc;


import org.monarchinitiative.loinc2hpo.exception.MalformedLoincScaleException;

/**
 * LOINC defines the following scale types: Quantitative(Qn), Ordinal(Ord), Nominal(Nom), Narrative(Nar).
 */
public enum LoincScale {
    QUANTITATIVE("Qn"), ORDINAL("Ord"), NOMINAL("Nom"), NARRATIVE("Nar");

    private final String abbreviation;

    LoincScale(String val) {
        abbreviation =val;
    }

    public static LoincScale string2enum(String scale) throws MalformedLoincScaleException {
        String s=scale.toLowerCase();
        switch (s) {
            case "qn" : return QUANTITATIVE;
            case "ordinal":
            case "ord": return ORDINAL;
            case "nominal":
            case "nom": return  NOMINAL;
            case "narrative":
            case "nar": return NARRATIVE;
            default: throw new MalformedLoincScaleException("Unrecognized LOINC scale: " + scale);
        }
    }

    public String toString() {
        return this.abbreviation;
    }
}
