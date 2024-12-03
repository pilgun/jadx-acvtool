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
import java.util.HashMap;

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
    private final HashMap<String, String> classMap = new HashMap<>();
    private ACVReportFiles acvReportFiles;

    @Override
    public JadxPluginInfo getPluginInfo() {
        return new JadxPluginInfo(PLUGIN_ID, "ACVTool Plugin",
                "Highlighting executed instructions with ACVTool.\n\n" +
                        "Open the ACVTool smali report for the selected class.");
    }

    @Override
    public void init(JadxPluginContext context) {
        context.registerOptions(options);
        if (options.isEnable()) {
            System.out.println("ACVTool enabled");
            JadxGuiContext guiContext = context.getGuiContext();
            acvReportFiles = new ACVReportFiles(guiContext, options, classMap);
            if (guiContext != null) {
                ACVAction acvAction = new ACVAction(acvReportFiles, options, classMap);
                guiContext.addPopupMenuAction("ACV: Open Class", ACVAction::canActivate, null, acvAction);
                addButton(guiContext);
                addPluginMenuButton(guiContext, acvReportFiles);
            }
        } else {
            System.out.println("ACVTool disabled");
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

}