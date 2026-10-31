# KTDash for Android (unofficial)

An independent Android WebView wrapper for https://ktdash.app/, for Android 8.0 and newer. The launcher icon is the website's own PWA icon. KTDash and its logo belong to their respective owners; this repository is not an official KTDash release.

## Install and update

Download `KTDash.apk` from this repository's [latest release](https://github.com/NamelessHero211/ktdash-android/releases/latest). Open it on your Android phone and confirm installation. Install over your previous version to preserve its local session.

The app checks this repository's latest Android release when opened, at most once every 12 hours. The **Updates** button checks immediately. When a newer version exists, it offers an APK download in your browser. Open that download to install it; Android requires your confirmation. This does not silently install apps in the background.

Website changes appear live without an APK update. This app needs the website and an internet connection. It does not include KTDash's database or offer a self-contained offline edition.

Version 1.3 removes the top Browser button. Cookie persistence runs off the UI thread, automatic update checks wait until the page has loaded, rotation and window resizing preserve the current page, and the loading indicator reserves its space to avoid page jumps. Website rendering and network speed still depend on the device, Android System WebView, and KTDash's servers; performance has not been benchmarked on a phone.

Native controls and the website's cards, buttons, and form fields use orange boxes with black lettering. Version 1.4 uses a deeper burnt orange (#CF5718), with approximately 5:1 contrast against black lettering. Buttons have solid black borders (2 CSS pixels in the web content and 2dp in native controls). The theme is applied inside this app only.

## Automated releases

The Android updates workflow runs for pushes to `main`, manual runs, and an hourly upstream check. A scheduled run builds only when the linked `vjosset/ktdash` **v2** branch changed since the last Android release. It records that upstream commit in the release notes and never executes or bundles upstream code. That branch is deprecated; see its [README](https://github.com/vjosset/ktdash/blob/v2/README.md) and the [current web project](https://github.com/vjosset/ktdash-v4-public).

Before automatic releases can work, add these encrypted GitHub Actions repository secrets:

- `ANDROID_KEYSTORE_BASE64`: the existing app signing keystore encoded as Base64.
- `ANDROID_KEYSTORE_PASSWORD`: the password for that keystore (alias `ktdash`).

The signing key must match the previously installed APK. Never commit the keystore or its password. Without these secrets the workflow skips publishing and reports that setup is incomplete. Once configured, run **Actions → Android updates → Run workflow** to publish the first automated release.

GitHub schedules may run late and public repositories' scheduled workflows can be disabled after 60 days without activity. Re-enable the workflow from Actions if that happens. Checks are hourly, not an immediate webhook from the upstream repository.

## Build locally

Use Java 17, Android SDK 35, and Gradle 8.9. This project uses Android Gradle Plugin 8.7.3 and no third-party runtime libraries. Open this directory in Android Studio or run `gradle assembleDebug`. For a release, set `ANDROID_KEYSTORE_PATH` and `ANDROID_KEYSTORE_PASSWORD`, then run `gradle assembleRelease`. CI assigns increasing version codes based on its run number.

APK signatures, package metadata, alignment, and local Java/DEX compilation have been checked. Runtime login, navigation, uploads, downloads, and update prompts still need on-device verification.


Version 1.5 shows uploaded operative portraits in deployed and reserve roster cards. When an operative has no photo, it shows the roster's kill-team artwork with a visible 'Kill-team artwork' label. Missing team art shows 'No artwork available'. Images load lazily; the app reads roster metadata once per roster visit and does not upload or alter any roster photos. This enables the website's portrait display preference locally in the app.


Version 1.6 makes the background of roster cards tappable as well as their picture/name. It shows immediate Opening roster feedback, suppresses repeated taps to the same pending roster, and falls back to loading that same URL after six seconds if client navigation has stalled. Menu buttons and other controls keep their normal actions. Website/server response time is outside the wrapper's control.

Black lettering in cards, web controls and native buttons is bold. The portrait observer no longer scans the page for every CSS class change.

For missing operative photos, the app uses bundled official miniature references matched to the roster's team and operative type. These are representative models for that type, and may show a different legal weapon build than the roster's chosen loadout. Uploaded photos take priority. Types without a verified reference retain labeled kill-team artwork. Reference images and source links are documented in MODEL-REFERENCES.md. Miniatures and images are copyright Games Workshop; this remains an unofficial app.
