import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  StatusBar,
} from 'react-native';

export default function SettingsScreen({ navigation }) {
  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#0b0b0b" />
      <View style={styles.header}>
        <Text style={styles.title}>Settings</Text>
        <Text style={styles.subtitle}>
          Customize your HOLO-FIND X experience
        </Text>
      </View>

      <View style={styles.section}>
        <SettingItem title="Theme" description="Dark mode (default)" />
        <SettingItem title="Permissions" description="Camera & AR access" />
        <SettingItem title="About" description="App version & credits" />
      </View>

      <TouchableOpacity
        style={styles.backButton}
        onPress={() => navigation.goBack()}
      >
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>

      <Text style={styles.footer}>HOLO-FIND X • Built with AI + AR</Text>
    </View>
  );
}

function SettingItem({ title, description }) {
  return (
    <TouchableOpacity style={styles.item} activeOpacity={0.8}>
      <View>
        <Text style={styles.itemTitle}>{title}</Text>
        <Text style={styles.itemDesc}>{description}</Text>
      </View>
      <Text style={styles.arrow}>›</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0b0b0b',
    paddingHorizontal: 24,
    paddingTop: 24,
    justifyContent: 'space-between',
  },
  header: {
    marginBottom: 20,
  },
  title: {
    color: '#ffffff',
    fontSize: 28,
    fontWeight: '700',
  },
  subtitle: {
    marginTop: 6,
    color: '#9ca3af',
    fontSize: 14,
  },
  section: {
    marginTop: 10,
  },
  item: {
    backgroundColor: '#111827',
    borderRadius: 16,
    paddingVertical: 18,
    paddingHorizontal: 20,
    marginBottom: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#1f2937',
  },
  itemTitle: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '500',
  },
  itemDesc: {
    color: '#9ca3af',
    fontSize: 13,
    marginTop: 4,
  },
  arrow: {
    color: '#4f7cff',
    fontSize: 22,
    fontWeight: '600',
  },
  backButton: {
    paddingVertical: 14,
    alignItems: 'center',
  },
  backText: {
    color: '#4f7cff',
    fontSize: 16,
  },
  footer: {
    textAlign: 'center',
    color: '#6b7280',
    fontSize: 12,
    marginBottom: 10,
  },
});
