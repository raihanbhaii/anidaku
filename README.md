# AniDaku 🎌

A dark anime streaming app for iPhone X (iOS 16) built with React Native. Powered by the [animepahe-navy API](https://animepahe-navy.vercel.app/).

---

## Features

- 🏠 Home — currently airing anime with infinite scroll
- 🔍 Search — live debounced search
- 📚 Browse — alphabetical anime list
- 📺 Detail — anime info + episode list (sort asc/desc, paginated)
- ▶️ Player — HLS video with quality selector + download links

---

## Project Structure

```
anidaku/
├── App.js
├── index.js
├── app.json
├── package.json
├── ios/
│   └── Podfile
├── src/
│   ├── screens/
│   │   ├── HomeScreen.js
│   │   ├── SearchScreen.js
│   │   ├── BrowseScreen.js
│   │   ├── DetailScreen.js
│   │   └── PlayerScreen.js
│   ├── components/
│   │   └── AnimeCard.js
│   ├── navigation/
│   │   └── AppNavigator.js
│   ├── services/
│   │   └── api.js          ← all API calls go here
│   └── utils/
│       └── theme.js
└── .github/
    └── workflows/
        └── build-ios.yml   ← GitHub Actions IPA build
```

---

## Local Development (requires macOS + Xcode)

```bash
# 1. Clone and install
git clone https://github.com/YOUR_USERNAME/anidaku.git
cd anidaku
npm install

# 2. Install iOS pods
cd ios && pod install && cd ..

# 3. Run on simulator
npx react-native run-ios --simulator "iPhone 14"

# 4. Run on real device (needs Xcode open + device connected)
npx react-native run-ios --device
```

---

## Building the IPA via GitHub Actions

The workflow at `.github/workflows/build-ios.yml` automatically builds an **unsigned IPA** whenever you push to `main`.

### Steps:

1. Push your code to GitHub
2. Go to **Actions** tab → select `Build AniDaku IPA`
3. Wait ~15–20 minutes for the build
4. Download the `.ipa` from the **Artifacts** section

---

## Sideloading with ESign

1. Download `anidaku-unsigned-ipa` from GitHub Actions artifacts
2. Transfer the `.ipa` to your iPhone (AirDrop, Files app, or via a web server)
3. Open **ESign** on your iPhone
4. Go to **App Library** → Import IPA → select `anidaku.ipa`
5. Tap the app → **Sign** → choose your certificate
6. Tap **Install** → Trust the developer profile in:
   `Settings → General → VPN & Device Management → Trust`
7. Open AniDaku 🎌

---

## API Endpoints Used

| Feature | Endpoint |
|---------|---------|
| Airing anime | `GET /api/airing?page=N` |
| Search | `GET /api/search?q=query` |
| Browse list | `GET /api/anime?tab=A` |
| Anime info | `GET /api/:session` |
| Episodes | `GET /api/:session/releases?sort=episode_asc&page=N` |
| Stream links | `GET /api/play/:session?episodeId=xxx` |

> The API base URL is `https://animepahe-navy.vercel.app/api`
> If the public instance is down, fork and deploy your own: [ElijahCodes12345/animepahe-api](https://github.com/ElijahCodes12345/animepahe-api)

---

## Customizing the API URL

Edit `src/services/api.js` line 1:
```js
const BASE_URL = 'https://YOUR_OWN_INSTANCE.vercel.app/api';
```

---

## Disclaimer

This app is for educational purposes. Not affiliated with Animepahe.
