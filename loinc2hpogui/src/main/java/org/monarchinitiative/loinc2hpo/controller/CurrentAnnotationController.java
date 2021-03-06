package org.monarchinitiative.loinc2hpo.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
//import javassist.CodeConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.loinc2hpo.codesystems.Code;
import org.monarchinitiative.loinc2hpo.codesystems.CodeSystemConvertor;
import org.monarchinitiative.loinc2hpo.codesystems.Loinc2HPOCodedValue;
import org.monarchinitiative.loinc2hpo.loinc.HpoTermId4LoincTest;
import org.monarchinitiative.loinc2hpo.loinc.LoincEntry;
import org.monarchinitiative.loinc2hpo.loinc.UniversalLoinc2HPOAnnotation;
import org.monarchinitiative.loinc2hpo.model.Annotation;
import org.monarchinitiative.loinc2hpo.model.Model;

import java.util.Map;

@Singleton
public class CurrentAnnotationController{
    private static final Logger logger = LogManager.getLogger();

    private Model model;

    private LoincEntry currentLoincEntry = null;
    private UniversalLoinc2HPOAnnotation currentAnnotation = null;

    @Inject AnnotateTabController annotateTabController;
    @Inject MainController mainController;

    @FXML private BorderPane currentAnnotationPane;

    @FXML private Label annotationTitle;
    @FXML private TextField internalCodingSystem;

    private ObservableList<Annotation> internalCodeAnnotations = FXCollections.observableArrayList();
    @FXML private TableView<Annotation> internalTableview;
    @FXML private TableColumn<Annotation, String> codeInternalTableview;
    @FXML private TableColumn<Annotation, String> hpoInternalTableview;
    @FXML private TableColumn<Annotation, Boolean> inversedInternalTableview;

    private ObservableList<Annotation> externalCodeAnnotations = FXCollections.observableArrayList();
    @FXML private TableView<Annotation> externalTableview;
    @FXML private TableColumn<Annotation, String> systemExternalTableview;
    @FXML private TableColumn<Annotation, String> codeExternalTableview;
    @FXML private TableColumn<Annotation, String> hpoExternalTableview;
    @FXML private TableColumn<Annotation, Boolean> inversedExternalTableview;

    private ObservableList<Annotation> interpretationCodeAnnotations = FXCollections.observableArrayList();
    @FXML private TableView<Annotation> interpretationTableview;
    @FXML private TableColumn<Annotation, String> systemInterpretTableview;
    @FXML private TableColumn<Annotation, String> codeInterpretTableview;
    @FXML private TableColumn<Annotation, String> hpoInterpretTableview;
    @FXML private TableColumn<Annotation, Boolean> inversedInterpretTableview;



    @FXML private void initialize() {


        internalCodingSystem.setText(Loinc2HPOCodedValue.CODESYSTEM);

        initTableStructure();

        populateTables();

    }

    private void initTableStructure() {
        codeInternalTableview.setSortable(true);
        codeInternalTableview.setCellValueFactory(cdf ->
                new ReadOnlyStringWrapper(cdf.getValue().getCode().getCode())
        );
        hpoInternalTableview.setCellValueFactory(cdf ->
                new ReadOnlyStringWrapper(cdf.getValue().getHpoTermId4LoincTest().getHpoTerm().getName()));
        inversedInternalTableview.setCellValueFactory(cdf ->
                new ReadOnlyBooleanWrapper(cdf.getValue().getHpoTermId4LoincTest().isNegated()));
        internalTableview.setItems(internalCodeAnnotations);

        systemExternalTableview.setSortable(true);
        systemExternalTableview.setCellValueFactory(cdf ->
                new ReadOnlyStringWrapper(cdf.getValue().getCode().getSystem()));
        codeExternalTableview.setSortable(true);
        codeExternalTableview.setCellValueFactory(cdf ->
                new ReadOnlyStringWrapper(cdf.getValue().getCode().getCode()));
        hpoExternalTableview.setCellValueFactory(cdf ->
                new ReadOnlyStringWrapper(cdf.getValue().getHpoTermId4LoincTest().getHpoTerm().getName()));
        inversedExternalTableview.setCellValueFactory(cdf ->
                new ReadOnlyBooleanWrapper(cdf.getValue().getHpoTermId4LoincTest().isNegated()));
        externalTableview.setItems(externalCodeAnnotations);

        systemInterpretTableview.setSortable(true);
        systemInterpretTableview.setCellValueFactory(cdf ->
                new ReadOnlyStringWrapper(cdf.getValue().getCode().getSystem()));
        codeInterpretTableview.setSortable(true);
        codeInterpretTableview.setCellValueFactory(cdf ->
                new ReadOnlyStringWrapper(cdf.getValue().getCode().getCode()));
        hpoInterpretTableview.setCellValueFactory(cdf ->
                new ReadOnlyStringWrapper(cdf.getValue().getHpoTermId4LoincTest().getHpoTerm().getName()));
        inversedInterpretTableview.setCellValueFactory(cdf ->
                new ReadOnlyBooleanWrapper(cdf.getValue().getHpoTermId4LoincTest().isNegated()));
        interpretationTableview.setItems(interpretationCodeAnnotations);
    }

