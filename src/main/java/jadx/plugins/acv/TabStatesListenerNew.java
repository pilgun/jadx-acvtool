package jadx.plugins.acv;

import java.awt.Color;
import java.awt.Container;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SmartHighlightPainter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.gui.JadxGuiContext;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.tab.ITabStatesListener;
import jadx.gui.ui.tab.TabBlueprint;
import jadx.gui.ui.tab.TabsController;

public class TabStatesListenerNew implements ITabStatesListener {
    private static final Color METHOD_HIGHLIGHT_COLOR = new Color(144, 238, 144, 128); // Light green with transparency
    private static final Logger LOG = LoggerFactory.getLogger(TabStatesListenerNew.class);
    private JadxGuiContext guiContext;
    private MainWindow mainWindow;
    private final HashMap<String, List<String>> executedMethodMap;
    private final List<Object> highlightTags = new ArrayList<>();

    public TabStatesListenerNew(JadxGuiContext guiContext, MainWindow mainWindow, HashMap<String, List<String>> executedMethodMap) {
        this.guiContext = guiContext;
        this.mainWindow = mainWindow;
        this.executedMethodMap = executedMethodMap;
    }

    @Override
    public void onTabOpen(TabBlueprint blueprint) {
        LOG.debug("Tab opened: {}", blueprint.getNode().getName());
        // Delay highlighting slightly to ensure UI is ready
        guiContext.uiRun(() -> {
            try {
                Thread.sleep(100); // Small delay to ensure content is loaded
                highlightMethodsInCurrentTab();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    @Override
    public void onTabSelect(TabBlueprint blueprint) {
        LOG.debug("Tab selected: {}", blueprint.getNode().getName());
        // Highlight when tab is selected/switched to
        guiContext.uiRun(() -> highlightMethodsInCurrentTab());
    }

    private void highlightMethodsInCurrentTab() {
        if (mainWindow == null) {
            LOG.warn("MainWindow is null, cannot highlight methods");
            return;
        }
        
        try {
            CodeArea codeArea = getCurrentCodeArea();
            if (codeArea != null) {
                highlightMethodsUsingMetadata(codeArea);
                LOG.debug("Method highlighting applied to current tab using metadata");
            } else {
                LOG.debug("No code area found in current tab");
            }
        } catch (Exception e) {
            LOG.error("Error highlighting methods in current tab", e);
        }
    }

    @Nullable
    private CodeArea getCurrentCodeArea() {
        if (mainWindow == null) return null;
        
        try {
            Container contentPane = mainWindow.getTabbedPane().getSelectedContentPanel();
            if (contentPane instanceof AbstractCodeContentPanel) {
                AbstractCodeArea codeArea = ((AbstractCodeContentPanel) contentPane).getCodeArea();
                if (codeArea instanceof CodeArea) {
                    return (CodeArea) codeArea;
                }
            }
        } catch (Exception e) {
            LOG.warn("Error getting current code area", e);
        }
        return null;
    }
    
    private void highlightMethodsUsingMetadata(CodeArea codeArea) {
        try {
            // Clear previous highlights first
            clearMethodHighlights(codeArea);

            // Get the RSyntaxTextArea for highlighting
            RSyntaxTextArea textArea = getTextAreaFromCodeArea(codeArea);
            if (textArea == null) {
                LOG.warn("Could not access text area for highlighting");
                return;
            }
            
            // Use CodeArea's built-in metadata to find methods
            String text = textArea.getText();
            if (text == null || text.isEmpty()) {
                LOG.debug("No text content to highlight");
                return;
            }
            
            // Get code info with metadata
            jadx.api.ICodeInfo codeInfo = codeArea.getCodeInfo();
            if (!codeInfo.hasMetadata()) {
                LOG.debug("No metadata available for highlighting");
                return;
            }
            
            SmartHighlightPainter painter = new SmartHighlightPainter(METHOD_HIGHLIGHT_COLOR);
            int highlightCount = 0;
            
            // Scan through the text to find method declarations using metadata
            for (int pos = 0; pos < text.length(); pos++) {
                try {
                    jadx.api.metadata.ICodeAnnotation annotation = codeInfo.getCodeMetadata().getAt(pos);
                    if (annotation != null && annotation.getAnnType() == jadx.api.metadata.ICodeAnnotation.AnnType.DECLARATION) {
                        // Get the Java node at this position
                        jadx.api.JavaNode javaNode = codeArea.getJadxWrapper().getDecompiler()
                                .getJavaNodeByCodeAnnotation(codeInfo, annotation);
                        
                        if (javaNode instanceof jadx.api.JavaMethod) {
                            jadx.api.JavaMethod javaMethod = (jadx.api.JavaMethod) javaNode;
                            String methodSignature = javaMethod.getMethodNode().getMethodInfo().getShortId();
                            String classDescriptor = javaMethod.getDeclaringClass().getClassNode().getClassInfo().getAliasFullPath();
                            String fullClassDesc = "L" + classDescriptor + ";";
                            LOG.info("{}->{}", fullClassDesc, methodSignature);
                            // else {
                            //     LOG.info("Skipping method '{}' as it is not in executedMethodMap", methodSignature);
                            //     continue; // Skip methods not in executedMethodMap
                            // }
                            // Find the end of this method declaration line
                            int lineStart = pos;
                            int lineEnd = text.indexOf('\n', pos);
                            if (lineEnd == -1) lineEnd = text.length();
                            
                            // Skip backwards to find the actual start of the line
                            while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
                                lineStart--;
                            }
                            
                            try {
                                // LOG.info("Trying: {}->{}", fullClassDesc, methodSignature);
                                // if(executedMethodMap.containsKey(fullClassDesc)){
                                //     LOG.info("fullClassDesc matches");
                                //     LOG.info(String.join(",", executedMethodMap.get(fullClassDesc)));
                                // } else {
                                //     LOG.info("fullClassDesc does not match");
                                // }
                                if (executedMethodMap.containsKey(fullClassDesc) && 
                                executedMethodMap.get(fullClassDesc).contains(methodSignature)) {
                                LOG.info("Highlighting {}->{}", fullClassDesc, methodSignature);

                                    Object tag = textArea.getHighlighter().addHighlight(lineStart, lineEnd, painter);
                                    highlightTags.add(tag);
                                    highlightCount++;
                                    LOG.info("Highlighted method '{}' at position {}-{}", methodSignature, lineStart, lineEnd);
                                } else {
                                    LOG.info("Skipping {}->{}", fullClassDesc, methodSignature);
                                }                             
                                // Skip ahead to avoid highlighting the same method multiple times
                                pos = lineEnd;
                                
                            } catch (BadLocationException e) {
                                LOG.warn("Failed to highlight method at position {}: {}", pos, e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.debug("Error processing position {}: {}", pos, e.getMessage());
                }
            }
            
            LOG.info("Successfully highlighted {} methods using CodeArea metadata", highlightCount);
        } catch (Exception e) {
            LOG.error("Error highlighting methods using metadata", e);
        }
    }
    
    @Nullable
    private RSyntaxTextArea getTextAreaFromCodeArea(CodeArea codeArea) {
        try {
            // Try to access the text component through reflection or public methods
            RSyntaxTextArea textArea = null;
            
            // Method 1: Try to get through reflection
            try {
                java.lang.reflect.Method[] methods = codeArea.getClass().getMethods();
                for (java.lang.reflect.Method method : methods) {
                    String methodName = method.getName();
                    if (methodName.equals("getCodeArea") || methodName.equals("getTextArea") || 
                        methodName.equals("getEditorPane") || methodName.equals("getTextComponent")) {
                        Object result = method.invoke(codeArea);
                        if (result instanceof RSyntaxTextArea) {
                            textArea = (RSyntaxTextArea) result;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debug("Failed to access text area through reflection: {}", e.getMessage());
            }
            
            // Method 2: Try accessing through component hierarchy
            if (textArea == null) {
                try {
                    java.awt.Component[] components = codeArea.getComponents();
                    textArea = findRSyntaxTextAreaInComponents(components);
                } catch (Exception e) {
                    LOG.debug("Failed to find RSyntaxTextArea in component hierarchy: {}", e.getMessage());
                }
            }
            
            // Method 3: Since CodeArea extends RSyntaxTextArea, try direct cast
            if (textArea == null) {
                try {
                    if (codeArea instanceof RSyntaxTextArea) {
                        textArea = (RSyntaxTextArea) codeArea;
                        LOG.debug("Successfully cast CodeArea to RSyntaxTextArea");
                    }
                } catch (Exception e) {
                    LOG.debug("Failed to cast CodeArea to RSyntaxTextArea: {}", e.getMessage());
                }
            }
            
            return textArea;
        } catch (Exception e) {
            LOG.warn("Error getting text area from CodeArea", e);
            return null;
        }
    }
    
    private RSyntaxTextArea findRSyntaxTextAreaInComponents(java.awt.Component[] components) {
        for (java.awt.Component component : components) {
            if (component instanceof RSyntaxTextArea) {
                return (RSyntaxTextArea) component;
            }
            if (component instanceof java.awt.Container) {
                java.awt.Container container = (java.awt.Container) component;
                RSyntaxTextArea found = findRSyntaxTextAreaInComponents(container.getComponents());
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
    
    private void clearMethodHighlights(CodeArea codeArea) {
        try {
            // Try to access the text component through the same methods as highlighting
            RSyntaxTextArea textArea = getTextAreaFromCodeArea(codeArea);
            
            if (textArea != null) {
                // Remove our specific highlights
                for (Object tag : highlightTags) {
                    try {
                        textArea.getHighlighter().removeHighlight(tag);
                    } catch (Exception e) {
                        // Tag might have been removed already, ignore
                    }
                }
            }
            highlightTags.clear();
            
            LOG.debug("Cleared previous method highlights");
        } catch (Exception e) {
            LOG.warn("Failed to clear method highlights", e);
        }
    }
    
    public void cleanup() {
        // Clean up when plugin is disposed
        if (mainWindow != null) {
            try {
                TabsController tabsController = mainWindow.getTabsController();
                tabsController.removeListener(this);
                LOG.info("Removed tab listener");
            } catch (Exception e) {
                LOG.warn("Error removing tab listener", e);
            }
        }
        
        // Clear any remaining highlights
        highlightTags.clear();
        
        LOG.info("Auto Method Highlight Plugin disposed");
    }
}
