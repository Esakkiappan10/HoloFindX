import React from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  StatusBar,
} from 'react-native';

const dummyObjects = [
  { id: '1', name: 'Keys' },
  { id: '2', name: 'Wallet' },
  { id: '3', name: 'TV Remote' },
];

export default function ObjectListScreen({ navigation }) {
  const renderItem = ({ item }) => (
    <TouchableOpacity
      style={styles.card}
      activeOpacity={0.85}
      onPress={() => navigation.navigate('AR', { object: item })}
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

      <View style={styles.header}>
        <Text style={styles.title}>Your Objects</Text>
        <Text style={styles.subtitle}>Select an object to locate using AR</Text>
      </View>

      <FlatList
        data={dummyObjects}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
      />

      <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0b0b0b',
    paddingHorizontal: 24,
    paddingTop: 24,
  },
  header: {
    marginBottom: 24,
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
  list: {
    paddingBottom: 20,
  },
  card: {
    backgroundColor: '#111827',
    borderRadius: 16,
    paddingVertical: 20,
    paddingHorizontal: 22,
    marginBottom: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderColor: '#1f2937',
  },
  cardLabel: {
    color: '#9ca3af',
    fontSize: 12,
    letterSpacing: 1,
    marginBottom: 4,
  },
  cardTitle: {
    color: '#ffffff',
    fontSize: 20,
    fontWeight: '600',
  },
  cardArrow: {
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
});
