import React from "react";
<<<<<<< HEAD
import { View, Text, StyleSheet } from "react-native";
=======
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  StatusBar
} from "react-native";
>>>>>>> 93febe5 (phase-2)

export default function SettingsScreen({ navigation }) {
  return (
    <View style={styles.container}>
<<<<<<< HEAD
      <Text style={styles.title}>Settings</Text>
      <Text style={styles.item}>• Theme</Text>
      <Text style={styles.item}>• Permissions</Text>
      <Text style={styles.item}>• About</Text>
=======
      <StatusBar barStyle="light-content" backgroundColor="#0b0b0b" />
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>Settings</Text>
        <Text style={styles.subtitle}>
          Customize your HOLO-FIND X experience
        </Text>
      </View>

      {/* Settings Options */}
      <View style={styles.section}>
        <SettingItem title="Theme" description="Dark mode (default)" />
        <SettingItem title="Permissions" description="Camera & AR access" />
        <SettingItem title="About" description="App version & credits" />
      </View>

      {/* Back Button */}
      <TouchableOpacity
        style={styles.backButton}
        onPress={() => navigation.goBack()}
      >
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>

      {/* Footer */}
      <Text style={styles.footer}>
        HOLO-FIND X • Built with AI + AR
      </Text>
>>>>>>> 93febe5 (phase-2)
    </View>
  );
}

<<<<<<< HEAD
const styles = StyleSheet.create({
  container: {
    flex: 1, 
    backgroundColor: "#0d0d0d",
    padding: 20
  },
  title: {
    fontSize: 26,
    color: "white",
    marginBottom: 20
  },
  item: {
    color: "#aaa",
    fontSize: 16,
=======
/* Reusable Item */
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
    backgroundColor: "#0b0b0b",
    paddingHorizontal: 24,
    paddingTop: 24,
    justifyContent: "space-between"
  },

  /* Header */
  header: {
    marginBottom: 20
  },

  title: {
    color: "#ffffff",
    fontSize: 28,
    fontWeight: "700"
  },

  subtitle: {
    marginTop: 6,
    color: "#9ca3af",
    fontSize: 14
  },

  /* Section */
  section: {
    marginTop: 10
  },

  item: {
    backgroundColor: "#111827",
    borderRadius: 16,
    paddingVertical: 18,
    paddingHorizontal: 20,
    marginBottom: 16,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#1f2937"
  },

  itemTitle: {
    color: "#ffffff",
    fontSize: 18,
    fontWeight: "500"
  },

  itemDesc: {
    color: "#9ca3af",
    fontSize: 13,
    marginTop: 4
  },

  arrow: {
    color: "#4f7cff",
    fontSize: 22,
    fontWeight: "600"
  },

  /* Back Button */
  backButton: {
    paddingVertical: 14,
    alignItems: "center"
  },

  backText: {
    color: "#4f7cff",
    fontSize: 16
  },

  /* Footer */
  footer: {
    textAlign: "center",
    color: "#6b7280",
    fontSize: 12,
>>>>>>> 93febe5 (phase-2)
    marginBottom: 10
  }
});
