package org.monarchinitiative.loinc2hpo.fhir;


import org.monarchinitiative.loinc2hpo.exception.MalformedFhirObservationException;

/**
 * <	Off scale low
 * >	Off scale high
 * A	Abnormal
 * AA	Critically abnormal
 * AC	Anti-complementary substances present
 * B	Better
 * D	Significant change down
 * DET	Detected
 * H	High
 * HH	Critically high
 * HM	Hold for Medical Review
 * HU	Very high
 * I	Intermediate
 * IE	Insufficient evidence
 * IND	Indeterminate
 * L	Low
 * LL	Critically low
 * LU	Very low
 * MS	Moderately susceptible. Indicates for microbiology susceptibilities only.
 * N	Normal
 * ND	Not Detected
 * NEG	Negative
 * NR	Non-reactive
 * NS	Non-susceptible
 * null	No range defined, or normal ranges don't apply
 * OBX	Interpretation qualifiers in separate OBX segments
 * POS	Positive
 * QCF	Quality Control Failure
 * R	Resistant
 * RR	Reactive
 * S	Susceptible
 * SDD	Susceptible-dose dependent
 * SYN-R	Synergy - resistant
 * SYN-S	Synergy - susceptible
 * TOX	Cytotoxic substance present
 * U	Significant change up
 * VS	Very susceptible. Indicates for microbiology susceptibilities only.
 * W	Worse
 * WR	Weakly reactive
 * These are the codes for FHIR interpretations of observations.
 **/
public enum FhirObservationCode {
    OFF_SCALE_LOW("<"),
    OFF_SCALE_HIGH(">"),
    ABNORMAL("A"),
    CRITICALLY_ABNORMAL("AA"),
    ANTI_COMPLEMENTARY_SUBSTANCES_PRESENT("AC"),
    BETTER("B"),
    SIGNIFICANT_CHANGE_DOWN("D"),
    DETECTED("DET"),
    HIGH("H"),
    CRITICALLY_HIGH("HH"),
    HOLD_FOR_MEDICAL_REVIEW("HM"),
    VERY_HIGH("HU"),
    INTERMEDIATE("I"),
    INSUFFICIENT_EVIDENCE("IE"),
    INDETERMINATE("IND"),
    LOW("L"),
    CRITICALLY_LOW("LL"),
    VERY_LOW("LU"),
    MODERATELY_SUSCEPTIBLE("MS"), //Indicates for microbiology susceptibilities only.
    NORMAL("N"),
    NOT_DETECTED("ND"),
    NEGATIVE("NEG"),
    NON_REACTIVE("NR"),
    NON_SUSCEPTIBLE("NS"),
    NO_RANGE_DEFINED("null"),
    INTERPRETATION_QUANTIFIERS_IN_SEPARATE_OBX_SEGMENTS("OBX"),
    POSITIVE("POS"),
    QUALITY_CONTROL_FAILURE("QCF"),
    RESISTANT("R"),
    REACTIVE("RR"),
    SUSCEPTIBLE("S"),
    SUSCEPTIBLE_DOSE_DEPENDENT("SDD"),
    SYNERGY_RESISTANT("SYN-R"),
    SYNERGY_SUSCEPTIBLE("SYN-S"),
    CYTOTOXIC_SUBSTANCE_PRESENT("TOX"),
    SIGNIFICANT_CHANGE_UP("U"),
    VERY_SUSCEPTIBLE("VS"), //Indicates for microbiology susceptibilities only.
    WORSE("W"),
    WEAKLY_REACTIVE("WR");


    private final String abbreviation;

    FhirObservationCode(String val) {
        abbreviation =val;
    }

    public static FhirObservationCode string2enum(String code) throws MalformedFhirObservationException {
        switch (code) {
            case "<" : return OFF_SCALE_LOW;
            case ">": return OFF_SCALE_HIGH;
            case "A": return ABNORMAL;
            case "AA": return CRITICALLY_ABNORMAL;
            case "AC": return ANTI_COMPLEMENTARY_SUBSTANCES_PRESENT;
            case "B": return BETTER;
            case "D": return SIGNIFICANT_CHANGE_DOWN;
            case "DET": return DETECTED;
            case "H": return HIGH;
            case "HH": return CRITICALLY_HIGH;
            case "HM": return  HOLD_FOR_MEDICAL_REVIEW;
            case "HU": return VERY_HIGH;
            case "I": return INTERMEDIATE;
            case "IE": return INSUFFICIENT_EVIDENCE;
            case "IND": return INDETERMINATE;
            case "L": return LOW;
            case "LL": return CRITICALLY_LOW;
            case "LU": return VERY_LOW;
            case "MS": return MODERATELY_SUSCEPTIBLE;
            case "N": return NORMAL;
            case "ND": return NOT_DETECTED;
            case "NEG": return NEGATIVE;
            case "NR": return NON_REACTIVE;
            case "NS": return NON_SUSCEPTIBLE;
            case "null": return NO_RANGE_DEFINED;
            case "OBX": return INTERPRETATION_QUANTIFIERS_IN_SEPARATE_OBX_SEGMENTS;
            case "POS": return POSITIVE;
            case "QCF": return QUALITY_CONTROL_FAILURE;
            case "R": return RESISTANT;
            case "RR": return REACTIVE;
            case "S": return SUSCEPTIBLE;
            case "SDD": return SUSCEPTIBLE_DOSE_DEPENDENT;
            case "SYN-R": return SYNERGY_RESISTANT;
            case "SYN-S": return SYNERGY_SUSCEPTIBLE;
            case "TOX": return CYTOTOXIC_SUBSTANCE_PRESENT;
            case "U": return SIGNIFICANT_CHANGE_UP;
            case "VS": return VERY_SUSCEPTIBLE;
            case "W": return WORSE;
            case "WR": return WEAKLY_REACTIVE;
            default: throw new MalformedFhirObservationException("Unrecognized Fhir observation code: " + code);
        }
    }

    public String toString() {
        return this.abbreviation;
    }
}
