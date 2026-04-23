# Anidaku Anime Streaming App: Architecture and Feature Plan

## 1. Introduction

This document outlines the proposed architecture and feature plan for the Anidaku anime streaming application. The goal is to transform the existing basic project into a full-fledged, production-ready application with comprehensive streaming capabilities, leveraging the Miruro API and ExoPlayer.

## 2. Core Technologies

*   **Language:** Kotlin
*   **Framework:** Android (Jetpack Compose for UI)
*   **API Integration:** Ktor HTTP Client with Kotlinx Serialization
*   **Video Playback:** ExoPlayer
*   **Dependency Injection:** Hilt (or Koin)
*   **Asynchronous Operations:** Kotlin Coroutines and Flow
*   **Local Storage:** Room Database (for watchlist, history, downloads)
*   **Version Control & CI/CD:** Git, GitHub Actions

## 3. App Architecture (MVVM-C)

The application will follow a Model-View-ViewModel-Coordinator (MVVM-C) architecture to ensure separation of concerns, testability, and maintainability.

*   **Model:** Represents the data layer, including Miruro API integration, local database, and data models.
    *   `data/api`: Miruro API service, DTOs, and API client.
    *   `data/database`: Room entities, DAOs, and database instance.
    *   `data/repository`: Abstracts data sources (API, DB) and provides a clean API to ViewModels.
    *   `data/models`: Domain models for Anime, Episode, Stream, etc.
*   **View:** The UI layer, composed of composable functions in Jetpack Compose.
    *   `ui/screens`: Individual screens (Home, Search, Detail, Player, etc.).
    *   `ui/components`: Reusable UI elements.
*   **ViewModel:** Exposes data streams to the View and handles UI-related logic, state management, and interaction with repositories.
*   **Coordinator (Navigation):** Manages navigation flow between different screens, decoupling navigation logic from Views and ViewModels.

## 4. Feature Plan

### 4.1. Core Features

*   **Home Screen:**
    *   Display trending, popular, upcoming, and recent anime from Miruro API.
    *   Horizontal carousels for different categories.
    *   Pagination for each category.
*   **Search Functionality:**
    *   Search anime by title using Miruro API.
    *   Autocomplete suggestions as the user types.
    *   Filter options (genre, tag, year, season, format, status, sort).
*   **Browse/Discover:**
    *   Dedicated screen to browse anime by categories, genres, and tags.
    *   Advanced filtering capabilities.
*   **Anime Detail Screen:**
    *   Display comprehensive anime information (description, cover/banner, genres, status, score, episodes, etc.) from `/info/{anilist_id}`.
    *   List episodes for the selected anime, grouped by provider and audio type (sub/dub) from `/episodes/{anilist_id}`.
    *   Display related anime and recommendations.
*   **Video Player (ExoPlayer):**
    *   Seamless playback of HLS/M3U8 streams obtained from `/watch/{episode_id}`.
    *   Basic player controls (play/pause, seek, volume).
    *   Support for subtitles (if available from API).
    *   Skip intro/outro functionality using timestamps from API.
    *   Quality selection for streams.

### 4.2. Advanced Features

*   **Watchlist/Favorites:**
    *   Allow users to add/remove anime to a personal watchlist.
    *   Store watchlist data locally using Room Database.
*   **Watch History:**
    *   Track watched episodes and anime progress.
    *   Resume playback from where the user left off.
    *   Store history data locally using Room Database.
*   **Downloads (Offline Playback):**
    *   Enable users to download episodes for offline viewing.
    *   Manage downloaded content within the app.
    *   Utilize ExoPlayer's download capabilities.
*   **Settings:**
    *   Theme selection (light/dark).
    *   Playback preferences.
    *   Clear cache/data options.
*   **Notifications:**
    *   Notify users about new episodes of anime on their watchlist.

## 5. GitHub Actions for CI/CD

A GitHub Actions workflow will be set up to automate the build and release process.

*   **Workflow Trigger:** Push to `main` branch or manual trigger.
*   **Steps:**
    1.  Checkout code.
    2.  Set up Java Development Kit (JDK).
    3.  Cache Gradle dependencies.
    4.  Run unit tests (if implemented).
    5.  Build debug APK/AAB.
    6.  Build release APK/AAB (signed with provided keystore).
    7.  Upload release artifacts (APK/AAB) to GitHub Releases.
    8.  (Optional) Publish to Google Play Store (requires additional setup).

## 6. Next Steps

1.  Implement core dependencies and project configuration.
2.  Integrate ExoPlayer.
3.  Refactor API service layer and models.
4.  Develop UI screens using Jetpack Compose.
5.  Implement advanced features.
6.  Set up GitHub Actions for CI/CD.
7.  Thorough testing and debugging.
