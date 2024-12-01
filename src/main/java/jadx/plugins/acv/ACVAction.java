package jadx.plugins.acv;

import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.JadxPluginContext;

import java.util.function.Consumer;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class ACVAction implements Consumer<ICodeNodeRef> {

    // private static final Logger LOG = LoggerFactory.getLogger(ACVAction.class);

    private final JadxPluginContext context;
    private final ACVOptions options;

    public ACVAction(JadxPluginContext context, ACVOptions options) {
        this.context = context;
        this.options = options;
    }

    @Override
    public void accept(ICodeNodeRef codeNodeRef) {
        System.out.println("ACVAction: accept");
        if (codeNodeRef == null) {
            return;
        }
        if (codeNodeRef.getAnnType() == ICodeNodeRef.AnnType.CLASS) {
            System.out.println("ACVAction: class");
        } else if (codeNodeRef.getAnnType() == ICodeNodeRef.AnnType.DECLARATION) {
            System.out.println("ACVAction: declaration");
        }
    }

    public static Boolean canActivate(ICodeNodeRef codeNodeRef) {
        System.out.println("ACVPlugin: canActivate");
        System.out.println(codeNodeRef);
        System.out.println(codeNodeRef.getAnnType());
        return Boolean.TRUE;
        // return codeNodeRef != null && (codeNodeRef.getAnnType() ==
        // ICodeNodeRef.AnnType.CLASS
        // || codeNodeRef.getAnnType() == ICodeNodeRef.AnnType.DECLARATION);
    }

}