    private void populateTables(){

        if (model == null) {
            logger.error("model is null.");
            return;
        }

        this.currentAnnotation = model.getCurrentAnnotation();
        LoincEntry currentLoincEntry = model.getLoincEntryMap().get(currentAnnotation.getLoincId());
        annotationTitle.setText(String.format("Annotations for Loinc: %s[%s]", currentLoincEntry.getLOINC_Number(), currentLoincEntry.getLongName()));


        internalCodeAnnotations.clear();
        currentAnnotation.getCandidateHpoTerms().entrySet()
            .stream()
            .filter(p -> p.getKey().getSystem().equals(Loinc2HPOCodedValue.CODESYSTEM))
            .map(p -> new Annotation(p.getKey(), p.getValue()))
            .forEach(internalCodeAnnotations::add);

        externalCodeAnnotations.clear();
        currentAnnotation.getCandidateHpoTerms().entrySet().stream()
                .filter(p -> !p.getKey().getSystem().equals(Loinc2HPOCodedValue.CODESYSTEM))
                .map(p -> new Annotation(p.getKey(), p.getValue()))
                .forEach(externalCodeAnnotations::add);

        interpretationCodeAnnotations.clear();
        for (Map.Entry<Code, Code> entry: CodeSystemConvertor.getCodeConversionMap().entrySet()) {
            logger.debug("key: " + entry.getKey() + "\nvalue: " + entry.getValue());
            HpoTermId4LoincTest result = currentAnnotation.loincInterpretationToHPO(entry.getValue());
            logger.debug("result is null? " + (result == null));
            if (result != null) {
                Annotation annotation = new Annotation(entry.getKey(), result);
                interpretationCodeAnnotations.add(annotation);
                logger.debug("interpretationCodeAnnotations size: " + interpretationCodeAnnotations.size());
            }
        }

        logger.debug("internalTableview is null: " + (internalTableview == null));
        logger.debug("internalCodeAnnotations is null: " + (internalTableview == null));
        logger.debug("internal annotation size: " + internalCodeAnnotations.size());
        internalCodeAnnotations.forEach(logger::info);


        logger.trace("exit initInternalTableview()");


    }
    private void ExternalTableview(){

    }
    private void initInterpretTableview(){

    }

    public void setModel(Model model) {
        if (model == null) {
            logger.trace("model for CurrentAnnotationController is set to null");
            return;
        }
        this.model = model;
        logger.info("model for CurrentAnnotationController is set");
    }


    public void setCurrentLoincEntry(LoincEntry currentLoinc) {
        this.currentLoincEntry = currentLoinc;
    }

    public void setCurrentAnnotation(UniversalLoinc2HPOAnnotation currentAnnotation) {
        this.currentAnnotation = currentAnnotation;
        logger.info("current annotation is set successfully for: " + this.currentAnnotation.getLoincId());
        initTableStructure();
    }

    @FXML
    void handleEdit(ActionEvent event) {

        logger.debug("user wants to edit current annotation");
        annotateTabController.editCurrentAnnotation(currentAnnotation);
        mainController.switchTab(MainController.TabPaneTabs.AnnotateTabe);

        Node source = (Node)  event.getSource();
        Stage stage  = (Stage) source.getScene().getWindow();
        stage.close();
        event.consume();
    }

    @FXML
    void handleSave(ActionEvent event) {
        System.out.println("user wants to save the annotation after editing");
        event.consume();
    }

    @FXML
    void setReferences(ActionEvent event) {
        System.out.println("user wants to set references");
        event.consume();

    }
}

