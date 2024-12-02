package jadx.plugins.acv;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.tab.TabBlueprint;
import jadx.api.plugins.JadxPluginContext;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import java.awt.Component;
import java.awt.Desktop;

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
                    if (!classMap.isEmpty()) {
                        addButton("ACV", guiContext, classMap);
                    }
                }
            };
            guiContext.addMenuAction("Scan ACV Report Classes", acvReportScan);
        }
    }

    private void addButton(String text, JadxGuiContext guiContext, HashMap<String, String> classMap) {
        String acvButtonName = "acvButton";
        URL res = ACVPlugin.class.getResource("/icons/acv16.png");
        ImageIcon icon = new ImageIcon(res);
        JButton button = new JButton();
        if (icon != null) {
            button.setIcon(icon);
        }
        button.setName(acvButtonName);

        button.addActionListener(e -> {
            System.out.println("ACV button clicked");
            TabBlueprint selectedTab = ((MainWindow) guiContext.getMainFrame()).getTabsController().getSelectedTab();
            if (selectedTab == null) {
                LOG.error("ACVPlugin: No tab selected");
                JOptionPane.showMessageDialog(guiContext.getMainFrame(), "No tab selected", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            String rawName = selectedTab.getNode()
                    .getRootClass().getCls().getRawName();
            System.out.println(rawName);
            if (!classMap.containsKey(rawName)) {
                LOG.error("ACVPlugin: Class not found: {}. Check the acv report directory.", rawName);
                JOptionPane.showMessageDialog(guiContext.getMainFrame(), "Class not found: " + rawName, "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            String classPath = classMap.get(rawName);
            try {
                LOG.info("ACVPlugin: Opening file: {}", classPath);
                Desktop.getDesktop().open(new File(classPath));
            } catch (IOException e1) {
                LOG.error("could not open file: {}", classPath);
                e1.printStackTrace();
            }
        });
        // ActionHandler action = new ActionHandler()
        JToolBar toolbar = (JToolBar) ((MainWindow) guiContext.getMainFrame()).getContentPane().getComponent(2);
        System.out.println(toolbar);
        System.out.println(toolbar.getComponentCount());
        System.out.println((JButton) toolbar.getComponents()[0]);
        // Check if the button already exists
        for (Component component : toolbar.getComponents()) {
            // System.out.println(component);
            if (component instanceof JButton) {
                JButton existingButton = (JButton) component;
                if (acvButtonName.equals(existingButton.getName())) {
                    System.out.println("Button with text '" + text + "' already exists, removing it");
                    toolbar.remove(component);
                }
            }
        }
        toolbar.add(button);
        toolbar.revalidate();
        System.out.println(button);
        System.out.println(toolbar.getComponentCount());
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
                    // LOG.info("ACVPlugin: Found class: {}-{}", key, value);
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