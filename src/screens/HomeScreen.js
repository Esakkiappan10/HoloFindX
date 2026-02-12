import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, StatusBar } from 'react-native';

export default function HomeScreen({ navigation }) {
  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#0b0b0b" />

      <View style={styles.header}>
        <Text style={styles.title}>HOLO-FIND X</Text>
        <Text style={styles.subtitle}>AI • AR • Smart Object Finder</Text>
      </View>

      <View style={styles.actions}>
        <TouchableOpacity
          style={styles.primaryBtn}
          activeOpacity={0.85}
          onPress={() => navigation.navigate('Objects')}
        >
          <Text style={styles.primaryText}>View Objects</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.secondaryBtn}
          activeOpacity={0.85}
          onPress={() => navigation.navigate('AR')}
        >
          <Text style={styles.secondaryText}>Open AR Mode</Text>
        </TouchableOpacity>
      </View>

      <TouchableOpacity
        style={styles.settings}
        onPress={() => navigation.navigate('Settings')}
      >
        <Text style={styles.settingsText}>Settings ⚙️</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0b0b0b',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 48,
    paddingHorizontal: 24,
  },
  header: {
    alignItems: 'center',
    marginTop: 40,
  },
  title: {
    color: '#ffffff',
    fontSize: 36,
    fontWeight: '800',
    letterSpacing: 1,
  },
  subtitle: {
    marginTop: 8,
    color: '#9ca3af',
    fontSize: 14,
    letterSpacing: 0.5,
  },
  actions: {
    width: '100%',
    alignItems: 'center',
  },
  primaryBtn: {
    width: '100%',
    backgroundColor: '#4f7cff',
    paddingVertical: 18,
    borderRadius: 16,
    alignItems: 'center',
    marginBottom: 20,
    shadowColor: '#4f7cff',
    shadowOpacity: 0.45,
    shadowRadius: 14,
    elevation: 10,
  },
  primaryText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
  },
  secondaryBtn: {
    width: '100%',
    borderWidth: 1.8,
    borderColor: '#4f7cff',
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
  },
  secondaryText: {
    color: '#4f7cff',
    fontSize: 17,
    fontWeight: '500',
  },
  settings: {
    marginBottom: 10,
  },
  settingsText: {
    color: '#6b7280',
    fontSize: 14,
  },
});
