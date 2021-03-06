package org.monarchinitiative.loinc2hpo.loinc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.monarchinitiative.loinc2hpo.codesystems.Code;
import org.monarchinitiative.loinc2hpo.codesystems.CodeSystemConvertor;
import org.monarchinitiative.loinc2hpo.codesystems.Loinc2HPOCodedValue;
import org.monarchinitiative.loinc2hpo.util.HPO_Class_Found;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO: implement Json serialization
 * This class is responsible for managing the annotation information. The app keeps a map from loinc -> annotation. This
 * class is only the annotation part. For each loinc code, we assign a candidate Hpo term for a potential observation
 * value. The observation value is a code in a coding system (if it is a numeric value, we change it to a code). For Qn
 * type of Loinc, we use the internal code:
 * L(ow)                -> Hpo term
 * A(bnormal)/N(ormal)  -> Hpo term
 * H(igh)               -> Hpo term
 * P(ositive)           -> Hpo term
 * N(ot)P(ositive)      -> Hpo term
 *
 * For Ord, Nom and other types, the observation is always a code in an external coding system, we have to assign Hpo
 * terms to expected coded values, or we have to convert the external coded value to an internal coded value listed above.
 *
 * Generally, we first consider the interpretation field and try to utilize this information so that we can avoid the
 * necessity to map various coding systems. We have a built-in map that converts fhir interpretation codes to our internal
 * code. We still need to have more maps for different interpretation systems.
 */

@JsonIgnoreProperties({"codeSystems", "unrecognizedCodes"})
//@JsonInclude(JsonInclude.Include.NON_NULL.NON_EMPTY)
public class UniversalLoinc2HPOAnnotation implements Serializable {

    @JsonIgnore
    private static final long serialVersionUID = 1L;

    @JsonProperty("version")
    private double version = 0.0;
    private LocalDate createdOn = null;
    private String createdBy = null;
    private LocalDate lastEditedOn = null;
    private String lastEditedBy = null;

    @JsonProperty("loinc id")
    private LoincId loincId = null;
    @JsonProperty("loinc scale")
    private LoincScale loincScale = null;
    //the keys are internal codes; each one should correspond to one HpoTerm4LoincTest
    //alternatively, the codes can be external codes, if it is for a Ord or Nom loinc test
    //access the codes from the CodeSystemConvertor.getCodeContainer
    @JsonProperty("annotations")
    private HashMap<Code, HpoTermId4LoincTest> candidateHpoTerms = new HashMap<>();
    @JsonProperty("note")
    private String note = null; //any comment for this annotation, say e.g. "highly confident about this annotation"
    @JsonProperty("flag")
    private boolean flag = false; //a simpler version that equals a comment "not sure about the annotation, come back later"

    private Set<String> codeSystems; //what code systems are used for annotation
    private Set<Code> unrecognizedCodes; //keep a record if a code is not annotated but used in real-world observation

    //private UniversalLoinc2HPOAnnotation(){ }

    public UniversalLoinc2HPOAnnotation(LoincId lid, LoincScale lsc){

        //super(lid, lsc);
        this.loincId = lid;
        this.loincScale = lsc;

    }

    public UniversalLoinc2HPOAnnotation(LoincId lid) {
        this.loincId = lid;
        //loincScale can be found from the loinc map
    }

    /**
     * Add annotations
     * @param code a coded value in a coding system
     * @param hpoTermId4LoincTest a hpo term wrapped in the HpoTermId4LoincTest class
     * @return the annotation object
     */
    public UniversalLoinc2HPOAnnotation addAnnotation(Code code, HpoTermId4LoincTest hpoTermId4LoincTest) {
        this.candidateHpoTerms.put(code, hpoTermId4LoincTest);
        return this;
    }

    /**
     * Add multiple annotation at once
     * @param annotation a map of <Code, HpoTermId4LoincTest> annotations
     * @return the annotation object
     */
    public UniversalLoinc2HPOAnnotation addAnnotation(Map<Code, HpoTermId4LoincTest> annotation){
        this.candidateHpoTerms.putAll(annotation);
        return this;
    }


    public UniversalLoinc2HPOAnnotation setLoincId(LoincId loincId){
        this.loincId = loincId;
        return this;
    }
    public LoincId getLoincId(){ return this.loincId; }

    public UniversalLoinc2HPOAnnotation setLoincScale(LoincScale scale){
        this.loincScale = scale;
        return this;
    }

    public LoincScale getLoincScale() { return this.loincScale;}

    /**
     * Add the note for the annotation.
     * @param note something that explains the quality or reasoning behind the annotation
     * @return the annotation object
     */
    public UniversalLoinc2HPOAnnotation setNote(String note){
        this.note = note;
        return this;
    }

    /**
     * Add a flag for the annotation
     * @param flag if true, it means that annotation is worth a re-visit
     * @return the annotation object
     */
    public UniversalLoinc2HPOAnnotation setFlag(boolean flag) {
        this.flag = flag;
        return this;
    }

    //@Override
    public  String getNote(){
        return this.note;
    }

