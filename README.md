# kizQR

Fast QR code scanner and generator without ads, internet and trackers.

Status: in development!

## Features

**Create**
- Content types: text, URL, Wi-Fi (scans connect to the network), contact (vCard 3.0), SMS, phone, geo point
- Live preview, PNG export (2048px) to `Pictures/kizQR`, system share sheet
- Works fully offline — no analytics, no network permission needed

**Style**
- 6 branded presets with original vector logos: Telegram, WhatsApp, VK, Instagram, YouTube, Spotify
- Auto-detect: paste a `t.me/...` link and the Telegram style is suggested
- 5 dot shapes and 4 eye shapes, solid / linear / radial gradient colors, transparent background
- Logo in the center: a brand mark or any photo from your gallery (error correction is raised to level H automatically)
- Adjustable error correction (L/M/Q/H) and quiet zone

**Scan**
- Camera (CameraX + ML Kit, bundled model, works offline) and gallery import
- Smart actions on results: open URL, join Wi-Fi network, dial, send SMS, open map, add contact, or turn the scan into a styled QR

**Extras**
- History of created and scanned codes (persists across restarts)
- Dark theme, Material You dynamic colors (Android 12+)
- Russian UI

## Tech

- Kotlin, **Jetpack Compose + Material 3** (AGP 9 built-in Kotlin), Navigation Compose, MVVM
- QR generation: matrix via **ZXing core**, custom rendering (`QrRenderer`) — dots, eyes, gradients, logos
- Scanning: **ML Kit Barcode Scanning** (bundled) + **CameraX**
- History: JSON file + StateFlow; settings — DataStore
- Brand logos: path data from [Simple Icons](https://github.com/simple-icons/simple-icons) (CC0), normalized for `androidx.core.graphics.PathParser`

## Build

```bash
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/
./gradlew test                 # unit tests for QR payload builders and parsers
```

Requirements: JDK 17+, Android SDK (compileSdk 37, minSdk 24).

## Project layout

```
app/src/main/java/com/kizqr/app/
├── qr/          # QrContent (payload builders), QrStyle, QrRenderer, presets, decoder
├── data/        # history (JSON + StateFlow), settings (DataStore), PNG export
├── ui/
│   ├── create/  # create screen + style editor (bottom sheet)
│   ├── scan/    # scanner (CameraX + ML Kit) and result sheet
│   ├── history/ # history
│   └── settings/# theme & about
└── ui/theme/    # orange Material 3 theme
```

## Licenses & trademarks

The Telegram, WhatsApp, VK, Instagram, YouTube and Spotify logos are trademarks of their respective owners, referenced solely to indicate the purpose of each preset. Glyph path data comes from [Simple Icons](https://github.com/simple-icons/simple-icons) (CC0).
