package org.monarchinitiative.loinc2hpo.controller;


//import apple.laf.JRSUIUtils;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.*;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.loinc2hpo.codesystems.Code;
import org.monarchinitiative.loinc2hpo.codesystems.CodeSystemConvertor;
import org.monarchinitiative.loinc2hpo.codesystems.Loinc2HPOCodedValue;
import org.monarchinitiative.loinc2hpo.exception.LoincCodeNotFoundException;
import org.monarchinitiative.loinc2hpo.exception.MalformedLoincCodeException;
import org.monarchinitiative.loinc2hpo.exception.NetPostException;
import org.monarchinitiative.loinc2hpo.github.GitHubLabelRetriever;
import org.monarchinitiative.loinc2hpo.github.GitHubPoster;
import org.monarchinitiative.loinc2hpo.gui.GitHubPopup;
import org.monarchinitiative.loinc2hpo.gui.Main;
import org.monarchinitiative.loinc2hpo.gui.PopUps;
import org.monarchinitiative.loinc2hpo.io.LoincOfInterest;
import org.monarchinitiative.loinc2hpo.io.OntologyModelBuilderForJena;
import org.monarchinitiative.loinc2hpo.loinc.*;
import org.monarchinitiative.loinc2hpo.model.Annotation;
import org.monarchinitiative.loinc2hpo.model.Model;
import org.monarchinitiative.loinc2hpo.util.HPO_Class_Found;
import org.monarchinitiative.loinc2hpo.util.LoincCodeClass;
import org.monarchinitiative.loinc2hpo.util.LoincLongNameParser;
import org.monarchinitiative.loinc2hpo.util.SparqlQuery;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.jena.sparql.vocabulary.VocabTestQuery.query;


@Singleton
public class AnnotateTabController {
    private static final Logger logger = LogManager.getLogger();

    private Model model=null;

    @Inject
    private Injector injector;

    /** Reference to the third tab. When the user adds a new annotation, we update the table, therefore, we need a reference. */
    @Inject private Loinc2HpoAnnotationsTabController loinc2HpoAnnotationsTabController;
    private ImmutableMap<LoincId,LoincEntry> loincmap=null;



    //private final Stage primarystage;
    @FXML private Button initLOINCtableButton;
    @FXML private Button IntializeHPOmodelbutton;
    @FXML private Button filterButton;
    @FXML private Button searchForLOINCIdButton;
    @FXML private Button createAnnotationButton;
    @FXML private TextField loincSearchTextField;
    @FXML private Button filterLoincTableByList;
    @FXML private TextField userInputForManualQuery;

    //drag and drop to the following fields
    private boolean advancedAnnotationModeSelected = false;
    @FXML private Label annotationLeftLabel;
    @FXML private Label annotationMiddleLabel;
    @FXML private Label annotationRightLabel;
    @FXML private TextField annotationTextFieldLeft;
    @FXML private TextField annotationTextFieldMiddle;
    @FXML private TextField annotationTextFieldRight;
    @FXML private CheckBox inverseChecker;
    @FXML private Button addCodedAnnotationButton;
    private HPO_Class_Found hpo_drag_and_drop;
    //private ImmutableMap<String, HPO_Class_Found> selectedHPOforAnnotation;

    private ImmutableMap<String,HpoTerm> termmap;

    @FXML private ListView hpoListView;



    @FXML private Accordion accordion;
    @FXML private TitledPane loincTableTitledpane;
    @FXML private TableView<LoincEntry> loincTableView;
    @FXML private TableColumn<LoincEntry, String> loincIdTableColumn;
    @FXML private TableColumn<LoincEntry, String> componentTableColumn;
    @FXML private TableColumn<LoincEntry, String> propertyTableColumn;
    @FXML private TableColumn<LoincEntry, String> timeAspectTableColumn;
    @FXML private TableColumn<LoincEntry, String> methodTableColumn;
    @FXML private TableColumn<LoincEntry, String> scaleTableColumn;
    @FXML private TableColumn<LoincEntry, String> systemTableColumn;
    @FXML private TableColumn<LoincEntry, String> nameTableColumn;


    @FXML private Button modeButton;
    @FXML private TitledPane advancedAnnotationTitledPane;
    @FXML private TableView<Annotation> advancedAnnotationTable;
    @FXML private TableColumn<Annotation, String> advancedAnnotationSystem;
    @FXML private TableColumn<Annotation, String> advancedAnnotationCode;
    @FXML private TableColumn<Annotation, String> advancedAnnotationHpo;
    private ObservableList<Annotation> tempAdvancedAnnotations = FXCollections.observableArrayList();

    //candidate HPO classes found by Sparql query
    //@FXML private TableView<HPO_Class_Found> candidateHPOList;
    //@FXML private TableColumn<HPO_Class_Found, Integer> score;
    //@FXML private TableColumn<HPO_Class_Found, String> id;
    //@FXML private TableColumn<HPO_Class_Found, String> label;
    //@FXML private TableColumn<HPO_Class_Found, String> definition;

    @FXML private TreeView<HPO_TreeView> treeView;

    @FXML private CheckBox flagForAnnotation;
    @FXML private Circle createAnnotationSuccess;
    @FXML private TextArea annotationNoteField;
    @FXML private Button clearButton;
    @FXML private Button allAnnotationsButton;


    @FXML private Button suggestHPOButton;
    @FXML private ContextMenu contextMenu;

    @FXML private Button autoQueryButton;
    @FXML private Button manualQueryButton;


    @Inject private CurrentAnnotationController currentAnnotationController;

    @FXML private void initialize() {
        if (model != null) {   //weird line. model is set by main controller; this line never runs
            setModel(model);
            //currentAnnotationController.setModel(model); //let current annotation stage have access to model
        }
        //currentAnnotationController.setModel(model); //let current annotation stage have access to model
        suggestHPOButton.setTooltip(new Tooltip("Suggest new HPO terms"));
        filterButton.setTooltip(new Tooltip("Filter Loinc by providing a Loinc list in txt file"));
        addCodedAnnotationButton.setTooltip(new Tooltip("Add current annotation"));
        flagForAnnotation.setTooltip(new Tooltip("Check if you are not confident"));
        clearButton.setTooltip(new Tooltip("Clear all textfields"));
        allAnnotationsButton.setTooltip(new Tooltip("Display annotations for currently selected Loinc code"));
        initLOINCtableButton.setTooltip(new Tooltip("Initialize Loinc Core Table. Download it first."));
        IntializeHPOmodelbutton.setTooltip(new Tooltip("Load hp.owl as a RDF model for query"));
        searchForLOINCIdButton.setTooltip(new Tooltip("Search Loinc with a Loinc code or name"));
        modeButton.setTooltip(new Tooltip("Switch between basic and advanced annotation mode"));
        autoQueryButton.setTooltip(new Tooltip("Find candidate HPO terms with automatically generated keys"));
        manualQueryButton.setTooltip(new Tooltip("Find candidate HPO terms with manually typed keys"));

    }


    private void noLoincEntryAlert(){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Selection ERROR");
            alert.setHeaderText("Select a row in Loinc table");
            alert.setContentText("A loinc code is required for ranking " +
                    "candidate HPO terms. Select one row in the loinc " +
                    "table and query again.");
            alert.showAndWait();
    }

    private void clearAbnormalityTextField(){
        annotationTextFieldLeft.setText("");
        annotationTextFieldRight.setText("");
        annotationTextFieldMiddle.setText("");
    }


