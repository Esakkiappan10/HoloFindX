import React from "react";
<<<<<<< HEAD
import { View, Text, FlatList, TouchableOpacity, StyleSheet } from "react-native";
=======
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  StatusBar
} from "react-native";
>>>>>>> 93febe5 (phase-2)

const dummyObjects = [
  { id: "1", name: "Keys" },
  { id: "2", name: "Wallet" },
<<<<<<< HEAD
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
=======
  { id: "3", name: "TV Remote" }
];

export default function ObjectListScreen({ navigation }) {
  const renderItem = ({ item }) => (
    <TouchableOpacity
      style={styles.card}
      activeOpacity={0.85}
      onPress={() => navigation.navigate("AR", { object: item })}
    >
      <View>
        <Text style={styles.cardLabel}>OBJECT</Text>
        <Text style={styles.cardTitle}>{item.name}</Text>
      </View>

      <Text style={styles.cardArrow}>→</Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#0b0b0b" />

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>Your Objects</Text>
        <Text style={styles.subtitle}>
          Select an object to locate using AR
        </Text>
      </View>

      {/* Object List */}
      <FlatList
        data={dummyObjects}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
      />

      {/* Back Button */}
      <TouchableOpacity
        style={styles.backButton}
>>>>>>> 93febe5 (phase-2)
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
<<<<<<< HEAD
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
=======
    backgroundColor: "#0b0b0b",
    paddingHorizontal: 24,
    paddingTop: 24
  },

  /* Header */
  header: {
    marginBottom: 24
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

  /* List */
  list: {
    paddingBottom: 20
  },

  card: {
    backgroundColor: "#111827",
    borderRadius: 16,
    paddingVertical: 20,
    paddingHorizontal: 22,
    marginBottom: 16,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    borderWidth: 1,
    borderColor: "#1f2937"
  },

  cardLabel: {
    color: "#9ca3af",
    fontSize: 12,
    letterSpacing: 1,
    marginBottom: 4
  },

  cardTitle: {
    color: "#ffffff",
    fontSize: 20,
    fontWeight: "600"
  },

  cardArrow: {
    color: "#4f7cff",
    fontSize: 22,
    fontWeight: "600"
  },

  /* Back */
  backButton: {
    paddingVertical: 14,
    alignItems: "center"
  },

  backText: {
    color: "#4f7cff",
    fontSize: 16
>>>>>>> 93febe5 (phase-2)
  }
});
