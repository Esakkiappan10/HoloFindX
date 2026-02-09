package com.holofindx;

import android.content.Intent;
import android.util.Log;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

/**
 * 🌉 React Native Bridge to Launch Native Activities
 */
public class IntentLauncherModule extends ReactContextBaseJavaModule {

    private static final String TAG = "IntentLauncher";

    public IntentLauncherModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "IntentLauncher";
    }

    /**
     * Launch any activity by fully qualified class name
     * Usage: IntentLauncher.startActivity("com.holofindx.ar.ARActivity")
     */
    @ReactMethod
    public void startActivity(String activityName, Promise promise) {
        try {
            ReactApplicationContext context = getReactApplicationContext();
            android.app.Activity currentActivity = getCurrentActivity();

            if (currentActivity == null) {
                Log.e(TAG, "❌ No current activity available");
                if (promise != null) {
                    promise.reject("NO_ACTIVITY", "Current activity is null");
                }
                return;
            }

            Log.d(TAG, "🚀 Launching activity: " + activityName);

            Intent intent = new Intent();
            intent.setClassName(context, activityName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            currentActivity.startActivity(intent);
            if (promise != null) {
                promise.resolve(true);
            }

            Log.d(TAG, "✅ Activity launched successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to launch activity: " + e.getMessage(), e);
            if (promise != null) {
                promise.reject("LAUNCH_ERROR", e.getMessage(), e);
            }
        }
    }

    /**
     * Launch AR Activity (convenience method)
     */
    @ReactMethod
    public void launchARActivity(Promise promise) {
        startActivity("com.holofindx.ar.ARActivity", promise);
    }

    /**
     * Launch Diagnostic Test Activity (convenience method)
     */
    @ReactMethod
    public void launchDiagnosticTest(Promise promise) {
        startActivity("com.holofindx.ar.DiagnosticTestActivity", promise);
    }
}