    /** Initialize the Model reference and set up the HPO autocomplete if possible. */
    public void setModel(Model m) {
        logger.trace("Setting model in AnnotateTabeController");
        model=m;
        if (model.getPathToHpoOboFile()==null) {
            logger.error("Path to hp.obo file is null. Cannot initialize autocomplete");
            return;
        }
        model.parseOntology();
        termmap = model.getTermMap();
//        WidthAwareTextFields.bindWidthAwareAutoCompletion(annotationTextFieldLeft, termmap.keySet());
//        WidthAwareTextFields.bindWidthAwareAutoCompletion(annotationTextFieldMiddle, termmap.keySet());
//        WidthAwareTextFields.bindWidthAwareAutoCompletion(annotationTextFieldRight, termmap.keySet());
        logger.trace(String.format("Initializing term map to %d terms",termmap.size()));
    }


    private void initTableStructure() {
        loincIdTableColumn.setSortable(true);
        loincIdTableColumn.setCellValueFactory(cdf ->
                new ReadOnlyStringWrapper(cdf.getValue().getLOINC_Number().toString())
        );
        componentTableColumn.setSortable(true);
        componentTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getComponent()));
        propertyTableColumn.setSortable(true);
        propertyTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getProperty()));
        timeAspectTableColumn.setSortable(true);
        timeAspectTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getTimeAspect()));
        methodTableColumn.setSortable(true);
        methodTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getMethod()));
        scaleTableColumn.setSortable(true);
        scaleTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getScale()));
        systemTableColumn.setSortable(true);
        systemTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getSystem()));
        nameTableColumn.setSortable(true);
        nameTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getLongName()));
        //hpoListView.setOrientation(Orientation.HORIZONTAL);

        loincTableView.setRowFactory( tv -> {
            TableRow<LoincEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    LoincEntry rowData = row.getItem();
                    if (model.getLoincUnderEditing() == null || //not under Editing mode
                            //or query the loinc code under editing
                            (model.getLoincUnderEditing() != null && model.getLoincUnderEditing().equals(rowData))) {
                        initHpoTermListView(rowData);
                    } else {
                        PopUps.showInfoMessage("You are currently editing " + model.getLoincUnderEditing().getLOINC_Number() +
                                        ". Save or cancel editing current loinc annotation before switching to others",
                                "Under Editing mode");
                    }

                    //clear text in abnormality text fields if not currently editing a term
                    if (!createAnnotationButton.getText().equals("Save")) { //under saving mode
                        clearAbnormalityTextField();
                        //inialize the flag field
                        flagForAnnotation.setIndeterminate(false);
                        flagForAnnotation.setSelected(false);
                        createAnnotationSuccess.setFill(Color.WHITE);
                        annotationNoteField.setText("");
                    }
                }
            });
            return row ;
        });

        accordion.setExpandedPane(loincTableTitledpane);
    }



    private void initHpoTermListView(LoincEntry entry) {
        if(SparqlQuery.model == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("HPO Model Undefined");
            alert.setHeaderText("Create HPO model first before querying");
            alert.setContentText("Click \"Initialize HPO model\" to create an" +
                    " HPO model for Sparql query. Click and query again.");
            alert.showAndWait();
            return;
        }
        String name = entry.getLongName();
        List<HPO_Class_Found> queryResults = SparqlQuery.query_auto(name);
        if (queryResults.size() != 0) {
            ObservableList<HPO_Class_Found> items = FXCollections.observableArrayList();
            for (HPO_Class_Found candidate: queryResults) {
                items.add(candidate);
            }
            this.hpoListView.setItems(items);
            //items.add("0 result is found. Try manual search with synonyms.");
        } else {
            ObservableList<String> items = FXCollections.observableArrayList();
            items.add("0 HPO class is found. Try manual search with " +
                    "alternative keys (synonyms)");
            this.hpoListView.setItems(items);
        }
    }

    @FXML private void handleAutoQueryButton(ActionEvent e){
        e.consume();
        LoincEntry entry = loincTableView.getSelectionModel()
                .getSelectedItem();
        if (entry == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Selection ERROR");
            alert.setHeaderText("Select a row in Loinc table");
            alert.setContentText("A loinc code is required for ranking " +
                    "candidate HPO terms. Select one row in the loinc " +
                    "table and query again.");
            alert.showAndWait();
            return;
        }
        logger.info(String.format("Start auto query for \"%s\"by pressing button",entry));
        if (model.getLoincUnderEditing() == null || //not under Editing mode
                //or query the loinc code under editing
                (model.getLoincUnderEditing() != null && model.getLoincUnderEditing().equals(entry))) {
            initHpoTermListView(entry);
        } else {
            PopUps.showInfoMessage("You are currently editing " + model.getLoincUnderEditing().getLOINC_Number() +
                            ". Save or cancel editing current loinc annotation before switching to others",
                    "Under Editing mode");
            return;
        }


        //clear text in abnormality text fields if not currently editing a term
        if (!createAnnotationButton.getText().equals("Save")) {
            clearAbnormalityTextField();
            //inialize the flag field
            flagForAnnotation.setIndeterminate(false);
            flagForAnnotation.setSelected(false);
            createAnnotationSuccess.setFill(Color.WHITE);
            annotationNoteField.setText("");
        }
    }

    @FXML private void handleManualQueryButton(ActionEvent e) {

        e.consume();
        if(SparqlQuery.model == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("HPO Model Undefined");
            alert.setHeaderText("Create HPO model first before querying");
            alert.setContentText("Click \"Initialize HPO model\" to create an" +
                    " HPO model for Sparql query. Click and query again.");
            alert.showAndWait();
            return;
        }

        //for now, force user choose a loinc entry. TODO: user may or may not
        // choose a loinc term.
        LoincEntry entry = loincTableView.getSelectionModel().getSelectedItem();
        if (entry == null) {
            noLoincEntryAlert();
            return;
        }


        if (model.getLoincUnderEditing() != null && !model.getLoincUnderEditing().equals(entry)) {

            PopUps.showInfoMessage("You are currently editing " + model.getLoincUnderEditing().getLOINC_Number() +
                            ". Save or cancel editing current loinc annotation before switching to others",
                    "Under Editing mode");
            return;
        }


        String userInput = userInputForManualQuery.getText();
        if (userInput == null || userInput.trim().length() < 2) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Input Error");
            alert.setHeaderText("Type in keys for manual query");
            alert.setContentText("Provide comma seperated keys for query. Do " +
                    "not use quotes(\"\"). Avoid non-specific words " +
                    "or numbers. Synonyms are strongly recommended if " +
                    "auto-query is not working.");
            alert.showAndWait();
            return;
        }
        String[] keys = userInput.split(",");
        List<String> keysInList = new ArrayList<>();
        for (String key: keys) {
            if (key.length() > 0) {
                keysInList.add(key);
            }
        }

        String name = entry.getLongName();
        LoincCodeClass loincCodeClass = LoincLongNameParser.parse(name);
        List<HPO_Class_Found> queryResults = SparqlQuery.query_manual
                (keysInList, loincCodeClass);
        if (queryResults.size() != 0) {
            ObservableList<HPO_Class_Found> items = FXCollections.observableArrayList();
            for (HPO_Class_Found candidate: queryResults) {
                items.add(candidate);
            }
            this.hpoListView.setItems(items);
            userInputForManualQuery.clear();
            //items.add("0 result is found. Try manual search with synonyms.");
        } else {
            ObservableList<String> items = FXCollections.observableArrayList();
            items.add("0 HPO class is found. Try manual search with " +
                    "alternative keys (synonyms)");
            this.hpoListView.setItems(items);
        }
        //clear text in abnormality text fields if not currently editing a term
        if (!createAnnotationButton.getText().equals("Save")) {
            clearAbnormalityTextField();
            //inialize the flag field
            flagForAnnotation.setIndeterminate(false);
            flagForAnnotation.setSelected(false);
            createAnnotationSuccess.setFill(Color.WHITE);
            annotationNoteField.setText("");
        }
    }

    @FXML private void initLOINCtable(ActionEvent e) {
        logger.trace("init LOINC table");
        initTableStructure();
        String loincCoreTableFile=model.getPathToLoincCoreTableFile();
        if (loincCoreTableFile==null) {
            PopUps.showWarningDialog("Error", "File not found", "Could not find LOINC Core Table file. Set the path first");
            return;
        }
        this.loincmap = LoincEntry.getLoincEntryList(loincCoreTableFile);
        model.setLoincEntryMap(this.loincmap);
        int limit=Math.min(loincmap.size(),1000); // we will show just the first 1000 entries in the table.
        List<LoincEntry> lst = loincmap.values().asList().subList(0,limit);
        loincTableView.getItems().clear(); // remove any previous entries
        loincTableView.getItems().addAll(lst);
        loincTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        e.consume();
    }
