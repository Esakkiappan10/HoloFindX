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
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
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
  }
});
