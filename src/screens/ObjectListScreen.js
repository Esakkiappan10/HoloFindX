import React from "react";
import { View, Text, FlatList, TouchableOpacity, StyleSheet } from "react-native";

const dummyObjects = [
  { id: "1", name: "Keys" },
  { id: "2", name: "Wallet" },
  { id: "3", name: "TV Remote" },
];

export default function ObjectListScreen({ navigation }) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Your Objects</Text>

      <FlatList
        data={dummyObjects}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <TouchableOpacity 
            style={styles.objectCard}
            onPress={() => navigation.navigate("AR", { object: item })}
          >
            <Text style={styles.objectText}>{item.name}</Text>
          </TouchableOpacity>
        )}
      />

      <TouchableOpacity 
        style={styles.backBtn}
        onPress={() => navigation.goBack()}
      >
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>
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
  objectCard: {
    backgroundColor: "#1c1c1c",
    padding: 20,
    borderRadius: 10,
    marginBottom: 15
  },
  objectText: {
    color: "white",
    fontSize: 18
  },
  backBtn: {
    marginTop: 20
  },
  backText: {
    color: "#4f7cff",
    fontSize: 18
  }
});
