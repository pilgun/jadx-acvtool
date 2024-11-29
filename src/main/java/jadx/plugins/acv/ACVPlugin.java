package jadx.plugins.acv;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginContext;
import jadx.plugins.acv.ACVOptions;

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
            // System.out.println("ACVTool enabled");
        }

    }
}