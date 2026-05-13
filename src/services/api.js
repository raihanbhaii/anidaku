const BASE_URL = 'https://animepahe-navy.vercel.app/api';

async function fetchJSON(path) {
  const res = await fetch(`${BASE_URL}${path}`);
  if (!res.ok) throw new Error(`API error: ${res.status}`);
  return res.json();
}

export const api = {
  // GET /api/airing?page=1
  getAiring: (page = 1) => fetchJSON(`/airing?page=${page}`),

  // GET /api/search?q=query
  search: (q, page = 1) => fetchJSON(`/search?q=${encodeURIComponent(q)}&page=${page}`),

  // GET /api/anime  or  /api/anime?tab=A
  getAnimeList: (tab = '') => fetchJSON(`/anime${tab ? `?tab=${tab}` : ''}`),

  // GET /api/:session
  getAnimeInfo: (session) => fetchJSON(`/${session}`),

  // GET /api/:session/releases?sort=episode_desc&page=1
  getReleases: (session, sort = 'episode_desc', page = 1) =>
    fetchJSON(`/${session}/releases?sort=${sort}&page=${page}`),

  // GET /api/play/:session?episodeId=xxx
  getStreamingLinks: (session, episodeId, withDownloads = false) =>
    fetchJSON(`/play/${session}?episodeId=${episodeId}&downloads=${withDownloads}`),

  // GET /api/queue
  getQueue: () => fetchJSON('/queue'),
};
