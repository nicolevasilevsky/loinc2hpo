package org.monarchinitiative.loinc2hpo.io;


import com.github.phenomics.ontolib.formats.hpo.HpoOntology;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.loinc2hpo.codesystems.Code;
import org.monarchinitiative.loinc2hpo.codesystems.CodeSystemConvertor;
import org.monarchinitiative.loinc2hpo.codesystems.Loinc2HPOCodedValue;
import org.monarchinitiative.loinc2hpo.exception.Loinc2HpoException;
import org.monarchinitiative.loinc2hpo.exception.MalformedHpoTermIdException;
import org.monarchinitiative.loinc2hpo.loinc.*;
import org.monarchinitiative.loinc2hpo.loinc.Loinc2HPOAnnotation;
import org.monarchinitiative.loinc2hpo.loinc.QnLoinc2HPOAnnotation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A clas to parse the biocurated Loinc-to-HPO file.
 * Current format
 * <pre>
 *     #FLAG	LOINC.id	LOINC.scale	HPO.low	HPO.wnl	HPO.high	note
 N	15074-8 Qn  HP:0001943  HP:0011015  HP:0003074  blood glucose test
 * </pre>
 */
public class FromFile {
    private static final Logger logger = LogManager.getLogger();


    private final HpoOntology ontology;

    private static final TermPrefix HPPREFIX = new ImmutableTermPrefix("HP");

    private Set<UniversalLoinc2HPOAnnotation> testset;

    private Set<QnLoinc2HPOAnnotation> qntests;



    private Map<LoincId, UniversalLoinc2HPOAnnotation> testmap;



    public FromFile(String loincPath, HpoOntology hpo) {
        this.ontology=hpo;
        testset=new HashSet<>();
        qntests=new HashSet<>();
        testmap=new HashMap<>();
        parseLoinc2Hpo(loincPath);
    }


    public Set<UniversalLoinc2HPOAnnotation> getTests() { return testset; };

    public Set<QnLoinc2HPOAnnotation> getQnTests() { return qntests; }


    public Map<LoincId, UniversalLoinc2HPOAnnotation> getTestmap() { return testmap; }

    private void parseLoinc2Hpo(String path) {
        logger.trace("Parsing at " + path);
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while ((line=br.readLine())!=null) {
                logger.trace("reading line: " +line);
                if (line.startsWith("#")) continue; // headr or comment
                String A[] = line.split("\t");
                String flag=A[0];
                boolean flagval=false;
               if (flag.startsWith("Y")) flagval=true;
                try {
                    LoincId id = new LoincId(A[1]);
                    LoincScale loincScale=getScale(A[2]);
                    TermId low = getHpoTermId(A[3]);
                    TermId wnl = getHpoTermId(A[4]);
                    TermId high = getHpoTermId(A[5]);
                    String comment = (A.length>5 && A[5]!=null)? A[5]:"";
                    if (loincScale.equals(LoincScale.Qn)) {
                        //Loinc2HPOAnnotation test = new QnLoinc2HPOAnnotation(id,LoincScale.Qn,low,wnl,high,flagval,comment);
                        Map<String, Code> internalCode = CodeSystemConvertor.getCodeContainer().getCodeSystemMap().get(Loinc2HPOCodedValue.CODESYSTEM);
                        UniversalLoinc2HPOAnnotation test = new UniversalLoinc2HPOAnnotation(id, loincScale)
                                .addAnnotation(internalCode.get("L"), new HpoTermId4LoincTest(low, false))
                                .addAnnotation(internalCode.get("A"), new HpoTermId4LoincTest(wnl, false))
                                .addAnnotation(internalCode.get("N"), new HpoTermId4LoincTest(wnl, true))
                                .addAnnotation(internalCode.get("H"), new HpoTermId4LoincTest(high, false))
                                .addAnnotation(internalCode.get("P"), new HpoTermId4LoincTest(high, false))
                                .addAnnotation(internalCode.get("NP"), new HpoTermId4LoincTest(wnl, true));
                        test.setFlag(flagval);
                        test.setNote(comment);
                        testset.add(test);
                        //what is the following line doing? TODO:?
                        qntests.add(new QnLoinc2HPOAnnotation(id,LoincScale.Qn,low,wnl,high));
                        testmap.put(id,test);
                    } else {

                    }

                } catch (Loinc2HpoException e) {
                    e.printStackTrace();
                    continue;
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    LoincScale getScale(String sc) {
        return LoincScale.string2enum(sc);
    }


    TermId getHpoTermId(String termString) throws MalformedHpoTermIdException {
        logger.trace("getHpoTermId="+termString);
        if (termString.equalsIgnoreCase("NA")) return null;
        int i = termString.indexOf(":");
        if (i!=2) throw new MalformedHpoTermIdException("Malformed HPO String: " + termString);
        TermId id = new ImmutableTermId(HPPREFIX,termString.substring(3));
        if (ontology.getTermMap().containsKey(id)) return id;
        else
            throw new MalformedHpoTermIdException("Could not find id "+ termString);
    }


}