    //@Override
    public boolean getFlag(){
        return this.flag;
    }
    /**
     * When we run the software to parse patient information, if we cannot interpret the observation result due to the
     * lack of annotation information, we will keep a record of the unrecognized coding system.
     * @param code
     */
    public void addUnrecognizedCode(Code code){
        this.unrecognizedCodes.add(code);
    }

    /**
     * When we run the software to parse patient information, if we cannot interpret the observation result due to the
     * lack of annotation information, we will keep a record of the unrecognized codes (includes code id, system etc).
     * @param code a code that the app cannot recognize
     * @return the annotation object
     */
    public Set<Code> getUnrecognizedCodes(Code code) {
        return this.unrecognizedCodes;
    }

    /**
     * The functions provides a way to access all the coding systems used in the annotation.
     * @return a set of code system names
     */
    public Set<String> getCodeSystems(){
        Set<String> codeSystems = new HashSet<>();
        candidateHpoTerms.keySet().forEach(x -> codeSystems.add(x.getSystem()));
        return codeSystems;
    }

    /**
     *
     * @return
     */
    //@Override
    public Set<Code> getCodes(){
        return candidateHpoTerms.keySet();
    }


    /**
     * Get the corresponding Hpo term for a coded value
     * @param code a code in a coding system. Usually, it is the internal code; for Ord, Nom, or Nar, it can be codes of
     *             an external coding system
     * @return the hpo term wrapped in the HpoTermId4LoincTest class
     */
    public HpoTermId4LoincTest loincInterpretationToHPO(Code code) {
        return candidateHpoTerms.get(code);
    }


    public static String getHeader() {
        String header = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                "loincId", "loincScale", "system", "code", "hpoTermId", "inversed", "note", "flag",
                "version", "createdOn", "createdBy", "lastEditedOn", "lastEditedBy");
        return header;
    }

    public HashMap<Code, HpoTermId4LoincTest> getCandidateHpoTerms() {
        return new HashMap<>(candidateHpoTerms);
    }

    public void setCandidateHpoTerms(HashMap<Code, HpoTermId4LoincTest> candidateHpoTerms) {
        this.candidateHpoTerms = candidateHpoTerms;
    }

    @Override
    public String toString(){

        StringBuilder stringBuilder = new StringBuilder();
        candidateHpoTerms.forEach((code, hpoTermId4LoincTest) -> {
            stringBuilder.append(this.loincId);
            stringBuilder.append("\t" + this.loincScale.toString());
            stringBuilder.append("\t" + code.getSystem());
            stringBuilder.append("\t" + code.getCode());
            stringBuilder.append("\t" + hpoTermId4LoincTest.getId().getIdWithPrefix());
            stringBuilder.append("\t" + hpoTermId4LoincTest.isNegated());
            stringBuilder.append("\t" + this.note);
            stringBuilder.append("\t" + this.flag);
            stringBuilder.append("\t" + this.version);
            stringBuilder.append("\t" + this.createdOn);
            stringBuilder.append("\t" + this.createdBy);
            stringBuilder.append("\t" + this.lastEditedOn);
            stringBuilder.append("\t" + this.lastEditedBy);
            stringBuilder.append("\n");
        });

        return stringBuilder.toString().trim();
    }

    public double getVersion() {
        return version;
    }

    public UniversalLoinc2HPOAnnotation setVersion(double version) {
        this.version = version;
        return this;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public UniversalLoinc2HPOAnnotation setCreatedOn(LocalDate createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public UniversalLoinc2HPOAnnotation setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public LocalDate getLastEditedOn() {
        return lastEditedOn;
    }

    public UniversalLoinc2HPOAnnotation setLastEditedOn(LocalDate lastEditedOn) {
        this.lastEditedOn = lastEditedOn;
        return this;
    }

    public String getLastEditedBy() {
        return lastEditedBy;
    }

    public UniversalLoinc2HPOAnnotation setLastEditedBy(String lastEditedBy) {
        this.lastEditedBy = lastEditedBy;
        return this;
    }

    private TermId getHpoTermIdForInternalCode(String internalCode){
        Code code = CodeSystemConvertor.getCodeContainer().getCodeSystemMap().get(Loinc2HPOCodedValue.CODESYSTEM).get(internalCode);
        if (candidateHpoTerms.get(code) == null) {
            return null;
        } else {
            return candidateHpoTerms.get(code).getId();
        }

    }

    //@Override
    @Deprecated
    public  TermId getBelowNormalHpoTermId(){
        return getHpoTermIdForInternalCode("L");
    }
    //@Override
    @Deprecated
    public  TermId getNotAbnormalHpoTermName(){
        return getHpoTermIdForInternalCode("N");
    }
    //@Override
    @Deprecated
    public TermId getAbnormalHpoTermName() {
        return getHpoTermIdForInternalCode("A");
    }
    //@Override
    @Deprecated
    public  TermId getAboveNormalHpoTermName(){

        return getHpoTermIdForInternalCode("H");
    }

    @Deprecated
    public HpoTermId4LoincTest loincInterpretationToHpo(ObservationResultInInternalCode obs){
        return null;
    }

}
