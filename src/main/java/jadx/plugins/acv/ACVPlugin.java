package jadx.plugins.acv;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.gui.JadxGuiContext;

import jadx.api.plugins.JadxPluginContext;

public class ACVPlugin implements JadxPlugin {
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
        // todo: redefine getContentPane() method in the JClass
        JadxGuiContext guiContext = context.getGuiContext();
        if (guiContext != null) {
            ACVAction acvAction = new ACVAction(context, options);
            guiContext.addPopupMenuAction("acv report", ACVAction::canActivate, null, acvAction);
        }
    }
}