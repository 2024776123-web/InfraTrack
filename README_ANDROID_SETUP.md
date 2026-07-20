# InfraTrack — Android App Setup Guide

This is the **Android mobile app** for the InfraTrack project (rubric item: Mobile Application).
It reads/writes the same Firestore `hazards` collection as your web admin panel, so **use the
same Firebase project** you already set up for the web app.

```
[ Admin Web Panel ]  --writes-->  [ Firebase Firestore ]  --reads-->  [ InfraTrack Android App ]
                                                                       (this project, also writes
                                                                        crowdsourced reports)
```

---

## Step 1 — Connect this app to your Firebase project

1. Go to the [Firebase Console](https://console.firebase.google.com) → open the **same project**
   you used for the web panel (do NOT create a new one — the app must share data with the web app).
2. Click the **gear icon ⚙ → Project settings → Your apps**.
3. Click **Add app → Android**.
   - Android package name: `com.uitm.infratrack` (must match exactly — this is set in
     `app/build.gradle.kts` as `applicationId`).
   - Nickname: `InfraTrack Android` (anything you like).
   - Register the app.
4. Download the **`google-services.json`** file Firebase gives you.
5. Replace the placeholder file at `app/google-services.json` in this project with the one you
   just downloaded.

## Step 2 — Add your Google Maps API key

Follow the same steps your team already used in *Setup_SDK_and_Google_Map.docx*:

1. Open the [Google Cloud Console](https://console.cloud.google.com), signed in with the same
   Google account as Firebase, and select your project.
2. **APIs & Services → Library** → search **"Maps SDK for Android"** → **Enable**.
3. **APIs & Services → Credentials → + Create Credentials → API key**. Copy the key
   (starts with `AIza...`).
4. In the root of this project, create (or edit) a file called **`local.properties`**
   (this file is git-ignored and never shared) and add:
   ```
   MAPS_API_KEY=AIzaSyD_your_actual_key_here
   ```
   The build script (`app/build.gradle.kts`) automatically injects this into the manifest —
   you don't need to edit `AndroidManifest.xml` yourself.

> Reminder from your docx: enabling the Maps SDK usually asks for a billing account even
> though using the map inside the app is free. If your class doesn't have one, use a single
> shared key from your lecturer/teacher account for the assignment, and don't commit it to a
> public GitHub repo.

## Step 3 — Open and run

1. Open this folder (`InfraTrack/`) in **Android Studio** (Hedgehog / 2023.1+ recommended).
2. Let Gradle sync (first sync downloads dependencies — needs internet).
3. Run on an emulator or a physical device with **Google Play services** installed
   (required for Maps + Location).
4. Grant the location permission when prompted, so the Map tab can show your GPS position
   and the Report tab can auto-capture coordinates.

## Step 4 — Test the data flow

1. Add a hazard from your **web admin panel** → it should appear instantly in the Android
   app's **Feed** tab (newest first) and as a marker on the **Map** tab.
2. Submit a report from the Android app's **Report** tab → it should appear instantly in the
   web panel's live feed too, since both write to the same `hazards` collection.

---

## Project structure

```
app/src/main/java/com/uitm/infratrack/
 ├─ MainActivity.kt                  Bottom-nav host (Feed / Map / Report / About)
 ├─ model/Hazard.kt                  Firestore document model (matches web app's schema)
 ├─ adapter/HazardAdapter.kt         RecyclerView adapter for the news feed
 ├─ util/NetworkUtils.kt             Internet-connectivity check
 └─ ui/
     ├─ list/HazardListFragment.kt   News feed: live Firestore listener, newest-first
     ├─ map/HazardMapFragment.kt     Interactive map: dynamic markers + GPS "my location"
     ├─ report/ReportHazardFragment.kt  Crowdsourcing form with GPS auto-capture
     └─ about/AboutFragment.kt       App/group/course info page
```

## Firestore data model (must match the web app exactly)

| Field          | Type              |
|----------------|-------------------|
| `hazardType`   | string            |
| `locationName` | string            |
| `description`  | string            |
| `latitude`     | number            |
| `longitude`    | number            |
| `reporterName` | string            |
| `reportedAt`   | timestamp (auto, server-set) |

## Notes for your report / video demo

- **UI/UX**: navy (`#1E3A5F`) + amber (`#F5A623`) hazard-alert palette, card-based feed,
  connection-status dot (green = connected, red = offline), loading spinners, Toasts on
  submit success/failure.
- **Core functions covered**: interactive map with dynamic markers (`HazardMapFragment`),
  crowdsourcing report submission (`ReportHazardFragment`), GPS integration via
  `FusedLocationProviderClient` (auto-fills latitude/longitude, and shows "my location" blue
  dot on the map).
- Firestore security rules: same test-mode note from the web README applies — fine for a
  class demo, tighten `allow write` before any real deployment.
- You still need to add real **app icons** (currently a simple placeholder vector) and
  polish the **About page GitHub link** (`strings.xml → about_github`) before submission.
