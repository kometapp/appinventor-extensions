package com.google.appinventor.client.editor.simple;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.shared.rpc.project.ChecksumedFileException;
import com.google.appinventor.shared.rpc.project.ChecksumedLoadFile;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidProjectNode;
import com.google.gwt.core.client.ScriptInjector;

import java.util.HashMap;
import java.util.Map;

import static com.google.appinventor.client.Ode.MESSAGES;

public class MockScriptsManager {

    private static final Map<String, String> scriptsMap = new HashMap<>();

    public static void load(final String type) {
        if (scriptsMap.containsKey(type)) {
            return; // script already loaded; don't load again!
        }

        Ode ode = Ode.getInstance();

        long projectId = ode.getCurrentYoungAndroidProjectId();

        String pkgName = type.substring(0, type.lastIndexOf('.'));
        String simpleName = type.substring(type.lastIndexOf('.') + 1);

        if (!SimpleComponentDatabase.getInstance(projectId).hasCustomMock(simpleName)) {
            return; // Component doesn't have its own custom Mock, so don't try to load it
        }

        String componentsFolder = ((YoungAndroidProjectNode) ode.getCurrentYoungAndroidProjectRootNode())
                .getComponentsFolder().getFileId();

        String fileId = componentsFolder + "/" + pkgName + "/Mock" + simpleName + ".js";

        ode.getProjectService().load2(
                projectId,
                fileId,
                new OdeAsyncCallback<ChecksumedLoadFile>(MESSAGES.loadError()) {
                    @Override
                    public void onSuccess(ChecksumedLoadFile result) {
                        try {
                            String mockJsFile = result.getContent();

                            scriptsMap.put(type, mockJsFile);

                            ScriptInjector.fromString(mockJsFile)
                                    .setWindow(ScriptInjector.TOP_WINDOW)
                                    .inject();

                        } catch (ChecksumedFileException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    public static void upgrade(String type) {
        if (scriptsMap.containsKey(type)) {
            unload(type);
            load(type);
        }
    }

    public static void unload(String type) {
        String simpleName = type.substring(type.lastIndexOf('.') + 1);
        scriptsMap.remove(type);
        MockComponentRegistry.unregister(simpleName);
        cleanup(simpleName);
    }

    private static native void cleanup(String name)/*-{
        delete $wnd["Mock" + name];
    }-*/;
}
