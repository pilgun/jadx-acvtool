package jadx.plugins.acv;

import jadx.api.metadata.ICodeNodeRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.ICodeNode;

import java.util.HashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ACVAction implements Consumer<ICodeNodeRef> {

    private static final Logger LOG = LoggerFactory.getLogger(ACVAction.class);

    private final ACVOptions options;
    private final HashMap<String, String> classMap;
    private final ACVReportFiles acvReportFiles;

    public ACVAction(ACVReportFiles acvReportFiles, ACVOptions options, HashMap<String, String> classMap) {
        this.acvReportFiles = acvReportFiles;
        this.options = options;
        this.classMap = classMap;
    }

    @Override
    public void accept(ICodeNodeRef codeNodeRef) {
        System.out.println("ACVAction: accept");
        if (codeNodeRef == null) {
            return;
        }
        System.out.println(codeNodeRef);
        System.out.println(codeNodeRef.getAnnType());
        if (codeNodeRef.getAnnType() == ICodeNodeRef.AnnType.CLASS) {
            String className = ((ClassNode) codeNodeRef).getFullName();
            if (classMap.isEmpty()) {
                acvReportFiles.scanAcvReportClasses();
            }
            if (classMap.containsKey(className)) {
                ACVReportFiles.openAcvFile(className, classMap);
            }
            else{
                LOG.error()
            }
        }
        if (codeNodeRef.getAnnType() == ICodeNodeRef.AnnType.DECLARATION) {
            System.out.println("ACVAction: DECLARATION");
        }
    }

    public static Boolean canActivate(ICodeNodeRef codeNodeRef) {
        if (codeNodeRef == null) {
            return Boolean.FALSE;
        }
        System.out.println((ICodeNode) codeNodeRef);
        if (codeNodeRef.getAnnType() == ICodeNodeRef.AnnType.CLASS) {
            System.out.println("ACVAction: CLASS " + codeNodeRef.getClass().getName());
            return Boolean.TRUE;

        } else if (codeNodeRef.getAnnType() == ICodeNodeRef.AnnType.DECLARATION) {
            System.out.println("ACVAction: DECLARATION" + codeNodeRef.getClass().getName());
        }
        return Boolean.FALSE;
    }
}
