# PixTidy

<p align="center">
  <strong>An Android app for cleaning, organizing, and managing photos and videos.</strong>
</p>

PixTidy scans the device media library to detect duplicate content, similar photos, blurry images, screenshots, large videos, and documents through OCR. Users can preview, select, delete, or restore relevant items from the in-app trash.

## Features

- Fast, smart, and deep scanning
- Exact duplicate photo detection
- Similar photo detection
- Blurry image detection
- Screenshot grouping
- Large video detection
- Text recognition for image documents (OCR)
- Media preview
- Batch deletion and trash management
- Cleanup recommendations based on library state
- Multiple language support
- Light and dark themes
- Ad configuration, Remote Config, Analytics, and in-app updates

## Technology

- Kotlin
- Android SDK 36 / minimum SDK 28
- Jetpack Compose and Material 3
- Kotlin Coroutines and Flow
- Hilt dependency injection
- Room and DataStore
- Navigation Compose
- Retrofit, OkHttp, Moshi, and Gson
- Coil and Media3
- ML Kit Text Recognition
- Firebase Analytics and Remote Config
- Google AdMob
- Google Play In-App Updates

## Architecture

The project uses a layered architecture with multiple Gradle modules:

| Module | Responsibility |
|---|---|
| `app` | Entry point, Application, MainActivity, navigation setup, and dependency injection |
| `feature` | Compose screens, ViewModels, contracts, and UI components |
| `domain` | Models, use cases, engines, and repository contracts |
| `data` | Room, MediaStore, Remote Config, repository implementations, and image analysis |
| `base` | Themes, shared utilities, permissions, logging, and base UI components |
| `monetization` | Advertising and monetization integration |

Main dependency flow:

```text
app -> feature -> domain
              \-> data -> domain
              \-> base -> domain
              \-> monetization
```

## Requirements

- Android Studio with Android Gradle Plugin 8.13.1 support
- JDK 21
- Android SDK Platform 36
- The Gradle wrapper included in the project
- An Android device or emulator running API 28 or higher

## Configuration

### 1. Clone the project

```bash
git clone https://github.com/nhatnguyen3104/PixTidy.git
cd PixTidy
```

### 2. Configure the Android SDK

Create a `local.properties` file in the project root if Android Studio has not generated it automatically:

```properties
sdk.dir=C:\\Users\\<USERNAME>\\AppData\\Local\\Android\\Sdk
```

### 3. Configure secrets

Create `secrets.properties` in the project root. This file is ignored by Git.

```properties
ID_ADMOB_Dev=<Development AdMob app ID>
ID_ADMOB_Prod=<Production AdMob app ID>
PRIVACY_URL_Prod=<Privacy policy URL>
TERMS_URL_Prod=<Terms of service URL>
```

If these values are not defined, the project uses the AdMob test app ID and default URLs for the privacy policy and terms links.

Do not commit `secrets.properties`, keystores, or credentials.

### 4. Configure Firebase

Add `google-services.json` to the `app/` directory for any environment that requires Firebase Analytics, Firebase Remote Config, or other Firebase services.

This file is ignored by `.gitignore`. Without it, some builds or Firebase features may not initialize correctly.

## Running the project

Open the project in Android Studio, synchronize Gradle, select an Android device or emulator, and run the `PixTidyDevDebug` variant.

You can also build from Windows using the Gradle wrapper:

```bat
gradlew.bat assemblePixTidyDevDebug
gradlew.bat assemblePixTidyProdDebug
```

Install directly on a connected device or emulator:

```bat
gradlew.bat installPixTidyDevDebug
```

Available build variants:

| Product flavor | Build type | Purpose |
|---|---|---|
| `PixTidy` | `devDebug` | Development and debugging |
| `PixTidy` | `devRelease` | Development release testing |
| `PixTidy` | `prodDebug` | Production configuration debugging |
| `PixTidy` | `prodRelease` | Production release |

Variants are created from the build type and product flavor combinations:

- `PixTidyDevDebug`
- `PixTidyDevRelease`
- `PixTidyProdDebug`
- `PixTidyProdRelease`

Build a production APK:

```bat
gradlew.bat assemblePixTidyProdRelease
```

Build outputs are generally located under `app/build/outputs/`.

## Testing

Run unit tests:

```bat
gradlew.bat testPixTidyProdDebugUnitTest
```

Run the full build and lint tasks:

```bat
gradlew.bat build
```

## Android permissions

The app requests access to the device photo and video library:

- `READ_EXTERNAL_STORAGE` on Android 12 and earlier
- `READ_MEDIA_IMAGES` on Android 13 and higher
- `READ_MEDIA_VIDEO` on Android 13 and higher
- `READ_MEDIA_VISUAL_USER_SELECTED` for per-item photo and video access on Android 14+
- `INTERNET` for Analytics, Remote Config, advertising, and updates

The app handles full, partial, and denied permission states and can redirect users to system settings when permission must be granted manually.

## Main project structure

```text
app/          Entry point, navigation, and dependency graph
base/         Utilities, themes, permissions, and base components
data/         Database, MediaStore, and repository implementations
domain/       Use cases, engines, and business models
feature/      Compose screens and ViewModels
monetization/ Advertising and monetization integration
gradle/       Gradle catalog and wrapper
```

## Contributing

1. Create a new branch from the main branch.
2. Keep changes focused and well scoped.
3. Run the relevant build and test tasks.
4. Do not commit secrets, keystores, or local configuration.
5. Open a pull request with a clear description, testing steps, and screenshots for UI changes.

## Repository

- GitHub: https://github.com/nhatnguyen3104/PixTidy

## License

This project does not currently include a LICENSE file. Please contact the repository owner before using or distributing the source code.