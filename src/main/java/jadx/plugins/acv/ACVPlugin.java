package jadx.plugins.acv;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.gui.plugins.context.CommonGuiPluginsContext;
import jadx.gui.plugins.context.GuiPluginContext;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.tab.TabBlueprint;
import jadx.gui.ui.tab.TabsController;
import jadx.api.plugins.JadxPluginContext;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import java.awt.Component;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ACVPlugin implements JadxPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(ACVPlugin.class);

    public static final String PLUGIN_ID = "acvtool-plugin";

    private final ACVOptions options = new ACVOptions();
    private final HashMap<String, String> classMap = new HashMap<>();
    private final HashMap<String, List<String>> executedMethodMap = new HashMap<>();
    private ACVReportFiles acvReportFiles;
    private TabStatesListenerNew tabStatesListener;
    private TabsController tabsController;

    @Override
    public JadxPluginInfo getPluginInfo() {
        return new JadxPluginInfo(PLUGIN_ID, "ACVTool Plugin",
                "Highlighting executed instructions with ACVTool.\n\n" +
                        "Open the ACVTool smali report for the selected class.");
    }

    @Override
    public void init(JadxPluginContext context) {
        LOG.info("ACVPlugin init");
        context.registerOptions(options);
        if (options.isEnable()) {
            LOG.info("ACVTool enabled");
            JadxGuiContext guiContext = context.getGuiContext();
            acvReportFiles = new ACVReportFiles(guiContext, options, classMap);
            if (guiContext != null) {
                ACVAction acvAction = new ACVAction(acvReportFiles, options, classMap);
                guiContext.addPopupMenuAction("ACV: Open Class", ACVAction::canActivate, null, acvAction);
                addButton(guiContext);
                addPluginMenuButton(guiContext, acvReportFiles);
                readJsonClassMethodMap();
                setupAutoHighlighting(guiContext);
            }
        } else {
            LOG.info("ACVTool disabled");
            JOptionPane.showMessageDialog(null, "ACVTool is disabled", "Disabled", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void readJsonClassMethodMap() {
        LOG.info("Reading JSON class method map");
        String jsonPath = this.options.getAcvtoolMethodJsonPath();
        if (jsonPath == null || jsonPath.trim().isEmpty()) {
            LOG.warn("ACV method JSON path not configured");
            return;
        }
        File jsonFile = new File(jsonPath);
        if (!jsonFile.exists()) {
            LOG.error("ACV method JSON file not found: {}", jsonPath);
            return;
        }
        try (FileReader reader = new FileReader(jsonFile)) {
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, List<String>>>(){}.getType();
            executedMethodMap.putAll(gson.fromJson(reader, mapType));
            // for (Map.Entry<String, List<String>> entry : executedMethodMap.entrySet()) {
            //     String className = entry.getKey();
            //     List<String> methods = entry.getValue();
            //     LOG.info("Class: {}, Methods: {}", className, methods);
            // }
            LOG.info("ACV method JSON file loaded successfully: {}", jsonPath);
        } catch (Exception e) {
            LOG.error("Error reading ACV method JSON file: {}", jsonPath, e);
        }
    }


    private void setupAutoHighlighting(JadxGuiContext guiContext) {
        if (guiContext instanceof GuiPluginContext){
            LOG.info("instance of GuiPluginContext");
            GuiPluginContext pluginContext = (GuiPluginContext) guiContext;
            CommonGuiPluginsContext commonContext = pluginContext.getCommonContext();
            MainWindow mainWindow = commonContext.getMainWindow();
            this.tabsController = mainWindow.getTabsController();
            this.tabStatesListener = new TabStatesListenerNew(guiContext, mainWindow, this.executedMethodMap);
            this.tabsController.addListener(this.tabStatesListener);
            LOG.info("Added improved tab states listener for method highlighting");
        }
    }

    private void addPluginMenuButton(JadxGuiContext guiContext, ACVReportFiles acvReportFiles) {
        Runnable acvReportScan = () -> {
            acvReportFiles.scanAcvReportClasses();
        };
        guiContext.addMenuAction("Re-scan ACV Report Classes", acvReportScan);
    }

    private void addButton(JadxGuiContext guiContext) {
        String acvButtonName = "acvButton";
        URL res = ACVPlugin.class.getResource("/icons/acv16.png");
        ImageIcon icon = new ImageIcon(res);
        JButton button = new JButton();
        if (icon != null) {
            button.setIcon(icon);
        }
        button.setToolTipText("ACV");
        button.setName(acvButtonName);

        button.addActionListener(e -> {
            if (classMap.isEmpty()) {
                acvReportFiles.scanAcvReportClasses();
            }
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
            if (!classMap.containsKey(rawName)) {
                LOG.error("ACVPlugin: Class not found: {}. Check the acv report directory.", rawName);
                JOptionPane.showMessageDialog(guiContext.getMainFrame(), "Class not found: " + rawName, "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            ACVReportFiles.openAcvFile(rawName, classMap);
        });
        JToolBar toolbar = (JToolBar) ((MainWindow) guiContext.getMainFrame()).getContentPane().getComponent(2);
        // Check if the button already exists, so we don't get several buttons when
        // reinstalling the plugin.
        for (Component component : toolbar.getComponents()) {
            if (component instanceof JButton) {
                JButton existingButton = (JButton) component;
                if (acvButtonName.equals(existingButton.getName())) {
                    System.out.println("ACVTool button already exists, removing it");
                    toolbar.remove(component);
                }
            }
        }
        toolbar.add(button);
        toolbar.revalidate();
    }

    public void dispose() {
        if (this.tabsController != null) {
            this.tabsController.removeListener(this.tabStatesListener);
            this.tabsController.closeAllTabs();
            this.tabsController = null;
        }
        if (this.tabStatesListener != null) {
            this.tabStatesListener.cleanup();
            this.tabStatesListener = null;
        }
        LOG.info("ACVPlugin disposed");
    }

}