import { NativeModules, PermissionsAndroid, Platform, Alert } from "react-native";

const { IntentLauncher } = NativeModules;

export async function openARActivity() {
  try {
    // ✅ Only needed on Android
    if (Platform.OS === "android") {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.CAMERA,
        {
          title: "Camera Permission",
          message: "HoloFindX needs camera access to scan objects in AR.",
          buttonPositive: "Allow",
          buttonNegative: "Deny",
          buttonNeutral: "Ask Later",
        }
      );

      if (granted !== PermissionsAndroid.RESULTS.GRANTED) {
        Alert.alert(
          "Permission Required",
          "Camera permission is required to open AR Mode."
        );
        return;
      }
    }

    // ✅ Open AR Activity only after permission granted
    if (IntentLauncher) {
      IntentLauncher.startActivity("com.holofindx.ar.ARActivity");
    } else {
      Alert.alert("Error", "IntentLauncher module not found!");
    }
  } catch (err) {
    console.log("openARActivity error:", err);
  }
}