/**
    @FXML private void initHPOmodelButton(ActionEvent e){

        //Remind user that this is a time consuming process
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Time Consuming step");
        alert.setHeaderText("Wait for the process to complete");
        alert.setContentText("This step will take about 2 minutes. It needs " +
                        "to be executed once for every session");
        alert.showAndWait();

        String pathToHPO = this.model.getPathToHpoOwlFile();
        logger.info("pathToHPO: " + pathToHPO);
        SparqlQuery.getOntologyModel(pathToHPO);

    }
**/
    @FXML private void initHPOmodelButton(ActionEvent e){

        String pathToHPO = this.model.getPathToHpoOwlFile();
        logger.info("pathToHPO: " + pathToHPO);
        //org.apache.jena.rdf.model.Model hpoModel = SparqlQuery.getOntologyModel(pathToHPO);
        //SparqlQuery.setHPOmodel(hpoModel);
        // The following codes run nicely from IDE, but fails in Jar.
        //create a task to create HPO model
        Task<org.apache.jena.rdf.model.Model> task = new OntologyModelBuilderForJena(pathToHPO);
        //Platform.runLater(new Thread(task)::start);
        new Thread(task).start();
        task.setOnSucceeded(x -> {
            SparqlQuery.setHPOmodel(task.getValue());
            IntializeHPOmodelbutton.setStyle("-fx-background-color: #00ff00");
            IntializeHPOmodelbutton.setText("HPO initialized");
        });
        task.setOnRunning(x -> {
            IntializeHPOmodelbutton.setStyle("-fx-background-color: #ffc0cb");
            IntializeHPOmodelbutton.setText("HPO initializing...");
        });
        task.setOnFailed(x -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Failed to create HPO model");
            alert.setContentText("Check whether hpo.owl is downloaded. Path to hpo.owl is set to: " + pathToHPO);
            IntializeHPOmodelbutton.setStyle("-fx-background-color: #ff0000");
            IntializeHPOmodelbutton.setText("Retry");
            alert.showAndWait();
        });

        e.consume();

    }


    @FXML private void search(ActionEvent e) {
        e.consume();
        String query = this.loincSearchTextField.getText().trim();
        if (query.isEmpty()) return;
        List<LoincEntry> entrylist=new ArrayList<>();
        try {
            LoincId loincId = new LoincId(query);
            if (this.loincmap.containsKey(loincId)) {
                entrylist.add(this.loincmap.get(loincId));
                logger.debug(this.loincmap.get(loincId).getLOINC_Number() + " : " + this.loincmap.get(loincId).getLongName());
            } else { //correct loinc code form but not valid
                throw new LoincCodeNotFoundException();
            }
        } catch (Exception msg) { //catch all kind of exception
            loincmap.values().stream()
                    .filter( loincEntry -> containedIn(query, loincEntry.getLongName()))
                    .forEach(loincEntry -> {
                        entrylist.add(loincEntry);
                        logger.debug(loincEntry.getLOINC_Number() + " : " + loincEntry.getLongName());
                    });
                    //.forEach(loincEntry -> entryListInOrder.add(loincEntry));
        }
        if (entrylist.isEmpty()) {
        //if (entryListInOrder.isEmpty()){
            logger.error(String.format("Could not identify LOINC entry for \"%s\"",query));
            PopUps.showWarningDialog("LOINC Search", "No hits found", String.format("Could not identify LOINC entry for \"%s\"",query));
            return;
        } else {
            logger.trace(String.format("Searching table for:  %s",query));
            logger.trace("# of loinc entries found: " + entrylist.size());
            //logger.trace("# of loinc entries found: " + entryListInOrder.size());
        }
        if (termmap==null) initialize(); // set up the Hpo autocomplete if possible
        loincTableView.getItems().clear();
        loincTableView.getItems().addAll(entrylist);
        accordion.setExpandedPane(loincTableTitledpane);
    }

    private boolean containedIn(String query, String text) {
        String [] keys = query.split("\\W");
        for (String key : keys) {
            if (!text.toLowerCase().contains(key.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    @FXML private void handleLoincFiltering(ActionEvent e){

        /**
        int malformedLoincCount = 0;
        List<String> notFound = new ArrayList<>();
        List<LoincEntry> entryOfInterest = new ArrayList<>();
        if (f != null) {
            String path = f.getAbsolutePath();
            try {
                HashSet<String> loincOfInterest = new LoincOfInterest(path).getLoincOfInterest();
                for (String loinc : loincOfInterest) {
                    LoincId loincId = new LoincId(loinc);
                    if (model.getLoincEntryMap().containsKey(loincId)) {
                        entryOfInterest.add(model.getLoincEntryMap().get(loinc));
                    } else {
                        notFound.add(loinc);
                    }
                }
                loincTableView.getItems().clear();
                loincTableView.getItems().addAll(entryOfInterest);
                loincTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

            } catch (FileNotFoundException excpt) {
                logger.error("unable to find the file for loinc of interest");
            } catch (MalformedLoincCodeException exception) {
                malformedLoincCount++;
            }


        } else {
            logger.error("Unable to obtain path to LOINC of interest file");
            return;
        }
        e.consume();
**/

        List<LoincEntry> entrylist=new ArrayList<>();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose File containing a list of interested Loinc " +
                "codes");
        File f = chooser.showOpenDialog(null);
        List<String> notFound = new ArrayList<>();
        int malformedLoincCount = 0;
        if (f != null) {
            String path = f.getAbsolutePath();
            try {
                Set<String> loincOfInterest = new LoincOfInterest(path).getLoincOfInterest();
                //loincOfInterest.stream().forEach(System.out::print);
                for (String loincString : loincOfInterest) {
                    LoincId loincId = null;
                    LoincEntry loincEntry = null;
                    try {
                        loincId = new LoincId(loincString);
                        loincEntry = model.getLoincEntryMap().get(loincId);
                    } catch (MalformedLoincCodeException e2) {
                        //try to see whether user provided Loinc long common name
                        if (model.getLoincEntryMapWithName().get(loincString) != null) {
                            loincEntry = model.getLoincEntryMapWithName().get(loincString);
                        } else {
                            logger.error("Malformed loinc");
                            malformedLoincCount++;
                        }
                    }
                    if (loincEntry != null) {
                        entrylist.add(loincEntry);
                    } else {
                        notFound.add(loincString);
                    }
                }
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }

            if (malformedLoincCount > 0 || !notFound.isEmpty()) {
                PopUps.showInfoMessage(String.format("# malformed Loinc codes: %d\n# Loinc codes not found: %d",
                        malformedLoincCount, notFound.size()), "Incomplete import of Loinc codes");
            }
            if (entrylist.isEmpty()) {
                logger.error(String.format("Found 0 Loinc codes"));
                PopUps.showWarningDialog("LOINC filtering", "No hits found", "Could not find any loinc codes");
                return;
            } else {
                logger.trace("Loinc filtering result: ");
                logger.trace("# of loinc entries found: " + entrylist.size());
            }

            if (termmap==null) initialize(); // set up the Hpo autocomplete if possible
            loincTableView.getItems().clear();
            loincTableView.getItems().addAll(entrylist);
            entrylist.forEach(p -> logger.trace(p.getLOINC_Number()));
            //loincTableView.sort((p, q) -> entrylist.indexOf(p) - entrylist.indexOf(q));
            accordion.setExpandedPane(loincTableTitledpane);
        } else {
            logger.error("Unable to obtain path to LOINC of interest file");
            return;
        }
    }




    /**
     * private class for showing HPO class in treeview.
     * Another reason to have this is to facilitate drag and draw from treeview.
     */
    private class HPO_TreeView{
        private HPO_Class_Found hpo_class_found;
        private HPO_TreeView() {
            this.hpo_class_found = null;
        }
        private HPO_TreeView(HPO_Class_Found hpo_class_found) {
            this.hpo_class_found = hpo_class_found;
        }

        public HPO_Class_Found getHpo_class_found() {
             return this.hpo_class_found;
        }

        @Override
        public String toString() {
            if (this.hpo_class_found == null) {
                return "root";
            }
            String stringRepretation = "";
            String[] id_words = this.hpo_class_found.getId().split("/");
            stringRepretation += id_words[id_words.length - 1];
            stringRepretation += "\n";
            stringRepretation += this.hpo_class_found.getLabel();
            return stringRepretation;
        }
    }

    @FXML private void handleCandidateHPODoubleClick(MouseEvent e){

        if (e.getClickCount() == 2 && hpoListView.getSelectionModel()
                .getSelectedItem() != null && hpoListView.getSelectionModel()
                .getSelectedItem() instanceof HPO_Class_Found) {
            HPO_Class_Found hpo_class_found = (HPO_Class_Found) hpoListView
                    .getSelectionModel().getSelectedItem();
            List<HPO_Class_Found> parents = SparqlQuery.getParents
                    (hpo_class_found.getId());
            List<HPO_Class_Found> children = SparqlQuery.getChildren
                    (hpo_class_found.getId());

            TreeItem<HPO_TreeView> rootItem = new TreeItem<>(new HPO_TreeView());
            rootItem.setExpanded(true);

            if (parents.size() > 0) {
                for (HPO_Class_Found parent : parents) {
                    TreeItem<HPO_TreeView> parentItem = new TreeItem<>(new
                            HPO_TreeView(parent));
                    rootItem.getChildren().add(parentItem);
                    TreeItem<HPO_TreeView> current = new TreeItem<>
                            (new HPO_TreeView(hpo_class_found));
                    parentItem.getChildren().add(current);
                    parentItem.setExpanded(true);
                    current.setExpanded(true);
                    if (children.size() > 0) {
                        for (HPO_Class_Found child : children) {
                            TreeItem<HPO_TreeView> childItem = new TreeItem<>
                                    (new HPO_TreeView(child));
                            current.getChildren().add(childItem);
                        }
                    }
                }
            }
            this.treeView.setRoot(rootItem);
        }
        e.consume();
    }

    @FXML private void handleCandidateHPODragged(MouseEvent e) {

        System.out.println("Drag event detected");
        Dragboard db = hpoListView.startDragAndDrop(TransferMode.ANY);
        ClipboardContent content = new ClipboardContent();
        Object selectedCell = hpoListView.getSelectionModel().getSelectedItem();
        if (selectedCell instanceof HPO_Class_Found) {
            content.putString(((HPO_Class_Found) selectedCell).getLabel());
            db.setContent(content);
        } else {
            logger.info("Dragging something that is not a HPO term");
        }

        e.consume();
    }

    @FXML private void handleHPOLowAbnormality(DragEvent e){

        if (e.getDragboard().hasString()) {
            annotationTextFieldLeft.setText(e.getDragboard().getString());
        }
        annotationTextFieldLeft.setStyle("-fx-background-color: WHITE;");
        e.consume();
    }

    @FXML private void handleHPOHighAbnormality(DragEvent e){

        if (e.getDragboard().hasString()) {
            annotationTextFieldRight.setText(e.getDragboard().getString());
        }
        annotationTextFieldRight.setStyle("-fx-background-color: WHITE;");
        e.consume();

    }

    @FXML private void handleParentAbnormality(DragEvent e){
        if (e.getDragboard().hasString()) {
            annotationTextFieldMiddle.setText(e.getDragboard().getString());
        }
        annotationTextFieldMiddle.setStyle("-fx-background-color: WHITE;");
        e.consume();
    }



    /**
     * Record the terms for basic annotation
     */
    private Map<String, String> recordTempTerms(){
        Map<String, String> temp = new HashMap<>();
        String hpoLo = annotationTextFieldLeft.getText();
        if (hpoLo!= null && !hpoLo.trim().isEmpty())
            hpoLo = stripEN(hpoLo.trim());
        String hpoNormal = annotationTextFieldMiddle.getText();
        if (hpoNormal != null && !hpoNormal.trim().isEmpty())
            hpoNormal = stripEN(hpoNormal.trim());
        String hpoHi= annotationTextFieldRight.getText();
        if (hpoHi != null && !hpoHi.isEmpty()) hpoHi = stripEN(hpoHi.trim());

        if(hpoLo != null && !hpoLo.isEmpty()) temp.put("hpoLo", hpoLo);
        if(hpoNormal != null && !hpoNormal.isEmpty()) temp.put("hpoNormal", hpoNormal);
        if(hpoHi != null && !hpoHi.isEmpty()) temp.put("hpoHi", hpoHi);
        return temp;
    }

    private Map<String, String> recordAdvancedAnnotation(){
        Map<String, String> temp = new HashMap<>();
        String system = annotationTextFieldLeft.getText();
        if (system!= null && !system.trim().isEmpty())
            temp.put("system", system);
        String code = annotationTextFieldMiddle.getText();
        if (code != null && !code.trim().isEmpty())
            temp.put("code", code);
        String hpoTerm= annotationTextFieldRight.getText();
        if (hpoTerm != null && !hpoTerm.isEmpty()) {
            temp.put("hpoTerm", hpoTerm);
        }
        return temp;
    }

    private boolean recordInversed() {
        return inverseChecker.isSelected();
    }


    @FXML private void createLoinc2HpoAnnotation(ActionEvent e) {


        if (loincTableView.getSelectionModel().getSelectedItem() == null) {
            PopUps.showInfoMessage("No loinc entry is selected. Try clicking \"Initialize Loinc Table\"", "No Loinc selection Error");
            return;
        }
        LoincId loincCode = loincTableView.getSelectionModel().getSelectedItem().getLOINC_Number();
        LoincScale loincScale = LoincScale.string2enum(loincTableView.getSelectionModel().getSelectedItem().getScale());

        if (createAnnotationButton.getText().equals("Create annotation") && model.getLoincAnnotationMap().containsKey(loincCode)) {
            boolean toOverwrite = PopUps.getBooleanFromUser("Do you want to overwrite?",
                    loincCode + " is already annotated", "Overwrite warning");
            if (!toOverwrite) return;
        }

        logger.debug("advancedAnnotationModeSelected: " + advancedAnnotationModeSelected);
        if(!advancedAnnotationModeSelected) { //we are last in basic mode, user might have changed data for basic annotation
            logger.debug("advancedAnnotationModeSelected: " + advancedAnnotationModeSelected + " recording");
            model.setTempTerms(recordTempTerms()); //update terms for basic annotation
            model.setInversedBasicMode(recordInversed());
            logger.debug("annotationTextFieldLeft is recorded: " + annotationTextFieldLeft.getText());
            logger.debug("annotationTextFieldMiddle is recorded: " + annotationTextFieldMiddle.getText());
            logger.debug("annotationTextFieldRight is recorded: " + annotationTextFieldRight.getText());
        } else { //if we are last in the advanced mode, user might have added a new annotation, we add this annotation
            handleAnnotateCodedValue(e);
        }

        //tempTerms.values().stream().forEach(System.out::println);
        //if this function is called at advanced annotation mode, the terms for basic annotation was already saved
        Map<String, String> tempTerms = model.getTempTerms();
        String hpoLo = tempTerms.get("hpoLo");
        String hpoNormal = tempTerms.get("hpoNormal");
        String hpoHi = tempTerms.get("hpoHi");

        if ((hpoLo == null || hpoLo.isEmpty()) &&
                (hpoNormal == null || hpoNormal.isEmpty()) &&
                (hpoHi == null || hpoHi.isEmpty()) &&
                tempAdvancedAnnotations.isEmpty()) {
            return;
        }

        //We don't have to force every loinc code to have three phenotypes
        HpoTerm low = termmap.get(hpoLo);
        HpoTerm normal = termmap.get(hpoNormal);
        HpoTerm high = termmap.get(hpoHi);

        //Warning user that there is something wrong
        //it happens when something is wrong with hpo termmap (a name could not be mapped)
        if (hpoLo != null && low==null) {
            logger.error(hpoLo + " cannot be mapped to a term");
            createAnnotationSuccess.setFill(Color.RED);
            showErrorOfMapping(hpoLo);
            return;
        }

        if (hpoHi != null && high==null) {
            logger.error(hpoHi + " cannot be mapped to a term");
            createAnnotationSuccess.setFill(Color.RED);
            showErrorOfMapping(hpoHi);
            return;
        }

        if (hpoNormal !=null && normal==null) {
            logger.error(hpoNormal + " cannot be mapped to a term");
            createAnnotationSuccess.setFill(Color.RED);
            showErrorOfMapping(hpoNormal);
            return;
        }



        UniversalLoinc2HPOAnnotation loinc2HPOAnnotation = new UniversalLoinc2HPOAnnotation(loincCode, loincScale);

        Map<String, Boolean> qcresult = qcAnnotation(hpoLo, hpoNormal, hpoHi);
        if (qcresult.get("issueDetected") && !qcresult.get("userconfirmed")) {
            createAnnotationSuccess.setFill(Color.RED);
            return;
        } else {
            //String note = annotationNoteField.getText().isEmpty()? "\"\"":annotationNoteField.getText();
            try {
                Map<String, Code> internalCode = CodeSystemConvertor.getCodeContainer().getCodeSystemMap().get(Loinc2HPOCodedValue.CODESYSTEM);
                if (hpoLo != null && low != null) {
                    loinc2HPOAnnotation.addAnnotation(internalCode.get("L"), new HpoTermId4LoincTest(low, false));
                }
                if (hpoNormal != null && normal != null) {
                    loinc2HPOAnnotation
                            .addAnnotation(internalCode.get("A"), new HpoTermId4LoincTest(normal, false));
                }
                if (hpoNormal != null && normal != null && model.isInversedBasicMode()) {
                    loinc2HPOAnnotation
                            .addAnnotation(internalCode.get("N"),  new HpoTermId4LoincTest(normal, true))
                            .addAnnotation(internalCode.get("NP"), new HpoTermId4LoincTest(normal, true));

                }
                if (hpoHi != null && high != null)
                    loinc2HPOAnnotation
                            .addAnnotation(internalCode.get("H"),  new HpoTermId4LoincTest(high, false))
                            .addAnnotation(internalCode.get("P"),  new HpoTermId4LoincTest(high, false));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


        if (loinc2HPOAnnotation != null && !tempAdvancedAnnotations.isEmpty()) {
            for (Annotation annotation : tempAdvancedAnnotations) {
                loinc2HPOAnnotation.addAnnotation(annotation.getCode(), annotation.getHpoTermId4LoincTest());
            }
        }

        loinc2HPOAnnotation.setFlag(flagForAnnotation.isSelected());
        loinc2HPOAnnotation.setNote(annotationNoteField.getText());

        if (loinc2HPOAnnotation != null) {
            logger.info(loinc2HPOAnnotation.getCodes().size() + " annotations");
            this.model.addLoincTest(loinc2HPOAnnotation);
            advancedAnnotationModeSelected = false;
            model.setTempTerms(new HashMap<>());//clear the temp term in model
            model.setInversedBasicMode(false);
            model.setTempAdvancedAnnotation(new HashMap<>());
            model.setInversedAdvancedMode(false);
            tempAdvancedAnnotations.clear();
            switchToBasicAnnotationMode();
            flagForAnnotation.setSelected(false);
            annotationNoteField.clear();

            loinc2HpoAnnotationsTabController.refreshTable();
            createAnnotationSuccess.setFill(Color.GREEN);
            if (createAnnotationButton.getText().equals("Save")) {
                createAnnotationButton.setText("Create annotation");
                model.setLoincUnderEditing(null);
            }
            changeColorLoincTableView();
        }
        //showSuccessOfMapping("Go to next loinc code!");

        e.consume();
    }

    /**
    protected UniversalLoinc2HPOAnnotation createCurrentAnnotation() {
        logger.trace("enter createCurrentAnnotation() ");
        if(!advancedAnnotationModeSelected) {
            model.setTempTerms(recordTempTerms()); //update terms for basic annotation
            model.setTempInversed(recordInversed());
        }
        //if this function is called at advanced annotation mode, the terms for basic annotation was already saved
        Map<String, String> tempTerms = model.getTempTerms();
        String hpoLo = tempTerms.get("hpoLo");
        String hpoNormal = tempTerms.get("hpoNormal");
        String hpoHi = tempTerms.get("hpoHi");

        //We don't have to force every loinc code to have three phenotypes
        HpoTerm low = termmap.get(hpoLo);
        HpoTerm normal = termmap.get(hpoNormal);
        HpoTerm high = termmap.get(hpoHi);

        //Warning user that there is something wrong
        //it happens when something is wrong with hpo termmap (a name could not be mapped)
        if (hpoLo != null && low==null) {
            logger.error(hpoLo + " cannot be mapped to a term");
            createAnnotationSuccess.setFill(Color.RED);
            showErrorOfMapping(hpoLo);
            return null;
        }

        if (hpoHi != null && high==null) {
            logger.error(hpoHi + " cannot be mapped to a term");
            createAnnotationSuccess.setFill(Color.RED);
            showErrorOfMapping(hpoHi);
            return null;
        }

        if (hpoNormal !=null && normal==null) {
            logger.error(hpoNormal + " cannot be mapped to a term");
            createAnnotationSuccess.setFill(Color.RED);
            showErrorOfMapping(hpoNormal);
            return null;
        }

        LoincId loincCode = loincTableView.getSelectionModel().getSelectedItem().getLOINC_Number();
        LoincScale loincScale = LoincScale.string2enum(loincTableView.getSelectionModel().getSelectedItem().getScale());

        UniversalLoinc2HPOAnnotation loinc2HPOAnnotation = new UniversalLoinc2HPOAnnotation(loincCode, loincScale);

        Map<String, Boolean> qcresult = qcAnnotation(hpoLo, hpoNormal, hpoHi);
        if (qcresult.get("issueDetected") && !qcresult.get("userconfirmed")) {
            createAnnotationSuccess.setFill(Color.RED);
            return null;
        } else {
            //String note = annotationNoteField.getText().isEmpty()? "\"\"":annotationNoteField.getText();
            try {
                Map<String, Code> internalCode = CodeSystemConvertor.getCodeContainer().getCodeSystemMap().get(Loinc2HPOCodedValue.CODESYSTEM);
                if (hpoLo != null && low != null) {
                    loinc2HPOAnnotation.addAnnotation(internalCode.get("L"), new HpoTermId4LoincTest(low, false));
                }
                if (hpoNormal != null && normal != null) {
                    loinc2HPOAnnotation
                            .addAnnotation(internalCode.get("A"), new HpoTermId4LoincTest(normal, false));
                }
                if (hpoNormal != null && normal != null && inverseChecker.isSelected()) {
                    loinc2HPOAnnotation
                            .addAnnotation(internalCode.get("N"),  new HpoTermId4LoincTest(normal, true))
                            .addAnnotation(internalCode.get("NP"), new HpoTermId4LoincTest(normal, true));

                }
                if (hpoHi != null && high != null)
                    loinc2HPOAnnotation
                            .addAnnotation(internalCode.get("H"),  new HpoTermId4LoincTest(high, false))
                            .addAnnotation(internalCode.get("P"),  new HpoTermId4LoincTest(high, false));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (!tempAdvancedAnnotations.isEmpty()) {
            for (Annotation annotation : tempAdvancedAnnotations) {
                loinc2HPOAnnotation.addAnnotation(annotation.getCode(), annotation.getHpoTermId4LoincTest());
            }
        }

        //this.model.setCurrentAnnotation(loinc2HPOAnnotation);
        logger.trace("exit createCurrentAnnotation() for: " + loinc2HPOAnnotation.getLoincId() + " with success.");
        return loinc2HPOAnnotation;
    }
**/


    /**
     * Do a qc of annotation, and ask user questions if there are potential issues
     * @param HpoLow
     * @param HpoNorm
     * @param HpoHigh
     * @return
     */
    private Map<String, Boolean> qcAnnotation(String HpoLow, String HpoNorm, String HpoHigh){

        boolean issueDetected = false;
        boolean userConfirmed = false;

        if ((HpoLow == null || HpoLow.trim().isEmpty()) &&
                (HpoNorm == null || HpoNorm.trim().isEmpty()) &&
                (HpoHigh == null || HpoHigh.trim().isEmpty())) {
            //popup an alert
            issueDetected = true;
            userConfirmed = PopUps.getBooleanFromUser("Are you sure you want to create an annotation without any HPO terms?",
                    "Annotation without HPO terms", "No HPO Alert");
        }

        if (HpoLow != null && HpoNorm != null && !HpoLow.trim().isEmpty() && stringEquals(HpoLow, HpoNorm)) {
            //alert: low and norm are same!
            issueDetected = true;
            userConfirmed = PopUps.getBooleanFromUser("Are you sure low and parent are the same HPO term?",
                    "Same HPO term for low and parent", "Duplicate HPO alert");
        }

        if (HpoLow != null && HpoHigh != null && !HpoLow.trim().isEmpty() && stringEquals(HpoLow, HpoHigh)) {
            //alert: low and high are same!
            issueDetected = true;
            userConfirmed = PopUps.getBooleanFromUser("Are you sure low and high are the same HPO term?",
                    "Same HPO term for low and high", "Duplicate HPO alert");
        }

        if (HpoNorm != null && HpoHigh != null && !HpoNorm.trim().isEmpty() && stringEquals(HpoNorm, HpoHigh)) {
            //alert: norm and high are the same!
            issueDetected = true;
            userConfirmed = PopUps.getBooleanFromUser("Are you sure parent and high are the same HPO term?",
                    "Same HPO term for parent and high", "Duplicate HPO alert");
        }
        HashMap<String, Boolean> results = new HashMap<>();
        results.put("issueDetected", issueDetected);
        results.put("userconfirmed", userConfirmed);
        return results;

    }

    private String stripEN(String hpoTerm) {
        if (hpoTerm.trim().toLowerCase().endsWith("@en")) {
            return hpoTerm.trim().substring(0, hpoTerm.length() - 3);
        } else {
            return hpoTerm.trim();
        }
    }

    /**
     * Determine whether two strings are identical (case insensitive, no space before and after string)
     * @param x
     * @param y
     * @return
     */
    private boolean stringEquals(String x, String y) {
        return x.trim().toLowerCase().equals(y.trim().toLowerCase());
    }

    
    private void showErrorOfMapping(String message) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Failure");
        errorAlert.setContentText(message + "could not be mapped. There is nothing to do from user. Contact developer.");
        errorAlert.showAndWait();
    }

    private void showSuccessOfMapping(String message) {
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("Success");
        infoAlert.setContentText(message);
        infoAlert.showAndWait();
        long currentTime = System.currentTimeMillis();
        long delay = currentTime + 1000;
        while (currentTime < delay) {
            currentTime = System.currentTimeMillis();
        }
        infoAlert.close();
    }

    @FXML
    private void handleDragOver(DragEvent e){
        Dragboard db = e.getDragboard();
        if (db.hasString()) {
            e.acceptTransferModes(TransferMode.MOVE);
        }

        logger.info("Drag over. Nothing specific todo");
        e.consume();

    }

    @FXML
    private void handleDragDone(DragEvent e) {

        logger.info("Drag done. Nothing specific todo");

    }

    @FXML
    private void handleDragInTreeView(MouseEvent e) {
        System.out.println("Drag event detected");
        Dragboard db = treeView.startDragAndDrop(TransferMode.ANY);
        ClipboardContent content = new ClipboardContent();
        Object selectedItem = treeView.getSelectionModel().getSelectedItem().getValue();
        if (selectedItem instanceof HPO_TreeView) {
            content.putString(((HPO_TreeView) selectedItem)
                    .getHpo_class_found().getLabel());
            db.setContent(content);
        } else {
            logger.info("Dragging something that is not a HPO term");
        }
        e.consume();
    }

    @FXML
    void handleDragEnterHighAbnorm(DragEvent event) {

        annotationTextFieldRight.setStyle("-fx-background-color: LIGHTBLUE;");
        event.consume();

    }

    @FXML
    void handleDragEnterLowAbnorm(DragEvent event) {
        annotationTextFieldLeft.setStyle("-fx-background-color: LIGHTBLUE;");
        event.consume();

    }

    @FXML
    void handleDragEnterParentAbnorm(DragEvent event) {
        annotationTextFieldMiddle.setStyle("-fx-background-color: LIGHTBLUE;");
        event.consume();

    }

    @FXML
    void handleDragExitHighAbnorm(DragEvent event) {

        annotationTextFieldRight.setStyle("-fx-background-color: WHITE;");
        event.consume();

    }

    @FXML
    void handleDragExitLowAbnorm(DragEvent event) {
        annotationTextFieldLeft.setStyle("-fx-background-color: WHITE;");
        event.consume();
    }

    @FXML
    void handleDragExitParentAbnorm(DragEvent event) {
        annotationTextFieldMiddle.setStyle("-fx-background-color: WHITE;");
        event.consume();
    }
    @FXML
    void handleFlagForAnnotation(ActionEvent event) {

    }

    //change the color of rows to green after the loinc code has been annotated
    protected void changeColorLoincTableView(){
/**
        loincIdTableColumn.setCellFactory(x -> new TableCell<LoincEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty){
                super.updateItem(item, empty);
                if(item != null && !empty) {
                    setText(item);
                    if(model.getLoincAnnotationMap().containsKey(item)) {
                        logger.info("model contains " + item);
                        logger.info("num of items in model " + model.getLoincAnnotationMap().size());
                        TableRow<LoincEntry> currentRow = getTableRow();
                        currentRow.setStyle("-fx-background-color: lightblue");
                        //setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-background-color: lightblue");
                    }
                }
            }

        });
 **/
    }

/**
 * The program never go to setRowFactory. WHy?
    //change the color of rows to green after the loinc code has been annotated
    protected void changeColorLoincTableView(){
        logger.debug("enter changeColorLoincTableView");
        logger.info("model size: " + model.getLoincAnnotationMap().size());

        loincTableView.setRowFactory(x -> new TableRow<LoincEntry>() {
            @Override
            protected void updateItem(LoincEntry item, boolean empty){
                super.updateItem(item, empty);
                logger.info("row loinc num: " + item.getLOINC_Number());

                //if(item != null && !empty && model.getLoincAnnotationMap().containsKey(item.getLOINC_Number())) {
                if(item != null && !empty) {
                        logger.info("model contains " + item);
                        logger.info("num of items in model " + model.getLoincAnnotationMap().size());
                        //TableRow<LoincEntry> currentRow = getTableRow();
                        setStyle("-fx-background-color: lightblue");

                }
            }

        });
        logger.debug("exit changeColorLoincTableView");
    }
**/





    @FXML
    private void annotationModeSwitchButton(ActionEvent e) {
        e.consume();
        //createTempAnnotation();
        //Important: Save annotation current annotation data
        if (!advancedAnnotationModeSelected) { //current state: Basic mode
            model.setTempTerms(recordTempTerms());
            model.setInversedBasicMode(recordInversed());
        }
        if (advancedAnnotationModeSelected) { //current state: Advanced mode
            model.setTempAdvancedAnnotation(recordAdvancedAnnotation());
            model.setInversedAdvancedMode(recordInversed());
        }

        advancedAnnotationModeSelected = ! advancedAnnotationModeSelected; //switch mode
        if (advancedAnnotationModeSelected) {
            switchToAdvancedAnnotationMode(); //change display for advanced mode
        } else {
            switchToBasicAnnotationMode(); //change display for basic mode
        }

    }

    private void switchToAdvancedAnnotationMode(){
        //before switching to advanced mode, save any data in the basic mode

        annotationLeftLabel.setText("system");
        annotationMiddleLabel.setText("code");
        annotationRightLabel.setText("hpo term");
        annotationTextFieldLeft.clear();
        annotationTextFieldMiddle.clear();
        annotationTextFieldRight.clear();
        annotationTextFieldLeft.setPromptText("code system");
        annotationTextFieldMiddle.setPromptText("code");
        annotationTextFieldRight.setPromptText("candidate HPO");
        modeButton.setText("<<<basic");
        inverseChecker.setSelected(false);
        if (!model.getTempAdvancedAnnotation().isEmpty()) { //if we have recorded temp data, display it accordingly
            annotationTextFieldLeft.setText(model.getTempAdvancedAnnotation().get("system"));
            annotationTextFieldMiddle.setText(model.getTempAdvancedAnnotation().get("code"));
            annotationTextFieldRight.setText(model.getTempAdvancedAnnotation().get("hpoTerm"));
            inverseChecker.setSelected(model.isInversedAdvancedMode());
        }
    }

    private void switchToBasicAnnotationMode(){
        annotationLeftLabel.setText("<Low threshold");
        annotationMiddleLabel.setText("intermediate");
        annotationRightLabel.setText(">High threshold");
        annotationTextFieldLeft.clear();
        annotationTextFieldMiddle.clear();
        annotationTextFieldRight.clear();
        annotationTextFieldLeft.setPromptText("hpo for low value");
        annotationTextFieldMiddle.setPromptText("hpo for mid value");
        annotationTextFieldRight.setPromptText("hpo for high value");
        modeButton.setText("advanced>>>");
        inverseChecker.setSelected(true);
        if (!model.getTempTerms().isEmpty()) { //if we have recorded temp data, display it accordingly
            annotationTextFieldLeft.setText(model.getTempTerms().get("hpoLo"));
            annotationTextFieldMiddle.setText(model.getTempTerms().get("hpoNormal"));
            annotationTextFieldRight.setText(model.getTempTerms().get("hpoHi"));
            inverseChecker.setSelected(model.isInversedBasicMode());
        }
    }

    @FXML
    private void handleAnnotateCodedValue(ActionEvent e){
        e.consume();

        if (!advancedAnnotationModeSelected) return; //do nothing if it is the basic mode

        Annotation annotation = null;
        String system = annotationTextFieldLeft.getText().trim().toLowerCase();
        String codeId = annotationTextFieldMiddle.getText().trim(); //case sensitive
        Code code = null;
        if (system != null && !system.isEmpty() && codeId != null && !codeId.isEmpty()) {
            code = Code.getNewCode().setSystem(system).setCode(codeId);
        }
        String candidateHPO = annotationTextFieldRight.getText();
        HpoTerm hpoterm = model.getTermMap().get(stripEN(candidateHPO));
        if (hpoterm == null) logger.error("hpoterm is null");
        if (code != null && hpoterm != null) {
            annotation = new Annotation(code, new HpoTermId4LoincTest(hpoterm, inverseChecker.isSelected()));
        }
        tempAdvancedAnnotations.add(annotation);
        //add annotated value to the advanced table view
        initadvancedAnnotationTable();
        accordion.setExpandedPane(advancedAnnotationTitledPane);
        inverseChecker.setSelected(false);
        model.setTempAdvancedAnnotation(new HashMap<>());
        model.setInversedAdvancedMode(false);
    }

    @FXML
    private void handleDeleteCodedAnnotation(ActionEvent event) {
        logger.debug("user wants to delete an annotation");
        Annotation selectedToDelete = advancedAnnotationTable.getSelectionModel().getSelectedItem();
        if (selectedToDelete != null) {
            tempAdvancedAnnotations.remove(selectedToDelete);
        }
        event.consume();
    }


    private void initadvancedAnnotationTable(){

        advancedAnnotationSystem.setSortable(true);
        advancedAnnotationSystem.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getCode().getSystem()));
        advancedAnnotationCode.setSortable(true);
        advancedAnnotationCode.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getCode().getCode()));
        advancedAnnotationHpo.setSortable(true);
        advancedAnnotationHpo.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getHpo_term()));

        advancedAnnotationTable.setItems(tempAdvancedAnnotations);
    }




    private String githubUsername;
    private String githubPassword;
    LoincId loincIdSelected=null;
    /**
     * For the GitHub new issues, we want to allow the user to choose a pre-existing label for the issue.
     * For this, we first go to GitHub and retrieve the labels with
     * {@link org.monarchinitiative.loinc2hpo.github.GitHubLabelRetriever}. We only do this
     * once per session though.
     */
    private void initializeGitHubLabelsIfNecessary() {
        if (model.hasLabels()) {
            return; // we only need to retrieve the labels from the server once per session!
        }
        GitHubLabelRetriever retriever = new GitHubLabelRetriever();
        List<String> labels = retriever.getLabels();
        if (labels == null) {
            labels = new ArrayList<>();
        }
        if (labels.size() == 0) {
            labels.add("new term request");
        }
        model.setGithublabels(labels);
    }


    @FXML
    private void suggestNewTerm(ActionEvent e) {
        e.consume();
        initializeGitHubLabelsIfNecessary();
        LoincEntry loincEntrySelected = loincTableView.getSelectionModel().getSelectedItem();
        if (loincEntrySelected == null) {

            logger.error("Select a loinc code before making a suggestion");
            PopUps.showInfoMessage("Please select a loinc code before creating GitHub issue",
                    "Error: No HPO Term selected");
            return;
        }
        loincIdSelected = loincEntrySelected.getLOINC_Number();
        logger.info("Selected loinc to create github issue for: " + loincIdSelected);

        GitHubPopup popup = new GitHubPopup(loincEntrySelected);
        initializeGitHubLabelsIfNecessary();
        popup.setLabels(model.getGithublabels());
        popup.setupGithubUsernamePassword(githubUsername, githubPassword);
        popup.setBiocuratorId(model.getBiocuratorID());
        logger.debug("get biocurator id from model: " + model.getBiocuratorID());
        popup.displayWindow(Main.getPrimarystage());
        String githubissue = popup.retrieveGitHubIssue();
        if (githubissue == null) {
            logger.trace("got back null github issue");
            return;
        }
        String title = String.format("Suggesting new term for Loinc:  \"%s\"", loincIdSelected);
        postGitHubIssue(githubissue, title, popup.getGitHubUserName(), popup.getGitHubPassWord());
    }

    @FXML
    private void suggestNewChildTerm(ActionEvent e) {
        e.consume();
        initializeGitHubLabelsIfNecessary();
        LoincEntry loincEntrySelected = loincTableView.getSelectionModel().getSelectedItem();
        if (loincEntrySelected == null) {

            logger.error("Select a loinc code before making a suggestion");
            PopUps.showInfoMessage("Please select a loinc code before creating GitHub issue",
                    "Error: No HPO Term selected");
            return;
        }
        loincIdSelected = loincEntrySelected.getLOINC_Number();
        logger.info("Selected loinc to create github issue for: " + loincIdSelected);

        HPO_Class_Found hpoSelected = (HPO_Class_Found) hpoListView.getSelectionModel().getSelectedItem();
        if (hpoSelected == null) {
            HPO_TreeView hpoSelectedInTree = treeView.getSelectionModel().getSelectedItem().getValue();
            hpoSelected = hpoSelectedInTree.hpo_class_found;
        }
        if (hpoSelected == null) {
            logger.error("Select a hpo term before making a suggestion");
            PopUps.showInfoMessage("Please select a hpo term before creating GitHub issue",
                    "Error: No HPO Term selected");
            return;
        }

        HpoTerm hpoTerm = model.getTermMap().get(hpoSelected.getLabel());

        GitHubPopup popup = new GitHubPopup(loincEntrySelected, hpoTerm, true);
        initializeGitHubLabelsIfNecessary();
        popup.setLabels(model.getGithublabels());
        popup.setupGithubUsernamePassword(githubUsername, githubPassword);
        popup.setBiocuratorId(model.getBiocuratorID());
        logger.debug("get biocurator id from model: " + model.getBiocuratorID());
        popup.displayWindow(Main.getPrimarystage());
        String githubissue = popup.retrieveGitHubIssue();
        if (githubissue == null) {
            logger.trace("got back null github issue");
            return;
        }
        String title = String.format("Suggesting new term for Loinc:  \"%s\"", loincIdSelected);
        postGitHubIssue(githubissue, title, popup.getGitHubUserName(), popup.getGitHubPassWord());
    }


    private void postGitHubIssue(String message, String title, String uname, String pword) {
        GitHubPoster poster = new GitHubPoster(uname, pword, title, message);
        this.githubUsername = uname;
        this.githubPassword = pword;
        try {
            poster.postIssue();
        } catch (NetPostException he) {
            PopUps.showException("GitHub error", "Bad Request (400): Could not post issue", he);
        } catch (Exception ex) {
            PopUps.showException("GitHub error", "GitHub error: Could not post issue", ex);
            return;
        }
        String response = poster.getHttpResponse();
        PopUps.showInfoMessage(
                String.format("Created issue for %s\nServer response: %s", loincIdSelected.toString(), response), "Created new issue");

    }

    @FXML
    private void getContextMenu4TreeView(ContextMenuEvent event) {
        event.consume();
        treeView.setContextMenu(contextMenu);
    }


    protected LoincEntry getLoincIdSelected() {
        return loincTableView.getSelectionModel().getSelectedItem();
    }

    protected void setLoincIdSelected(LoincEntry loincEntry) {
        loincTableView.getSelectionModel().select(loincEntry);
    }
    protected void setLoincIdSelected(LoincId loincId) {
        LoincEntry loincEntry = model.getLoincEntryMap().get(loincId);
        loincTableView.getSelectionModel().select(loincEntry);
    }

    @FXML
    protected void showAllAnnotations(ActionEvent event) {
        event.consume();

        LoincEntry loincEntry2Review = getLoincIdSelected();
        if (loincEntry2Review == null) {
            PopUps.showInfoMessage("There is no annotation to review. Select a loinc entry and try again",
                    "No content to show");
            return;
        }
        if (model.getLoincAnnotationMap().get(loincEntry2Review.getLOINC_Number()) != null) {
            logger.debug("The annotation to review is already added to the annotation map");
            //currentAnnotationController.setCurrentAnnotation(model.getLoincAnnotationMap().get(loincEntry2Review.getLOINC_Number()));
            model.setCurrentAnnotation(model.getLoincAnnotationMap().get(loincEntry2Review.getLOINC_Number()));
        } else {
            logger.debug("currently selected loinc has no annotation. A temporary annotation is being created for " + loincEntry2Review.getLOINC_Number());
            PopUps.showInfoMessage("Currently selected loinc code has not been annotated.",
                    "No content to show");
            return;
            //currentAnnotationController.setCurrentAnnotation(createCurrentAnnotation());
            //model.setCurrentAnnotation(createCurrentAnnotation());
        }


        Stage window = new Stage();
        window.setResizable(true);
        window.centerOnScreen();
        window.setTitle("All annotations for Loinc " + getLoincIdSelected().getLOINC_Number());
        window.initStyle(StageStyle.UTILITY);
        window.initModality(Modality.APPLICATION_MODAL);
        Parent root = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("/fxml/currentAnnotation.fxml"));

