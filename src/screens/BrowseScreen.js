import React, { useState, useEffect } from 'react';
import {
  View, Text, StyleSheet, FlatList,
  TouchableOpacity, ActivityIndicator, ScrollView,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '../services/api';
import AnimeCard from '../components/AnimeCard';
import { COLORS, FONTS } from '../utils/theme';

const TABS = ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','0-9'];

export default function BrowseScreen({ navigation }) {
  const [tab, setTab] = useState('');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadList();
  }, [tab]);

  const loadList = async () => {
    setLoading(true);
    try {
      const data = await api.getAnimeList(tab);
      setItems(data.data || data.results || data || []);
    } catch (e) {
      console.error('Browse error:', e);
    } finally {
      setLoading(false);
    }
  };

  const renderItem = ({ item }) => (
    <AnimeCard
      item={item}
      onPress={() => navigation.navigate('Detail', { session: item.session, title: item.title })}
    />
  );

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <Text style={styles.heading}>Browse Anime</Text>

      {/* Alpha tabs */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.tabsScroll} contentContainerStyle={styles.tabs}>
        <TouchableOpacity
          style={[styles.tabPill, tab === '' && styles.tabPillActive]}
          onPress={() => setTab('')}
        >
          <Text style={[styles.tabText, tab === '' && styles.tabTextActive]}>All</Text>
        </TouchableOpacity>
        {TABS.map(t => (
          <TouchableOpacity
            key={t}
            style={[styles.tabPill, tab === t && styles.tabPillActive]}
            onPress={() => setTab(t)}
          >
            <Text style={[styles.tabText, tab === t && styles.tabTextActive]}>{t}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={COLORS.accent} />
        </View>
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item, i) => item.session || String(i)}
          renderItem={renderItem}
          numColumns={2}
          columnWrapperStyle={styles.row}
          contentContainerStyle={styles.list}
          ListEmptyComponent={<Text style={styles.empty}>No anime found.</Text>}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.bg },
  heading: { color: COLORS.text, fontFamily: FONTS.bold, fontSize: 22, paddingHorizontal: 16, paddingTop: 8, paddingBottom: 12 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  tabsScroll: { maxHeight: 44 },
  tabs: { paddingHorizontal: 12, paddingBottom: 8, gap: 6 },
  tabPill: { paddingHorizontal: 12, paddingVertical: 6, borderRadius: 20, backgroundColor: COLORS.surface, marginRight: 4 },
  tabPillActive: { backgroundColor: COLORS.accent },
  tabText: { color: COLORS.textMuted, fontFamily: FONTS.medium, fontSize: 13 },
  tabTextActive: { color: '#fff' },
  row: { paddingHorizontal: 8 },
  list: { paddingTop: 8, paddingBottom: 24 },
  empty: { color: COLORS.textMuted, textAlign: 'center', marginTop: 40, fontFamily: FONTS.regular },
});
