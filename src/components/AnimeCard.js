import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, Dimensions } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { COLORS, FONTS } from '../utils/theme';

const { width } = Dimensions.get('window');
const CARD_WIDTH = (width - 36) / 2;

export default function AnimeCard({ item, onPress }) {
  return (
    <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.8}>
      <View style={styles.thumbWrap}>
        {item.poster || item.image || item.cover ? (
          <Image
            source={{ uri: item.poster || item.image || item.cover }}
            style={styles.thumb}
            resizeMode="cover"
          />
        ) : (
          <View style={[styles.thumb, styles.thumbPlaceholder]}>
            <Icon name="image-outline" size={32} color={COLORS.textMuted} />
          </View>
        )}
        {item.episodes && (
          <View style={styles.epsBadge}>
            <Text style={styles.epsBadgeText}>{item.episodes} eps</Text>
          </View>
        )}
        {item.status === 'Currently Airing' && (
          <View style={styles.airingDot} />
        )}
      </View>
      <View style={styles.info}>
        <Text style={styles.title} numberOfLines={2}>{item.title}</Text>
        {item.season && <Text style={styles.meta}>{item.season}</Text>}
        {item.type && <Text style={styles.meta}>{item.type}</Text>}
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    width: CARD_WIDTH,
    margin: 6,
    borderRadius: 12,
    backgroundColor: COLORS.surface,
    overflow: 'hidden',
  },
  thumbWrap: { position: 'relative' },
  thumb: { width: '100%', height: CARD_WIDTH * 1.4 },
  thumbPlaceholder: { backgroundColor: COLORS.surfaceAlt, justifyContent: 'center', alignItems: 'center' },
  epsBadge: {
    position: 'absolute', bottom: 6, right: 6,
    backgroundColor: 'rgba(0,0,0,0.75)',
    paddingHorizontal: 6, paddingVertical: 2, borderRadius: 6,
  },
  epsBadgeText: { color: '#fff', fontSize: 10, fontFamily: FONTS.medium },
  airingDot: {
    position: 'absolute', top: 8, right: 8,
    width: 8, height: 8, borderRadius: 4, backgroundColor: '#22c55e',
    borderWidth: 1.5, borderColor: '#fff',
  },
  info: { padding: 8 },
  title: { color: COLORS.text, fontFamily: FONTS.medium, fontSize: 12, lineHeight: 16, marginBottom: 4 },
  meta: { color: COLORS.textMuted, fontSize: 11, fontFamily: FONTS.regular },
});