//            This sets the same controller factory (Callback) as above using method reference syntax (in single line)
//            fxmlLoader.setControllerFactory(injector::getInstance);

            fxmlLoader.setControllerFactory(new Callback<Class<?>, Object>() {
                 @Override
                 public Object call(Class<?> clazz) {
                     return injector.getInstance(clazz);
                 }
            });

            root = fxmlLoader.load();
            Scene scene = new Scene(root, 800, 600);

            window.setScene(scene);
            window.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is called from the pop up window
     * @param loincAnnotation passed from the pop up window
     */
    protected void editCurrentAnnotation(UniversalLoinc2HPOAnnotation loincAnnotation) {

        setLoincIdSelected(loincAnnotation.getLoincId());
        model.setLoincUnderEditing(model.getLoincEntryMap().get(loincAnnotation.getLoincId()));

        Map<String, Code> internalCode = CodeSystemConvertor.getCodeContainer().getCodeSystemMap().get(Loinc2HPOCodedValue.CODESYSTEM);
        Code codeLow = internalCode.get("L");
        Code codeHigh = internalCode.get("H");
        Code codeNormal = internalCode.get("N");
        HpoTermId4LoincTest hpoLow = loincAnnotation.loincInterpretationToHPO(codeLow);
        HpoTermId4LoincTest hpoHigh = loincAnnotation.loincInterpretationToHPO(codeHigh);
        HpoTermId4LoincTest hpoNormal = loincAnnotation.loincInterpretationToHPO(codeNormal);
        if (hpoLow != null) {
            String hpoLowTermName = hpoLow.getHpoTerm().getName();
            annotationTextFieldLeft.setText(hpoLowTermName);
        }
        if (hpoHigh != null) {
            String hpoHighTermName = hpoHigh.getHpoTerm().getName();
            annotationTextFieldRight.setText(hpoHighTermName);
        }
        if (hpoNormal != null) {
            String hpoNormalTermName = hpoNormal.getHpoTerm().getName();
            boolean isnegated = hpoNormal.isNegated();
            annotationTextFieldMiddle.setText(hpoNormalTermName);
            inverseChecker.setSelected(isnegated);
        }

        for (Map.Entry<Code, HpoTermId4LoincTest> entry : loincAnnotation.getCandidateHpoTerms().entrySet()) {
            if (entry.getKey().getSystem() != Loinc2HPOCodedValue.CODESYSTEM) {
                tempAdvancedAnnotations.add(new Annotation(entry.getKey(), entry.getValue()));
            }
        }

        boolean flag = loincAnnotation.getFlag();
        flagForAnnotation.setSelected(flag);
        String comment = loincAnnotation.getNote();
        annotationNoteField.setText(comment);

        createAnnotationButton.setText("Save");
        clearButton.setText("Cancel");

    }


    @FXML
    private void handleClear(ActionEvent event) {
        annotationTextFieldLeft.clear();
        annotationTextFieldMiddle.clear();
        annotationTextFieldRight.clear();
        flagForAnnotation.setSelected(false);
        annotationNoteField.clear();
        tempAdvancedAnnotations.clear();
        switchToBasicAnnotationMode();
        if (clearButton.getText().equals("Cancel")) {
            clearButton.setText("Clear");
            model.setLoincUnderEditing(null);
        }
        createAnnotationButton.setText("Create annotation");

    }


}
