
# BeatFlow

## Overview

BeatFlow is an Android application designed to manage and explore music playlists. The app allows users to sign in with their accounts, browse through different users and their playlists, and manage their own playlists by adding or removing songs.

## Features

- **User Authentication**: Users can sign in using various providers including email, phone, and Google accounts via Firebase Authentication.
- **Playlist Management**: Create, edit, and delete playlists. Add or remove songs from playlists with ease.
- **User Profiles**: View and manage user profiles, including profile images and descriptions.
- **Search Functionality**: Search for other users within the app.
- **Swipe to Refresh**: Refresh data with swipe-to-refresh functionality in user searches.
- **Real-time Database**: Uses Firebase Realtime Database to store user and playlist information.
- **Image Upload**: Users can upload and update their profile and playlist images via Firebase Storage.

## System Requirements

- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34
- **Build Tools Version**: 34.0.0

## Dependencies

- **Firebase**:
  - Firebase Authentication
  - Firebase Realtime Database
  - Firebase Storage
- **Glide**: Image loading and caching library
- **Material Components**: For using modern Android UI components
- **AndroidX Libraries**: Includes AppCompat, ConstraintLayout, and more for backwards compatibility and UI design.
- **SwipeRefreshLayout**: For swipe-to-refresh functionality.

## Setup and Installation

1. **Clone the Repository**:
    ```bash
    git clone https://github.com/your-repo/beatflow.git
    cd beatflow
    ```

2. **Open the Project in Android Studio**:
    - Open Android Studio.
    - Select "Open an existing Android Studio project."
    - Choose the project directory.

3. **Sync the Project**:
    - Android Studio should automatically sync the project. If it doesn't, click on "File" > "Sync Project with Gradle Files."

4. **Firebase Setup**:
    - Ensure you have a Firebase project set up.
    - Download the `google-services.json` file from your Firebase console and place it in the `app/` directory.

5. **Run the Project**:
    - Select your target device/emulator.
    - Click on "Run" or use `Shift + F10` to build and run the project.

## File Structure

- **res/anim**: Animation XML files for fragment transitions.
- **res/drawable**: Image assets and XML files for vector drawables.
- **res/layout**: XML layouts for activities, fragments, and UI components.
- **res/values**: Contains XML files for colors, dimensions, strings, and styles.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request.

## License

This project is licensed under the MIT License. See the LICENSE file for details.
