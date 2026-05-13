import React, { useState, useCallback, useRef } from 'react';
import {
  View, Text, StyleSheet, TextInput, FlatList,
  TouchableOpacity, ActivityIndicator, Keyboard,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons as Icon } from '@expo/vector-icons';
import { api } from '../services/api';
import AnimeCard from '../components/AnimeCard';
import { COLORS, FONTS } from '../utils/theme';

export default function SearchScreen({ navigation }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const debounceRef = useRef(null);

  const doSearch = useCallback(async (q) => {
    if (!q.trim()) { setResults([]); setSearched(false); return; }
    setLoading(true);
    setSearched(true);
    try {
      const data = await api.search(q.trim());
      setResults(data.data || data.results || data || []);
    } catch (e) {
      console.error('Search error:', e);
      setResults([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const onChangeText = (text) => {
    setQuery(text);
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => doSearch(text), 500);
  };

  const onSubmit = () => {
    Keyboard.dismiss();
    doSearch(query);
  };

  const renderItem = ({ item }) => (
    <AnimeCard item={item} onPress={() => navigation.navigate('Detail', { session: item.session, title: item.title })} />
  );

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.back}>
          <Icon name="arrow-back" size={22} color={COLORS.text} />
        </TouchableOpacity>
        <View style={styles.inputWrap}>
          <Icon name="search-outline" size={18} color={COLORS.textMuted} style={styles.inputIcon} />
          <TextInput
            style={styles.input}
            placeholder="Search anime..."
            placeholderTextColor={COLORS.textMuted}
            value={query}
            onChangeText={onChangeText}
            onSubmitEditing={onSubmit}
            returnKeyType="search"
            autoFocus
          />
          {query.length > 0 && (
            <TouchableOpacity onPress={() => { setQuery(''); setResults([]); setSearched(false); }}>
              <Icon name="close-circle" size={18} color={COLORS.textMuted} />
            </TouchableOpacity>
          )}
        </View>
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={COLORS.accent} />
        </View>
      ) : (
        <FlatList
          data={results}
          keyExtractor={(item, i) => item.session || String(i)}
          renderItem={renderItem}
          numColumns={2}
          columnWrapperStyle={styles.row}
          contentContainerStyle={styles.list}
          ListEmptyComponent={
            searched && <Text style={styles.empty}>No results for "{query}"</Text>
          }
          keyboardShouldPersistTaps="handled"
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.bg },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingVertical: 10, gap: 10 },
  back: { padding: 4 },
  inputWrap: {
    flex: 1, flexDirection: 'row', alignItems: 'center',
    backgroundColor: COLORS.surface, borderRadius: 12, paddingHorizontal: 12, height: 44,
  },
  inputIcon: { marginRight: 8 },
  input: { flex: 1, color: COLORS.text, fontFamily: FONTS.regular, fontSize: 15 },
  row: { paddingHorizontal: 8 },
  list: { paddingBottom: 24 },
  empty: { color: COLORS.textMuted, textAlign: 'center', marginTop: 40, fontFamily: FONTS.regular },
});
