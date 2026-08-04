# Paper Plane Drift

A cozy, paper-themed endless arcade game for Android, built with **Kotlin + Jetpack Compose**.

Instead of tap-to-flap: **hold to climb, release to dive**. A paper airplane glides through a
desk-world of torn notebook paper, closing scissors, snapping staplers, open windows, and desk
fans — while **wind gusts** push it off course. Read the arrows, counter the wind, and chase
"clean glide" bonuses.

## Features

- Hold-to-climb / release-to-dive one-thumb controls (portrait)
- Procedural endless obstacle generation with a distance-based difficulty curve
- Visualized wind-gust zones (translucent chevron streaks) that apply real physics forces
- 5 obstacle types: torn paper, scissors, stapler, window frames, desk fans
- Distance scoring + "clean glide" bonus for holding steady through gusts
- Crumple crash animation with paper-scrap particle burst and a soft procedural "thud"
- Plane skin shop (milestone, ad-watch and IAP unlocks), Daily Challenge with a shared seeded course
- Local persistence via DataStore (high score, streaks, skins, settings)
- AdMob (rewarded revive, capped interstitials, banners) behind an `AdManager` interface with a mock implementation
- Google Play Billing v6 for Remove Ads and cosmetic skins
- Unit tests for collision, physics/wind forces, scoring, and spawner difficulty scaling

## Project structure

```
app/src/main/java/com/paperfly/paperplanedrift/
  ui/screens/     MainMenu, Gameplay (+ GameOver overlay), SkinShop, Settings, Splash
  ui/components/  GameCanvas (all game rendering), PlanePreview, AdBanner
  ui/             GameViewModel (frame-tick game loop via withFrameNanos)
  domain/         GameConfig, PhysicsEngine, ObstacleSpawner, CollisionDetector, ScoreCalculator, models
  data/           ProgressRepository (DataStore), SkinRepository
  ads/            AdManager interface + AdMobAdManager + MockAdManager + InterstitialPolicy
  billing/        BillingManager (Play Billing v6)
  util/           SoundManager (procedural audio), HapticsManager
```

## Build & run

Requirements: JDK 17, Android SDK (compile SDK 34), min SDK 26.

```bash
./gradlew :app:assembleDebug     # build APK
./gradlew :app:testDebugUnitTest # run unit tests
./gradlew :app:installDebug      # install on a connected device
```

## Privacy Policy

The Play Store privacy policy lives at [`docs/privacy-policy.html`](docs/privacy-policy.html).

Once GitHub Pages is enabled for this repo (Settings → Pages → Deploy from branch `main` / folder `/docs`), the public URL is:

**https://juanbarriospiaggio.github.io/paperfly/privacy-policy.html**

Paste that URL into Play Console → App content → Privacy policy.

Contact email in the policy: `info@vexlo.xo.uk`.

## Setup for release

### 1. Real AdMob IDs

Test IDs are used everywhere. Swap them:

- `app/src/main/AndroidManifest.xml` — replace the `com.google.android.gms.ads.APPLICATION_ID`
  meta-data value with your AdMob **app ID**.
- `ads/AdMobAdManager.kt` — replace `REWARDED_ID`, `INTERSTITIAL_ID`, `BANNER_ID` with your
  real **ad unit IDs**.

To test gameplay without any AdMob at all (e.g. emulator), swap `AdMobAdManager()` for
`MockAdManager()` in `PaperPlaneApp.kt`.

Interstitial cadence (grace runs, frequency) is tuned in `ads/AdManager.kt`
(`InterstitialPolicy`).

### 2. Play Billing

Create these in-app products in Play Console (all one-time, non-consumable):

| Product ID          | Purpose                          |
| ------------------- | -------------------------------- |
| `remove_ads`        | Removes all ads                  |
| `skin_origami_crane`| Unlocks the Origami Crane skin   |
| `skin_pack_all`     | Unlocks every IAP skin           |

Entitlements are granted in `AppContainer` (see `PaperPlaneApp.kt`). The app must be signed
and uploaded to a Play track for billing to work end-to-end.

### 3. Play Games Services (leaderboards & achievements)

The app submits scores to two leaderboards and unlocks nine achievements via
Play Games Services v2. Until configured, every call fails soft (the game plays fine).

1. In Play Console: **Grow > Play Games Services > Setup and management > Configuration**,
   create a game project and link this app.
2. Create two leaderboards ("High Score", "Daily Challenge") and the achievements
   (first flight, score 100/250/500/1000, clean streak 3/5, daily challenge, early crumple).
3. Replace the placeholder IDs in `games/PlayGamesManager.kt` (`Ids` object) and put the
   numeric Games project ID in `res/values/strings.xml` (`game_services_project_id`).

PGS leaderboards automatically track daily / weekly / all-time windows — no reset
logic needed in the app. Scores are submitted in `GameViewModel.reportToPlayGames()`.

### 4. Tuning the difficulty curve

Everything lives in `domain/GameConfig.kt`:

- `GRAVITY`, `CLIMB_ACCEL`, `MAX_FALL_SPEED` — flight feel
- `BASE_FORWARD_SPEED`, `MAX_FORWARD_SPEED`, `SPEED_RAMP_PER_UNIT` — scroll speed and ramp
- `BASE_SPACING`/`MIN_SPACING`, `BASE_GAP_HALF`/`MIN_GAP_HALF` — obstacle density and gap size
- `BASE_GUST_STRENGTH`/`MAX_GUST_STRENGTH`, `BASE_GUST_CHANCE`/`MAX_GUST_CHANCE` — wind
- `DIFFICULTY_FULL_DISTANCE` — how fast the whole curve ramps (bigger = gentler)
- `CLEAN_GLIDE_TOLERANCE`, `CLEAN_GLIDE_BONUS` — bonus scoring

### 5. Adding a new plane skin

Append one entry to `SkinRepository.skins` in `data/SkinRepository.kt`:

```kotlin
PlaneSkin("neon", "Neon Fold", 0xFFB2FF59, 0xFF76CC33, 0xFF33691E, UnlockMethod.Milestone(750))
```

The shop grid, unlock logic, previews, and in-game renderer pick it up automatically.
Unlock methods: `Free`, `Milestone(score)`, `AdWatch`, or `Iap(productId)` (also add the
product in Play Console and map it in `AppContainer` if it's a new IAP).
