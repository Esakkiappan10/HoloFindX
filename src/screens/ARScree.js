import React from "react";
import { View, Text, StyleSheet } from "react-native";

export default function ARScreen({ route }) {
  const object = route.params?.object;

  return (
    <View style={styles.container}>
      <Text style={styles.label}>
        AR Mode (Coming in Step 2)
      </Text>

      {object && (
        <Text style={styles.objectName}>
          Finding: {object.name}
        </Text>
      )}
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
  label: {
    color: "#999",
    fontSize: 22,
  },
  objectName: {
    marginTop: 20,
    color: "white",
    fontSize: 18
  }
});
