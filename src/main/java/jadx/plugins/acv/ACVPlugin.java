package jadx.plugins.acv;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.gui.JadxGuiContext;

import jadx.api.plugins.JadxPluginContext;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ACVPlugin implements JadxPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(ACVPlugin.class);

    public static final String PLUGIN_ID = "acvtool-plugin";

    private final ACVOptions options = new ACVOptions();

    @Override
    public JadxPluginInfo getPluginInfo() {
        return new JadxPluginInfo(PLUGIN_ID, "ACVTool plugin", "Visualize instruction coverage");
    }

    @Override
    public void init(JadxPluginContext context) {
        context.registerOptions(options);
        if (options.isEnable()) {
            System.out.println("ACVTool enabled");

        } else {
            System.out.println("ACVTool disabled");
        }
        // JFrame frame = context.getGuiContext().getMainFrame();
        JadxGuiContext guiContext = context.getGuiContext();
        if (guiContext != null) {
            ACVAction acvAction = new ACVAction(context, options);
            guiContext.addPopupMenuAction("acv report", ACVAction::canActivate, null, acvAction);

            Runnable acvReportScan = () -> {
                System.out.println("ACVPlugin: Scan ACV Report Classes");
                String reportPath = options.getAcvtoolReportPath();
                File reportDirectory = new File(reportPath);
                File[] directories = reportDirectory.listFiles(File::isDirectory);
                if (directories != null) {
                    HashMap<String, String> classMap = new HashMap<>();
                    for (File directory : directories) {
                        LOG.info("ACVPlugin: Found directory: {}", directory.getAbsolutePath());
                        System.out.println("ACVPlugin: Found directory: " + directory.getAbsolutePath());
                        HashMap<String, String> dexClassMap = findClasses(directory, "");
                        classMap.putAll(dexClassMap);
                    }
                }
                List<File> smaliHtmlFiles = findFilesWithExtension(reportPath, ".smali.html");
            };
            guiContext.addMenuAction("Scan ACV Report Classes", acvReportScan);
        }
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
                    LOG.info("ACVPlugin: Found class: {}-{}", key, value);
                }
            }
        }
        return classMap;
    }

    private List<File> findFilesWithExtension(String directoryPath, String extension) {
        List<File> files = new ArrayList<>();
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] fileList = directory.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    if (file.isFile() && file.getName().endsWith(extension)) {
                        files.add(file);
                        // System.out.println("ACVPlugin: Found file: " + file.getAbsolutePath());
                    } else if (file.isDirectory()) {
                        files.addAll(findFilesWithExtension(file.getAbsolutePath(), extension));
                    }
                }
            }
        }
        return files;
    }
}