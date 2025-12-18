import { NativeModules } from "react-native";

const { IntentLauncher } = NativeModules;

export function openARActivity() {
  if (IntentLauncher) {
    IntentLauncher.startActivity("com.holofindx.ar.ARActivity");
  }
}
