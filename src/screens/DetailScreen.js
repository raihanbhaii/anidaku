import React, { useEffect, useState, useCallback } from 'react';
import {
  View, Text, StyleSheet, ScrollView, TouchableOpacity,
  Image, ActivityIndicator, FlatList, StatusBar,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/Ionicons';
import { api } from '../services/api';
import { COLORS, FONTS } from '../utils/theme';

export default function DetailScreen({ route, navigation }) {
  const { session, title } = route.params;
  const [info, setInfo] = useState(null);
  const [episodes, setEpisodes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [epLoading, setEpLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [sort, setSort] = useState('episode_asc');

  const loadInfo = useCallback(async () => {
    try {
      const data = await api.getAnimeInfo(session);
      setInfo(data);
    } catch (e) { console.error('Info error:', e); }
  }, [session]);

  const loadEpisodes = useCallback(async (p = 1, reset = false) => {
    setEpLoading(true);
    try {
      const data = await api.getReleases(session, sort, p);
      const eps = data.data || data.results || data || [];
      if (reset) setEpisodes(eps);
      else setEpisodes(prev => [...prev, ...eps]);
      setHasMore(eps.length > 0);
    } catch (e) { console.error('Episodes error:', e); }
    finally { setEpLoading(false); setLoading(false); }
  }, [session, sort]);

  useEffect(() => { loadInfo(); loadEpisodes(1, true); }, []);

  useEffect(() => {
    setPage(1);
    loadEpisodes(1, true);
  }, [sort]);

  const loadMore = () => {
    if (epLoading || !hasMore) return;
    const next = page + 1;
    setPage(next);
    loadEpisodes(next);
  };

  const toggleSort = () => setSort(s => s === 'episode_asc' ? 'episode_desc' : 'episode_asc');

  const renderEpisode = ({ item }) => (
    <TouchableOpacity
      style={styles.epCard}
      onPress={() => navigation.navigate('Player', { session, episodeId: item.session, episodeNum: item.episode, title })}
    >
      <View style={styles.epLeft}>
        <Text style={styles.epNum}>EP {item.episode}</Text>
        {item.snapshot && <Image source={{ uri: item.snapshot }} style={styles.epThumb} />}
      </View>
      <View style={styles.epInfo}>
        <Text style={styles.epTitle} numberOfLines={2}>{item.title || `Episode ${item.episode}`}</Text>
        {item.duration && <Text style={styles.epMeta}>{item.duration}</Text>}
        {item.fansub && <Text style={styles.epMeta}>{item.fansub}</Text>}
      </View>
      <Icon name="play-circle" size={28} color={COLORS.accent} />
    </TouchableOpacity>
  );

  if (loading) {
    return <View style={styles.center}><ActivityIndicator size="large" color={COLORS.accent} /></View>;
  }

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <StatusBar barStyle="light-content" backgroundColor={COLORS.bg} />

      {/* Back header */}
      <View style={styles.topBar}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Icon name="arrow-back" size={22} color={COLORS.text} />
        </TouchableOpacity>
        <Text style={styles.topTitle} numberOfLines={1}>{title}</Text>
        <View style={{ width: 22 }} />
      </View>

      <FlatList
        data={episodes}
        keyExtractor={(item, i) => item.session || String(i)}
        renderItem={renderEpisode}
        onEndReached={loadMore}
        onEndReachedThreshold={0.4}
        ListHeaderComponent={
          <>
            {/* Anime banner + info */}
            {info && (
              <View style={styles.infoSection}>
                {info.cover && (
                  <Image source={{ uri: info.cover }} style={styles.cover} resizeMode="cover" />
                )}
                <View style={styles.infoOverlay}>
                  {info.poster && (
                    <Image source={{ uri: info.poster }} style={styles.poster} resizeMode="cover" />
                  )}
                  <View style={styles.infoText}>
                    <Text style={styles.infoTitle}>{info.title}</Text>
                    <View style={styles.tags}>
                      {info.status && <View style={styles.tag}><Text style={styles.tagText}>{info.status}</Text></View>}
                      {info.type && <View style={styles.tag}><Text style={styles.tagText}>{info.type}</Text></View>}
                      {info.episodes && <View style={styles.tag}><Text style={styles.tagText}>{info.episodes} eps</Text></View>}
                    </View>
                  </View>
                </View>
                {info.synopsis && (
                  <Text style={styles.synopsis} numberOfLines={4}>{info.synopsis}</Text>
                )}
              </View>
            )}
            {/* Episodes header */}
            <View style={styles.epHeader}>
              <Text style={styles.sectionTitle}>Episodes ({episodes.length})</Text>
              <TouchableOpacity style={styles.sortBtn} onPress={toggleSort}>
                <Icon name={sort === 'episode_asc' ? 'arrow-up' : 'arrow-down'} size={14} color={COLORS.accent} />
                <Text style={styles.sortText}>{sort === 'episode_asc' ? 'Oldest' : 'Newest'}</Text>
              </TouchableOpacity>
            </View>
          </>
        }
        ListFooterComponent={epLoading ? <ActivityIndicator color={COLORS.accent} style={{ margin: 16 }} /> : null}
        contentContainerStyle={{ paddingBottom: 32 }}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: COLORS.bg },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: COLORS.bg },
  topBar: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 16, paddingVertical: 12,
  },
  topTitle: { flex: 1, color: COLORS.text, fontFamily: FONTS.bold, fontSize: 16, textAlign: 'center', marginHorizontal: 8 },
  infoSection: { marginBottom: 8 },
  cover: { width: '100%', height: 200 },
  infoOverlay: { flexDirection: 'row', padding: 12, gap: 12, marginTop: -40 },
  poster: { width: 90, height: 130, borderRadius: 10, borderWidth: 2, borderColor: COLORS.accent },
  infoText: { flex: 1, paddingTop: 44 },
  infoTitle: { color: COLORS.text, fontFamily: FONTS.bold, fontSize: 16, marginBottom: 8 },
  tags: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  tag: { backgroundColor: COLORS.accentDim, paddingHorizontal: 8, paddingVertical: 3, borderRadius: 6 },
  tagText: { color: COLORS.accent, fontSize: 11, fontFamily: FONTS.medium },
  synopsis: { color: COLORS.textMuted, fontFamily: FONTS.regular, fontSize: 13, paddingHorizontal: 12, paddingBottom: 12, lineHeight: 20 },
  epHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 10 },
  sectionTitle: { color: COLORS.text, fontFamily: FONTS.bold, fontSize: 16 },
  sortBtn: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  sortText: { color: COLORS.accent, fontFamily: FONTS.medium, fontSize: 13 },
  epCard: {
    flexDirection: 'row', alignItems: 'center', backgroundColor: COLORS.surface,
    marginHorizontal: 12, marginBottom: 8, borderRadius: 12, padding: 10, gap: 10,
  },
  epLeft: { position: 'relative' },
  epThumb: { width: 80, height: 50, borderRadius: 6 },
  epNum: { position: 'absolute', top: 4, left: 4, zIndex: 1, backgroundColor: COLORS.accent, color: '#fff', fontSize: 10, fontFamily: FONTS.bold, paddingHorizontal: 4, paddingVertical: 1, borderRadius: 4 },
  epInfo: { flex: 1 },
  epTitle: { color: COLORS.text, fontFamily: FONTS.medium, fontSize: 13, marginBottom: 4 },
  epMeta: { color: COLORS.textMuted, fontSize: 11, fontFamily: FONTS.regular },
});
