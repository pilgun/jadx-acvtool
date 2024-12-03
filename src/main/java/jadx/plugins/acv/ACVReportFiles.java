package jadx.plugins.acv;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JOptionPane;

import java.awt.Desktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.gui.JadxGuiContext;

public class ACVReportFiles {

    private static final Logger LOG = LoggerFactory.getLogger(ACVReportFiles.class);

    private final ACVOptions options;
    private final HashMap<String, String> classMap;
    private final JadxGuiContext guiContext;

    public ACVReportFiles(JadxGuiContext guiContext, ACVOptions options, HashMap<String, String> classMap) {
        this.guiContext = guiContext;
        this.options = options;
        this.classMap = classMap;
    }

    public static void openAcvFile(String rawName, HashMap<String, String> classMap) {

        String classPath = classMap.get(rawName);
        try {
            LOG.info("ACVPlugin: Opening file: {}", classPath);
            Desktop.getDesktop().open(new File(classPath));
        } catch (IOException e1) {
            LOG.error("could not open file: {}", classPath);
            e1.printStackTrace();
        }
    }

    public void scanAcvReportClasses() {
        LOG.info("ACVPlugin: Scan ACV Report Classes");
        classMap.clear();
        String reportPath = options.getAcvtoolReportPath();
        File reportDirectory = new File(reportPath);
        if (!reportDirectory.exists()) {
            LOG.error("ACVPlugin: ACVTool report directory not found: {}", reportPath);
            JOptionPane.showMessageDialog(guiContext.getMainFrame(),
                    "ACVTool report directory not found: " + reportPath,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File[] directories = reportDirectory.listFiles(File::isDirectory);
        if (directories != null) {
            for (File directory : directories) {
                LOG.info("ACVPlugin: Found directory: {}", directory.getAbsolutePath());
                HashMap<String, String> dexClassMap = findClasses(directory, "");
                classMap.putAll(dexClassMap);
            }
            if (classMap.isEmpty()) {
                LOG.error("ACVPlugin: No smali classes found. Check the acv report directory.");
                JOptionPane.showMessageDialog(guiContext.getMainFrame(),
                        "ACVPlugin: No smali classes found. Check the acv report directory.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        LOG.info("ACVPlugin: Found {} classes", classMap.size());
        System.out.println("ACVPlugin: Found " + classMap.size() + " classes");
    }

    private HashMap<String, String> findClasses(File directory, String pathPrefix) {
        HashMap<String, String> classMap = new HashMap<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    classMap.putAll(findClasses(file, pathPrefix + file.getName() + "."));
                } else if (file.isFile() && file.getName().endsWith(".smali.html")) {
                    String key = pathPrefix + file.getName().substring(0, file.getName().length() - 11);
                    String value = file.getAbsolutePath();
                    classMap.put(key, value);
                }
            }
        }
        return classMap;
    }
}
