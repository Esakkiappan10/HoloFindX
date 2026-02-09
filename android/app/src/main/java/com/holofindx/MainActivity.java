package com.holofindx;

import android.os.Bundle;
import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactActivityDelegate;

/**
 * Main React Native Activity
 * 
 * NOTE: IntentLauncher registration happens in MainApplication.kt,
 * not here. This is just the entry point activity.
 */
public class MainActivity extends ReactActivity {

  /**
   * Returns the name of the main component registered from JavaScript.
   * This is used to schedule rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "HoloFindX";
  }

  /**
   * Returns the instance of the {@link ReactActivityDelegate}. 
   * Here we use a util class {@link DefaultReactActivityDelegate} which allows you to easily 
   * enable Fabric and Concurrent React (aka React 18) with two boolean flags.
   */
  @Override
  protected ReactActivityDelegate createReactActivityDelegate() {
    return new DefaultReactActivityDelegate(
        this,
        getMainComponentName(),
        // If you opted-in for the New Architecture, we enable the Fabric Renderer.
        DefaultNewArchitectureEntryPoint.getFabricEnabled()
    );
  }

  /**
   * Keep screen ON while app is running (important for AR apps).
   * Prevents screen from dimming during AR detection.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }
}