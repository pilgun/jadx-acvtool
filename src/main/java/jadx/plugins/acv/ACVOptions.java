package jadx.plugins.acv;

import jadx.api.plugins.options.OptionFlag;
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;
// import java.nio.file.Path;
import java.nio.file.Paths;

public class ACVOptions extends BasePluginOptionsBuilder {

    private boolean enable;
    private String acvtoolReportPath;

    @Override
    public void registerOptions() {
        boolOption(ACVPlugin.PLUGIN_ID + ".enable")
                .description("Enable ACVTool view")
                .defaultValue(false)
                .setter(o -> enable = o);
        String defaultReportPath = Paths.get(System.getProperty("user.home"), "acvtool", "acvtool_working_dir",
                "report").toString();
        strOption(ACVPlugin.PLUGIN_ID + ".report-path")
                .description("ACVTool report directory")
                .defaultValue(defaultReportPath)
                .setter(o -> acvtoolReportPath = o)
                .flags(OptionFlag.PER_PROJECT, OptionFlag.NOT_CHANGING_CODE);
    }

    public boolean isEnable() {
        return enable;
    }

    public String getAcvtoolReportPath() {
        return acvtoolReportPath;
    }

}
