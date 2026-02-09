package com.holofindx;

import android.view.View;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ReactShadowNode;
import com.facebook.react.uimanager.ViewManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 📦 React Native Package for IntentLauncher
 */
public class IntentLauncherPackage implements ReactPackage {

    @Override
    public List<NativeModule> createNativeModules(
        ReactApplicationContext reactContext
    ) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new IntentLauncherModule(reactContext));
        return modules;
    }

    @Override
    public List<ViewManager> createViewManagers(
        ReactApplicationContext reactContext
    ) {
        return Collections.emptyList();
    }
}