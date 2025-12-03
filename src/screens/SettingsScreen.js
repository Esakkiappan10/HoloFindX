import React from "react";
import { View, Text, StyleSheet } from "react-native";

export default function SettingsScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Settings</Text>
      <Text style={styles.item}>• Theme</Text>
      <Text style={styles.item}>• Permissions</Text>
      <Text style={styles.item}>• About</Text>
    </View>
  );
}

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
    marginBottom: 10
  }
});
