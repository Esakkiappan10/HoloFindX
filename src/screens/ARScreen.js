<<<<<<< HEAD
<<<<<<< HEAD
import React, { useEffect, useState, useRef, useCallback } from "react";
import { View, Text, StyleSheet, TouchableOpacity, Alert, Linking, AppState } from "react-native";
import { Camera, useCameraDevice, useCameraPermission } from "react-native-vision-camera";

export default function ARScreen({ route, navigation }) {
  const object = route.params?.object;
  const device = useCameraDevice('back');
  const { hasPermission, requestPermission } = useCameraPermission();
  const [isActive, setIsActive] = useState(true);
  const camera = useRef(null);

  const requestCameraPermission = useCallback(async () => {
    const permission = await requestPermission();
    if (!permission) {
      Alert.alert(
        'Camera Permission Required',
        'Please enable camera access in your device settings to use AR mode.',
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Open Settings', onPress: () => Linking.openSettings() }
        ]
      );
    }
  }, [requestPermission]);

  const handleAppStateChange = useCallback((nextAppState) => {
    setIsActive(nextAppState === 'active');
  }, []);

  useEffect(() => {
    if (!hasPermission) {
      requestCameraPermission();
    }
  }, [hasPermission, requestCameraPermission]);

  useEffect(() => {
    const subscription = AppState.addEventListener('change', handleAppStateChange);
    return () => {
      subscription?.remove();
    };
  }, [handleAppStateChange]);

  if (!hasPermission) {
    return (
      <View style={styles.container}>
        <Text style={styles.permissionText}>Requesting camera permission...</Text>
        <TouchableOpacity 
          style={styles.backBtn}
          onPress={() => navigation.goBack()}
        >
          <Text style={styles.backText}>← Back</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (device == null) {
    return (
      <View style={styles.container}>
        <Text style={styles.errorText}>Camera device not available</Text>
        <TouchableOpacity 
          style={styles.backBtn}
          onPress={() => navigation.goBack()}
        >
          <Text style={styles.backText}>← Back</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Camera
        ref={camera}
        style={StyleSheet.absoluteFill}
        device={device}
        isActive={isActive && hasPermission}
        photo={true}
        video={false}
      />

      <View style={styles.overlay}>
        <View style={styles.header}>
          <TouchableOpacity 
            style={styles.backButton}
            onPress={() => navigation.goBack()}
          >
            <Text style={styles.backButtonText}>← Back</Text>
          </TouchableOpacity>
        </View>

        {object && (
          <View style={styles.objectInfo}>
            <Text style={styles.objectLabel}>Finding Object:</Text>
            <Text style={styles.objectName}>{object.name}</Text>
          </View>
        )}

        <View style={styles.footer}>
          <View style={styles.centerMarker}>
            <View style={styles.markerOuter}>
              <View style={styles.markerInner} />
            </View>
          </View>
          <Text style={styles.instructions}>
            Point camera at surroundings to locate object
          </Text>
        </View>
      </View>
=======
import React from "react";
=======
import React, { useState } from "react";
>>>>>>> 2fcb6ff (somedone)
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  NativeModules,
  StatusBar,
  Platform,
  PermissionsAndroid,
  Alert,
  ActivityIndicator
} from "react-native";

const { IntentLauncher } = NativeModules;

export default function ARScreen({ route }) {
  const object = route?.params?.object;
  const [isLoading, setIsLoading] = useState(false);

  /**
   * ✅ FIXED: Proper permission request before launching AR
   */
  const requestCameraPermission = async () => {
    try {
      if (Platform.OS === 'android') {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.CAMERA,
          {
            title: "Camera Permission Required",
            message: "HoloFindX needs camera access to detect objects in AR mode.",
            buttonPositive: "Allow",
            buttonNegative: "Deny",
          }
        );

        console.log("📷 Camera permission result:", granted);
        
        if (granted === PermissionsAndroid.RESULTS.GRANTED) {
          console.log("✅ Camera permission GRANTED");
          return true;
        } else {
          console.log("❌ Camera permission DENIED");
          Alert.alert(
            "Permission Required",
            "Camera permission is required to use AR mode. Please enable it in app settings.",
            [
              { text: "Cancel", style: "cancel" },
              { 
                text: "Open Settings", 
                onPress: () => {
                  // Open app settings
                  if (NativeModules.OpenAppSettingsModule) {
                    NativeModules.OpenAppSettingsModule.openSettings();
                  }
                }
              }
            ]
          );
          return false;
        }
      }
      return true; // iOS doesn't need runtime permission for camera in this context
    } catch (err) {
      console.error("❌ Permission request error:", err);
      return false;
    }
  };

  /**
   * ✅ FIXED: Launch AR with permission check
   */
  const handleStartAR = async () => {
    setIsLoading(true);
    
    try {
      console.log("🚀 Starting AR Activity...");
      console.log("Platform:", Platform.OS);
      console.log("IntentLauncher available:", !!IntentLauncher);

      // 1. Check platform
      if (Platform.OS !== "android") {
        Alert.alert("Not Supported", "AR mode is only available on Android devices.");
        setIsLoading(false);
        return;
      }

      // 2. Check if IntentLauncher module exists
      if (!IntentLauncher) {
        console.error("❌ IntentLauncher module not found!");
        Alert.alert(
          "Error",
          "Native module not found. Please rebuild the app.",
          [{ text: "OK" }]
        );
        setIsLoading(false);
        return;
      }

      // 3. Request camera permission
      const hasPermission = await requestCameraPermission();
      if (!hasPermission) {
        console.log("⚠️ Permission not granted, aborting AR launch");
        setIsLoading(false);
        return;
      }

      // 4. Launch AR Activity
      console.log("🎯 Launching ARActivity...");
      
      if (IntentLauncher.launchARActivity) {
        // Use convenience method if available
        await IntentLauncher.launchARActivity();
      } else if (IntentLauncher.startActivity) {
        // Use generic method
        await IntentLauncher.startActivity("com.holofindx.ar.ARActivity");
      } else {
        throw new Error("No launch method available");
      }

      console.log("✅ AR Activity launched successfully");
      
    } catch (error) {
      console.error("❌ Failed to launch AR:", error);
      Alert.alert(
        "Error",
        `Failed to start AR mode: ${error.message}`,
        [{ text: "OK" }]
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#000" />

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>AR Finder</Text>
        <Text style={styles.subtitle}>
          Augmented Reality Object Detection
        </Text>
      </View>

      {/* Target Object Card */}
      {object && (
        <View style={styles.card}>
          <Text style={styles.cardLabel}>TARGET OBJECT</Text>
          <Text style={styles.objectName}>{object.name}</Text>
        </View>
      )}

      {/* Status Indicator */}
      {isLoading && (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#4f7cff" />
          <Text style={styles.loadingText}>Starting AR Camera...</Text>
        </View>
      )}

      {/* Action Button */}
      <TouchableOpacity 
        style={[styles.button, isLoading && styles.buttonDisabled]} 
        onPress={handleStartAR}
        disabled={isLoading}
      >
        <Text style={styles.buttonText}>
          {isLoading ? "Starting..." : "🎯 Start AR Camera"}
        </Text>
      </TouchableOpacity>

<<<<<<< HEAD
      {/* Footer Hint */}
      <Text style={styles.hint}>
        Move your phone slowly for best results
      </Text>
>>>>>>> 93febe5 (phase-2)
=======
      {/* Footer Hints */}
      <View style={styles.footer}>
        <Text style={styles.hint}>
          📱 Move your phone slowly for best results
        </Text>
        <Text style={styles.hint}>
          💡 Ensure good lighting conditions
        </Text>
      </View>
>>>>>>> 2fcb6ff (somedone)
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
<<<<<<< HEAD
    flex: 1, 
    backgroundColor: "#000",
    alignItems: "center",
    justifyContent: "center"
  },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'space-between',
  },
  header: {
    paddingTop: 50,
    paddingHorizontal: 20,
  },
  backButton: {
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    paddingHorizontal: 15,
    paddingVertical: 10,
    borderRadius: 8,
    alignSelf: 'flex-start',
  },
  backButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  objectInfo: {
    position: 'absolute',
    top: 120,
    left: 20,
    right: 20,
    backgroundColor: 'rgba(79, 124, 255, 0.9)',
    padding: 15,
    borderRadius: 12,
  },
  objectLabel: {
    color: 'rgba(255, 255, 255, 0.8)',
    fontSize: 14,
    marginBottom: 5,
  },
  objectName: {
    color: 'white',
    fontSize: 22,
    fontWeight: 'bold',
  },
  footer: {
    paddingBottom: 80,
    alignItems: 'center',
  },
  centerMarker: {
    alignItems: 'center',
    marginBottom: 20,
  },
  markerOuter: {
    width: 80,
    height: 80,
    borderRadius: 40,
    borderWidth: 3,
    borderColor: 'rgba(79, 124, 255, 0.8)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  markerInner: {
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: 'rgba(79, 124, 255, 0.8)',
  },
  instructions: {
    color: 'white',
    fontSize: 14,
    textAlign: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 8,
  },
  permissionText: {
    color: '#999',
    fontSize: 18,
    textAlign: 'center',
    marginBottom: 20,
  },
  errorText: {
    color: '#ff4444',
    fontSize: 18,
    textAlign: 'center',
    marginBottom: 20,
  },
  backBtn: {
    marginTop: 20,
  },
  backText: {
    color: '#4f7cff',
    fontSize: 18,
=======
    flex: 1,
    backgroundColor: "#000",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: 40,
    paddingHorizontal: 24
  },

  header: {
    alignItems: "center",
    marginTop: 20
  },

  title: {
    color: "#ffffff",
    fontSize: 28,
    fontWeight: "700",
    letterSpacing: 0.5
  },

  subtitle: {
    marginTop: 6,
    color: "#9ca3af",
    fontSize: 14
  },

  card: {
    width: "100%",
    backgroundColor: "#111827",
    borderRadius: 16,
    padding: 20,
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#1f2937"
  },

  cardLabel: {
    color: "#9ca3af",
    fontSize: 12,
    letterSpacing: 1,
    marginBottom: 8
  },

  objectName: {
    color: "#ffffff",
    fontSize: 22,
    fontWeight: "600"
  },

  loadingContainer: {
    alignItems: "center",
    marginVertical: 20
  },

  loadingText: {
    color: "#9ca3af",
    fontSize: 14,
    marginTop: 12
  },

  button: {
    width: "100%",
    backgroundColor: "#4f7cff",
    paddingVertical: 16,
    borderRadius: 14,
    alignItems: "center",
    marginTop: 20,
    shadowColor: "#4f7cff",
    shadowOpacity: 0.4,
    shadowRadius: 10,
    elevation: 8
  },

  buttonDisabled: {
    backgroundColor: "#334155",
    opacity: 0.6
  },

  buttonText: {
    color: "#ffffff",
    fontSize: 18,
    fontWeight: "600"
  },

  footer: {
    alignItems: "center",
    marginBottom: 10
  },

  hint: {
    color: "#6b7280",
    fontSize: 13,
<<<<<<< HEAD
    marginBottom: 10
>>>>>>> 93febe5 (phase-2)
=======
    marginVertical: 4,
    textAlign: "center"
>>>>>>> 2fcb6ff (somedone)
  }
});