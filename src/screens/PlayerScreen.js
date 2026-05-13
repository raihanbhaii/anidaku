import React, { useEffect, useState, useRef } from 'react';
import {
  View, Text, StyleSheet, ActivityIndicator,
  TouchableOpacity, StatusBar, ScrollView, Linking,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Video, ResizeMode } from 'expo-av';
import { Ionicons as Icon } from '@expo/vector-icons';
import { api } from '../services/api';
import { COLORS, FONTS } from '../utils/theme';

export default function PlayerScreen({ route, navigation }) {
  const { session, episodeId, episodeNum, title } = route.params;
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedSource, setSelectedSource] = useState(null);
  const [paused, setPaused] = useState(false);
  const [error, setError] = useState(null);
  const [controlsVisible, setControlsVisible] = useState(true);
  const controlsTimer = useRef(null);
  const videoRef = useRef(null);

  useEffect(() => {
    StatusBar.setHidden(true);
    loadStreaming();
    return () => {
      StatusBar.setHidden(false);
      clearTimeout(controlsTimer.current);
    };
  }, []);

  const loadStreaming = async () => {
    try {
      const res = await api.getStreamingLinks(session, episodeId);
      setData(res);
      const sources = res.sources || res.data || [];
      if (sources.length > 0) {
        // prefer 720p or highest quality
        const preferred = sources.find(s => s.quality === '720') || sources[sources.length - 1];
        setSelectedSource(preferred);
      }
    } catch (e) {
      console.error('Streaming error:', e);
      setError('Could not load streaming links. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const showControls = () => {
    setControlsVisible(true);
    clearTimeout(controlsTimer.current);
    controlsTimer.current = setTimeout(() => setControlsVisible(false), 3000);
  };

  const sources = data?.sources || data?.data || [];
  const downloads = data?.downloads || [];

  if (loading) {
    return (
      <View style={styles.center}>
        <StatusBar hidden />
        <ActivityIndicator size="large" color={COLORS.accent} />
        <Text style={styles.loadingText}>Loading stream...</Text>
      </View>
    );
  }

  if (error || !selectedSource) {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="light-content" />
        <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
          <Icon name="arrow-back" size={22} color={COLORS.text} />
        </TouchableOpacity>
        <View style={styles.center}>
          <Icon name="alert-circle-outline" size={48} color={COLORS.accent} />
          <Text style={styles.errorText}>{error || 'No streaming source available'}</Text>
          <TouchableOpacity style={styles.retryBtn} onPress={loadStreaming}>
            <Text style={styles.retryText}>Retry</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <View style={styles.fullscreen}>
      <StatusBar hidden />

      {/* Video Player */}
      <TouchableOpacity activeOpacity={1} style={styles.videoContainer} onPress={showControls}>
        <Video
          ref={videoRef}
          source={{ uri: selectedSource.url }}
          style={styles.video}
          resizeMode="contain"
          paused={paused}
          onLoad={() => setControlsVisible(true)}
          onError={(e) => setError('Playback error. Try another quality.')}
          controls={false}
        />

        {/* Overlay controls */}
        {controlsVisible && (
          <View style={styles.overlay}>
            <View style={styles.overlayTop}>
              <TouchableOpacity onPress={() => navigation.goBack()}>
                <Icon name="arrow-back" size={24} color="#fff" />
              </TouchableOpacity>
              <Text style={styles.overlayTitle} numberOfLines={1}>{title} — EP {episodeNum}</Text>
              <View style={{ width: 24 }} />
            </View>
            <TouchableOpacity style={styles.playPause} onPress={() => setPaused(p => !p)}>
              <Icon name={paused ? 'play' : 'pause'} size={48} color="#fff" />
            </TouchableOpacity>
            <View style={styles.overlayBottom}>
              {/* Quality selector */}
              <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.qualityRow}>
                {sources.map((s, i) => (
                  <TouchableOpacity
                    key={i}
                    style={[styles.qualityBtn, selectedSource?.url === s.url && styles.qualityBtnActive]}
                    onPress={() => { setSelectedSource(s); showControls(); }}
                  >
                    <Text style={[styles.qualityText, selectedSource?.url === s.url && styles.qualityTextActive]}>
                      {s.quality || s.label || `Q${i + 1}`}p
                    </Text>
                  </TouchableOpacity>
                ))}
              </ScrollView>
            </View>
          </View>
        )}
      </TouchableOpacity>

      {/* Download links */}
      {downloads.length > 0 && (
        <ScrollView style={styles.dlSection}>
          <Text style={styles.dlTitle}>Download Links</Text>
          {downloads.map((dl, i) => (
            <TouchableOpacity key={i} style={styles.dlItem} onPress={() => Linking.openURL(dl.url)}>
              <Icon name="download-outline" size={18} color={COLORS.accent} />
              <Text style={styles.dlText}>{dl.quality || dl.label || `Quality ${i + 1}`}</Text>
              <Icon name="open-outline" size={16} color={COLORS.textMuted} />
            </TouchableOpacity>
          ))}
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  fullscreen: { flex: 1, backgroundColor: '#000' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: COLORS.bg },
  container: { flex: 1, backgroundColor: COLORS.bg },
  loadingText: { color: COLORS.textMuted, fontFamily: FONTS.regular, marginTop: 12 },
  errorText: { color: COLORS.text, fontFamily: FONTS.regular, textAlign: 'center', marginTop: 12, marginHorizontal: 24 },
  retryBtn: { marginTop: 20, backgroundColor: COLORS.accent, paddingHorizontal: 24, paddingVertical: 10, borderRadius: 10 },
  retryText: { color: '#fff', fontFamily: FONTS.bold },
  backBtn: { padding: 16 },
  videoContainer: { width: '100%', aspectRatio: 16 / 9, backgroundColor: '#000' },
  video: { flex: 1 },
  overlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'space-between', padding: 16 },
  overlayTop: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  overlayTitle: { flex: 1, color: '#fff', fontFamily: FONTS.bold, fontSize: 14 },
  playPause: { alignSelf: 'center' },
  overlayBottom: {},
  qualityRow: { flexDirection: 'row' },
  qualityBtn: { paddingHorizontal: 12, paddingVertical: 6, marginRight: 8, borderRadius: 6, backgroundColor: 'rgba(255,255,255,0.15)' },
  qualityBtnActive: { backgroundColor: COLORS.accent },
  qualityText: { color: '#fff', fontFamily: FONTS.medium, fontSize: 12 },
  qualityTextActive: { color: '#fff' },
  dlSection: { flex: 1, backgroundColor: COLORS.bg, padding: 16 },
  dlTitle: { color: COLORS.text, fontFamily: FONTS.bold, fontSize: 15, marginBottom: 10 },
  dlItem: { flexDirection: 'row', alignItems: 'center', gap: 10, backgroundColor: COLORS.surface, padding: 12, borderRadius: 10, marginBottom: 8 },
  dlText: { flex: 1, color: COLORS.text, fontFamily: FONTS.medium, fontSize: 14 },
});
