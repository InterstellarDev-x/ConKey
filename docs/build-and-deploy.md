# Build & deploy

ConKey is an **Expo bare-workflow** app with a custom native Android module. It
cannot run in Expo Go — it must be compiled into an APK.

There are two ways to build: **cloud (EAS)** — no local toolchain needed — and
**local (Gradle)** — needs the Android SDK + JDK.

## Option A — EAS Build (recommended, no JDK/Android Studio)

EAS compiles the APK on Expo's servers and gives you a download link.

```bash
# one-time
npm install -g eas-cli
eas login                      # needs a free expo.dev account

# build the APK
cd ConKey
eas build --platform android --profile preview
```

When it finishes (~10–15 min for the first build), open the returned link on
your Android phone and install the APK.

### Profiles

Defined in [eas.json](../eas.json):

| Profile | Output | Use |
|---|---|---|
| `preview` | **APK** (`assembleRelease`) | Sideload / MVP testing. |
| `production` | **AAB** (app-bundle) | Google Play Store upload. |

### Keystore

On the first build, EAS asks **"Generate a new Android Keystore?"** → answer
**Yes**. EAS manages signing for you; you don't need to handle the keystore
manually.

### Project link

The app is linked to its EAS project via `extra.eas.projectId` and `slug` in
[app.json](../app.json). These must match the project on expo.dev or the build
errors with a slug mismatch.

## Option B — Local build (Gradle)

Requires:

1. **JDK 17** — [adoptium.net](https://adoptium.net) (Temurin 17 LTS).
2. **Android SDK** — via [Android Studio](https://developer.android.com/studio),
   then install **SDK Platform 34** + **Build Tools 34**.
3. Environment variables:
   ```
   JAVA_HOME    = <jdk-17 path>
   ANDROID_HOME = C:\Users\<you>\AppData\Local\Android\Sdk
   ```
   Add `%ANDROID_HOME%\platform-tools` to `PATH`.

Then, with a device connected (USB debugging on) or an emulator running:

```bash
cd ConKey
npx expo run:android              # debug build + install
```

To produce a release APK locally:

```bash
cd ConKey/android
./gradlew assembleRelease
# output: android/app/build/outputs/apk/release/app-release.apk
```

## Installing the APK on a phone

- **From a link (EAS):** open the build URL in the phone browser → download →
  install (allow "install from unknown sources" if prompted).
- **From a file:** copy the APK to the phone and tap it, or
  `adb install app-release.apk`.

## Versioning

Bump these in [app.json](../app.json) before each release:

- `version` — human-facing (e.g. `1.0.1`).
- `android.versionCode` — integer, must increase for each Play Store upload.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `eas.json is not valid … buildType must be one of [apk, app-bundle]` | Use `app-bundle` (not `aab`) for production. |
| `slug … does not match` | Make `slug` in app.json match the EAS project. |
| `An Expo user account is required` | Run `eas login` first. |
| Build fails on the Kotlin module | Confirm `minSdkVersion >= 28` (HID API requirement). |
