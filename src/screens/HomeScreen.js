import React from "react";
import { View, Text, TouchableOpacity, StyleSheet } from "react-native";

export default function HomeScreen({ navigation }) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>HOLO-FIND X</Text>
      <Text style={styles.subtitle}>AI + AR Object Finder</Text>

      <TouchableOpacity 
        style={styles.btn} 
        onPress={() => navigation.navigate("Objects")}
      >
        <Text style={styles.btnText}>View Objects</Text>
      </TouchableOpacity>

      <TouchableOpacity 
        style={styles.btnOutline}
        onPress={() => navigation.navigate("AR")}
      >
        <Text style={styles.btnOutlineText}>Open AR Mode</Text>
      </TouchableOpacity>

      <TouchableOpacity 
        style={styles.settings}
        onPress={() => navigation.navigate("Settings")}
      >
        <Text style={styles.settingsText}>Settings ⚙️</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1, 
    backgroundColor: "#0d0d0d",
    alignItems: "center",
    justifyContent: "center",
    padding: 20
  },
  title: {
    color: "white",
    fontSize: 34,
    fontWeight: "bold",
  },
  subtitle: {
    color: "#aaa",
    fontSize: 16,
    marginBottom: 40,
  },
  btn: {
    backgroundColor: "#4f7cff",
    padding: 15,
    width: "80%",
    borderRadius: 12,
    marginBottom: 20
  },
  btnText: {
    textAlign: "center",
    color: "white",
    fontSize: 18
  },
  btnOutline: {
    borderColor: "#4f7cff",
    borderWidth: 2,
    padding: 15,
    width: "80%",
    borderRadius: 12,
  },
  btnOutlineText: {
    textAlign: "center",
    color: "#4f7cff",
    fontSize: 18,
  },
  settings: {
    position: "absolute",
    bottom: 40
  },
  settingsText: {
    color: "#777",
    fontSize: 16
  }
});
