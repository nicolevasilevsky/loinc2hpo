package org.monarchinitiative.loinc2hpo.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.loinc2hpo.gui.HelpViewFactory;
import org.monarchinitiative.loinc2hpo.gui.SettingsViewFactory;
import org.monarchinitiative.loinc2hpo.io.Downloader;
import org.monarchinitiative.loinc2hpo.io.Loinc2HpoPlatform;
import org.monarchinitiative.loinc2hpo.model.Model;
import java.io.File;

import static org.monarchinitiative.loinc2hpo.gui.PopUps.getStringFromUser;

@Singleton
public class MainController {
    private static final Logger logger = LogManager.getLogger();
    /** Download address for {@code hp.obo}. */
    private final static String HP_OBO_URL ="https://raw.githubusercontent.com/obophenotype/human-phenotype-ontology/master/hp.obo";
    //It appears Jena Sparql API does not like obo, so also download hp.owl
    private final static String HP_OWL_URL ="https://raw.githubusercontent.com/obophenotype/human-phenotype-ontology/master/hp.owl";
    private Model model=null;



    @Inject private SetupTabController setupTabController;
    @Inject private AnnotateTabController annotateTabController;
    @Inject private Loinc2HpoAnnotationsTabController loinc2HpoAnnotationsTabController;

    @FXML private MenuBar loincmenubar;
    @FXML private MenuItem closeMenuItem;



    @FXML private void initialize() {
        this.model = new Model();
        File settings = getPathToSettingsFileAndEnsurePathExists();
        model.setPathToSettingsFile(settings.getAbsolutePath());
        if (settings.exists()) {
            model.inputSettings(settings.getAbsolutePath());
        }
        if (setupTabController==null) {
            logger.error("setupTabController is null");
            return;
        }
       setupTabController.setModel(model);
        if (annotateTabController==null) {
            logger.error("annotate Controller is null");
            return;
        }
        annotateTabController.setModel(model);
        if (loinc2HpoAnnotationsTabController==null) {
            logger.error("loinc2HpoAnnotationsTabController is null");
            return;
        }
        loinc2HpoAnnotationsTabController.setModel(model);
        if (Loinc2HpoPlatform.isMacintosh()) {
            loincmenubar.useSystemMenuBarProperty().set(true);
        }
    }

    @FXML public void downloadHPO(ActionEvent e) {
        String dirpath= Loinc2HpoPlatform.getLOINC2HPODir().getAbsolutePath();
        File f = new File(dirpath);
        if (f==null || ! (f.exists() && f.isDirectory())) {
            logger.trace("Cannot download hp.obo, because directory not existing at " + f.getAbsolutePath());
            return;
        }
        String BASENAME="hp.obo";
        String BASENAME_OWL = "hp.owl";

        ProgressIndicator pb = new ProgressIndicator();
        javafx.scene.control.Label label=new javafx.scene.control.Label("downloading hp.obo...");
        FlowPane root = new FlowPane();
        root.setPadding(new Insets(10));
        root.setHgap(10);
        root.getChildren().addAll(label,pb);
        Scene scene = new Scene(root, 400, 100);
        Stage window = new Stage();
        window.setTitle("HPO download");
        window.setScene(scene);

        Task hpodownload = new Downloader(dirpath, HP_OBO_URL,BASENAME,pb);
        new Thread(hpodownload).start();
        hpodownload = new Downloader(dirpath, HP_OWL_URL, BASENAME_OWL, pb);
        new Thread(hpodownload).start();
        window.show();
        hpodownload.setOnSucceeded(event -> {
            window.close();
            logger.trace(String.format("Successfully downloaded hpo to %s",dirpath));
            String fullpath=String.format("%s%shp.obo",dirpath,File.separator);
            String fullpath_owl = String.format("%s%shp.owl", dirpath, File
                    .separator);
            model.setPathToHpOboFile(fullpath);
            model.setPathToHpOwlFile(fullpath_owl);
            model.writeSettings();
        });
        hpodownload.setOnFailed(event -> {
            window.close();
            logger.error("Unable to download HPO obo file");
        });
       // Thread thread = new Thread(hpodownload);
        //thread.start();

        e.consume();
    }

    @FXML public  void setPathToLoincCoreTableFile(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose LOINC Core Table file");
        File f = chooser.showOpenDialog(null);
        if (f != null) {
            String path = f.getAbsolutePath();
            model.setPathToLoincCoreTableFile(path);
            logger.trace(String.format("Setting path to LOINC Core Table file to %s",path));
        } else {
            logger.error("Unable to obtain path to LOINC Core Table file");
        }
        model.writeSettings();
        e.consume();
    }

    @FXML public void close(ActionEvent e) {
        Platform.exit();
        System.exit(0);
    }

    /**
     * This function will create the .loinc2hpo directory in the user's home directory if it does not yet exist.
     * Then it will return the path of the settings file.
     * @return
     */
    private File getPathToSettingsFileAndEnsurePathExists() {
        File loinc2HpoUserDir = Loinc2HpoPlatform.getLOINC2HPODir();
        if (!loinc2HpoUserDir.exists()) {
            File fck = new File(loinc2HpoUserDir.getAbsolutePath());
            if (!fck.mkdir()) { // make sure config directory is created, exit if not
                logger.fatal("Unable to create LOINC2HPO config directory.\n"
                        + "Even though this is a serious problem I'm exiting gracefully. Bye.");
                System.exit(1);
            }
        }
        String defaultSettingsPath = Loinc2HpoPlatform.getPathToSettingsFile();
        File settingsFile=new File(defaultSettingsPath);
       return settingsFile;
    }

    @FXML private void setBiocuratorID(ActionEvent e) {
        String bcid=getStringFromUser("Biocurator ID", "e.g., MGM:rrabbit", "Enter biocurator ID");
        if (bcid!=null && bcid.indexOf(":")>0) {
            model.setBiocuratorID(bcid);
            model.writeSettings();
        } else {
            logger.error(String.format("Invalid biocurator ID; must be of the form MGM:rrabbit; you tried: \"%s\"",
                    bcid!=null?bcid:""));
        }
        e.consume();
    }

    /** Show the about message */
    @FXML private void aboutWindow(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("LOINC2HPO Biocuration tool");
        alert.setHeaderText("Loinc2Hpo");
        String s = "A tool for biocurating HPO mappings for LOINC laboratory codes.";
        alert.setContentText(s);
        alert.showAndWait();
        e.consume();
    }

    /** Open a help dialog */
    @FXML private void openHelpDialog() {
        HelpViewFactory.openHelpDialog();
    }

    /** Open a help dialog */
    @FXML private void openSettingsDialog() {
        SettingsViewFactory.openSettingsDialog(this.model);
    }

    @FXML private void handleSave(ActionEvent e) {

        e.consume();
        System.out.println("usr wants to save file");
        loinc2HpoAnnotationsTabController.saveLoincAnnotation();

    }
}

