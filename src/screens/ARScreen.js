import React, { useState } from 'react';
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
  ActivityIndicator,
  Linking,
} from 'react-native';

const { IntentLauncher } = NativeModules;

export default function ARScreen({ route }) {
  const object = route?.params?.object;
  const [isLoading, setIsLoading] = useState(false);

  const requestCameraPermission = async () => {
    if (Platform.OS !== 'android') {
      return true;
    }

    try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.CAMERA,
        {
          title: 'Camera Permission Required',
          message: 'HoloFindX needs camera access to detect objects in AR mode.',
          buttonPositive: 'Allow',
          buttonNegative: 'Deny',
        },
      );

      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        return true;
      }

      Alert.alert(
        'Permission Required',
        'Camera permission is required to use AR mode. Please enable it in app settings.',
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Open Settings', onPress: () => Linking.openSettings() },
        ],
      );
    } catch (error) {
      Alert.alert('Error', 'Unable to request camera permission.');
    }

    return false;
  };

  const handleStartAR = async () => {
    setIsLoading(true);

    try {
      if (Platform.OS !== 'android') {
        Alert.alert('Not Supported', 'AR mode is only available on Android devices.');
        return;
      }

      if (!IntentLauncher) {
        Alert.alert('Error', 'Native module not found. Please rebuild the app.');
        return;
      }

      const hasPermission = await requestCameraPermission();
      if (!hasPermission) {
        return;
      }

      if (IntentLauncher.launchARActivity) {
        await IntentLauncher.launchARActivity();
        return;
      }

      if (IntentLauncher.startActivity) {
        await IntentLauncher.startActivity('com.holofindx.ar.ARActivity');
        return;
      }

      throw new Error('No launch method available');
    } catch (error) {
      Alert.alert('Error', `Failed to start AR mode: ${error.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#000" />

      <View style={styles.header}>
        <Text style={styles.title}>AR Finder</Text>
        <Text style={styles.subtitle}>Augmented Reality Object Detection</Text>
      </View>

      {object && (
        <View style={styles.card}>
          <Text style={styles.cardLabel}>TARGET OBJECT</Text>
          <Text style={styles.objectName}>{object.name}</Text>
        </View>
      )}

      {isLoading && (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#4f7cff" />
          <Text style={styles.loadingText}>Starting AR Camera...</Text>
        </View>
      )}

      <TouchableOpacity
        style={[styles.button, isLoading && styles.buttonDisabled]}
        onPress={handleStartAR}
        disabled={isLoading}
      >
        <Text style={styles.buttonText}>
          {isLoading ? 'Starting...' : '🎯 Start AR Camera'}
        </Text>
      </TouchableOpacity>

      <View style={styles.footer}>
        <Text style={styles.hint}>📱 Move your phone slowly for best results</Text>
        <Text style={styles.hint}>💡 Ensure good lighting conditions</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 40,
    paddingHorizontal: 24,
  },
  header: {
    alignItems: 'center',
    marginTop: 20,
  },
  title: {
    color: '#ffffff',
    fontSize: 28,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  subtitle: {
    marginTop: 6,
    color: '#9ca3af',
    fontSize: 14,
  },
  card: {
    width: '100%',
    backgroundColor: '#111827',
    borderRadius: 16,
    padding: 20,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#1f2937',
  },
  cardLabel: {
    color: '#9ca3af',
    fontSize: 12,
    letterSpacing: 1,
    marginBottom: 8,
  },
  objectName: {
    color: '#ffffff',
    fontSize: 22,
    fontWeight: '600',
  },
  loadingContainer: {
    alignItems: 'center',
    marginVertical: 20,
  },
  loadingText: {
    color: '#9ca3af',
    fontSize: 14,
    marginTop: 12,
  },
  button: {
    width: '100%',
    backgroundColor: '#4f7cff',
    paddingVertical: 16,
    borderRadius: 14,
    alignItems: 'center',
    marginTop: 20,
    shadowColor: '#4f7cff',
    shadowOpacity: 0.4,
    shadowRadius: 10,
    elevation: 8,
  },
  buttonDisabled: {
    backgroundColor: '#334155',
    opacity: 0.6,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
  footer: {
    alignItems: 'center',
    marginBottom: 10,
  },
  hint: {
    color: '#6b7280',
    fontSize: 13,
    marginVertical: 4,
    textAlign: 'center',
  },
});
