import React, { useEffect, useState, useCallback } from 'react';
import {
  View, Text, StyleSheet, FlatList, TouchableOpacity,
  Image, ActivityIndicator, RefreshControl, ScrollView, StatusBar,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons as Icon } from '@expo/vector-icons';
import { api } from '../services/api';
import AnimeCard from '../components/AnimeCard';
import { COLORS, FONTS } from '../utils/theme';

export default function HomeScreen({ navigation }) {
  const [airing, setAiring] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [page, setPage] = useState(1);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);

  const loadAiring = useCallback(async (p = 1, reset = false) => {
    try {
      const data = await api.getAiring(p);
      const items = data.data || data.results || data || [];
      if (reset) {
        setAiring(items);
      } else {
        setAiring(prev => [...prev, ...items]);
      }
      setHasMore(items.length > 0);
    } catch (e) {
      console.error('Home load error:', e);
    } finally {
      setLoading(false);
      setRefreshing(false);
      setLoadingMore(false);
    }
  }, []);

  useEffect(() => { loadAiring(1, true); }, []);

  const onRefresh = () => {
    setRefreshing(true);
    setPage(1);
    loadAiring(1, true);
  };

  const loadMore = () => {
    if (loadingMore || !hasMore) return;
    setLoadingMore(true);
    const next = page + 1;
    setPage(next);
    loadAiring(next);
  };

  const renderItem = ({ item }) => (
    <AnimeCard item={item} onPress={() => navigation.navigate('Detail', { session: item.session, title: item.title })} />
  );

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color={COLORS.accent} />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <StatusBar barStyle="light-content" backgroundColor={COLORS.bg} />
      <View style={styles.header}>
        <Text style={styles.logo}>Ani<Text style={styles.logoAccent}>Daku</Text></Text>
        <TouchableOpacity onPress={() => navigation.navigate('Search')}>
          <Icon name="search-outline" size={24} color={COLORS.text} />
        </TouchableOpacity>
      </View>

      <FlatList
        data={airing}
        keyExtractor={(item, i) => item.session || String(i)}
        renderItem={renderItem}
        numColumns={2}
        columnWrapperStyle={styles.row}
        contentContainerStyle={styles.list}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={COLORS.accent} />}
        onEndReached={loadMore}
        onEndReachedThreshold={0.5}
        ListHeaderComponent={
          <Text style={styles.sectionTitle}>Currently Airing</Text>
        }
        ListFooterComponent={loadingMore ? <ActivityIndicator color={COLORS.accent} style={{ marginVertical: 16 }} /> : null}
        ListEmptyComponent={<Text style={styles.empty}>No anime found.</Text>}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.bg },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: COLORS.bg },
  header: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingHorizontal: 16, paddingVertical: 12,
  },
  logo: { fontSize: 24, fontFamily: FONTS.bold, color: COLORS.text },
  logoAccent: { color: COLORS.accent },
  sectionTitle: { fontSize: 18, fontFamily: FONTS.bold, color: COLORS.text, marginLeft: 16, marginBottom: 12, marginTop: 4 },
  row: { paddingHorizontal: 8 },
  list: { paddingBottom: 24 },
  empty: { color: COLORS.textMuted, textAlign: 'center', marginTop: 40, fontFamily: FONTS.regular },
});
