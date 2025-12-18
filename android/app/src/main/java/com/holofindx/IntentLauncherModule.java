package com.holofindx;

import android.content.Intent;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class IntentLauncherModule extends ReactContextBaseJavaModule {

  public IntentLauncherModule(ReactApplicationContext context) {
    super(context);
  }

  @Override
  public String getName() {
    return "IntentLauncher";
  }

  @ReactMethod
  public void startActivity(String activityName) {
    try {
      Intent intent = new Intent();
      intent.setClassName(getReactApplicationContext(), activityName);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getReactApplicationContext().startActivity(intent);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
