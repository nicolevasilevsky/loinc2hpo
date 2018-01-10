package org.monarchinitiative.loinc2hpo.io;


import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.File;

/**
 * This is used to figure out where VPVGui will store the viewpoint files. For instance, with linux
 * this would be /home/username/.loinc2hpo/...
 */
public class Loinc2HpoPlatform {

    /**
     * Get path to directory where HRMD-gui stores global settings.
     * The path depends on underlying operating system. Linux, Windows and OSX
     * currently supported.
     * @return File to directory
     */
    public static File getLOINC2HPODir() {
        CurrentPlatform platform = figureOutPlatform();

        File linuxPath = new File(System.getProperty("user.home") + File.separator + ".loinc2hpo");
        File windowsPath = new File(System.getProperty("user.home") + File.separator + "loinc2hpo");
        File osxPath = new File(System.getProperty("user.home") + File.separator + ".loinc2hpo");

        switch (platform) {
            case LINUX: return linuxPath;
            case WINDOWS: return windowsPath;
            case OSX: return osxPath;
            case UNKNOWN: return null;
            default:
                Alert a = new Alert(AlertType.ERROR);
                a.setTitle("Find LOINC2HPO config dir");
                a.setHeaderText(null);
                a.setContentText(String.format("Unrecognized platform. %s", platform.toString()));
                a.showAndWait();
                return null;
        }
    }

    /**
     * Get the absolute path to the viewpoint file, which is a serialized Java file (suffix {@code .ser}).
     * @param basename The plain viewpoint name, e.g., human37cd4
     * @return the absolute path,e.g., /home/user/data/immunology/human37cd4.ser
     */
    public static String getAbsoluteProjectPath(String basename) {
        File dir = getLOINC2HPODir();
        return new String(dir + File.separator + basename + ".ser");
    }


    public static String getLocalHpoOboPath() {
        File dir = getLOINC2HPODir();
        return new String(dir + File.separator + "hp.obo");
    }

    public static String getLocalHpoOwlPath() {
        File dir = getLOINC2HPODir();
        return new String(dir + File.separator + "hp.owl");
    }

    /**
     * Get the absolute path to the log file.
     * @return the absolute path,e.g., /home/user/.vpvgui/vpvgui.log
     */
    public static String getAbsoluteLogPath() {
        File dir = getLOINC2HPODir();
        return new String(dir + File.separator +  "loinc2hpo.log");
    }

    /** Return the absolute path to the settings file, which is kept in the .loinc2hpo directory in the
     * user's home directory. For simplicity assume one user per account etc. The file is a simple key:value file.
     * @return
     */
    public static String getPathToSettingsFile() {
        File dir = getLOINC2HPODir();
        return new String(dir + File.separator + "loinc2hpo.settings");
    }

    /* Based on this post: http://www.mkyong.com/java/how-to-detect-os-in-java-systemgetpropertyosname/ */
    private static CurrentPlatform figureOutPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0 || osName.indexOf("aix") >= 0) {
            return CurrentPlatform.LINUX;
        } else if (osName.indexOf("win") >= 0) {
            return CurrentPlatform.WINDOWS;
        } else if (osName.indexOf("mac") >= 0) {
            return CurrentPlatform.OSX;
        } else {
            return CurrentPlatform.UNKNOWN;
        }
    }

    public static boolean isMacintosh() {
        return figureOutPlatform().equals(CurrentPlatform.OSX);
    }




    private enum CurrentPlatform {
        LINUX("Linux"),
        WINDOWS("Windows"),
        OSX("Os X"),
        UNKNOWN("UNKNOWN");

        private String name;

        CurrentPlatform(String n) {this.name = n; }

        @Override
        public String toString() { return this.name; }
    }

}
