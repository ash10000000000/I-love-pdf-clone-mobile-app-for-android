# 🦊 PDFox - Complete PDF Utility App for Android

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)]()
[![Material Design 3](https://img.shields.io/badge/Material%20Design%203-1A73E8?style=for-the-badge)]()
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-green.svg)]()
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34-blue.svg)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **PDFox** is a full-featured native Android PDF utility app that replicates the complete functionality of **ilovepdf.com** — redesigned with Material You (Material Design 3) principles, following Google-style UI patterns similar to Google Drive, Google Docs, and Google Photos.

---

## 📱 Screenshots

| Home Screen | Tool Selection | Processing | Result |
|:-----------:|:--------------:|:----------:|:------:|
| ![Home](docs/screenshots/home.png) | ![Tools](docs/screenshots/tools.png) | ![Process](docs/screenshots/processing.png) | ![Result](docs/screenshots/result.png) |

---

## ✨ Features — 25 Complete PDF Tools

### 🗂️ Organize
| # | Tool | Description |
|---|------|-------------|
| 1 | **Merge PDF** | Combine multiple PDFs into one with drag-to-reorder |
| 2 | **Split PDF** | Extract pages or split by page range |
| 3 | **Remove Pages** | Delete unwanted pages from PDF |
| 4 | **Extract Pages** | Select and extract specific pages |
| 5 | **Organize Pages** | Reorder and rotate individual pages |

### ⚡ Optimize
| # | Tool | Description |
|---|------|-------------|
| 6 | **Compress PDF** | Reduce file size while keeping quality |

### 🔄 Convert (FROM PDF)
| # | Tool | Description |
|---|------|-------------|
| 7 | **PDF to Word** | Convert PDF to editable DOCX |
| 8 | **PDF to PowerPoint** | Convert PDF pages to PPTX slides |
| 9 | **PDF to Excel** | Extract PDF tables to XLSX |
| 10 | **PDF to Image** | Export pages as JPG/PNG (zipped) |
| 11 | **PDF to PDF/A** | Convert to archival PDF/A format |

### 🔄 Convert (TO PDF)
| # | Tool | Description |
|---|------|-------------|
| 12 | **Word to PDF** | Convert DOC/DOCX to PDF |
| 13 | **PowerPoint to PDF** | Convert PPT/PPTX to PDF |
| 14 | **Excel to PDF** | Convert XLS/XLSX to PDF |
| 15 | **Image to PDF** | Convert JPG/PNG/BMP/WEBP to PDF |
| 16 | **HTML to PDF** | Convert web pages to PDF |

### 🔒 Security
| # | Tool | Description |
|---|------|-------------|
| 17 | **Protect PDF** | Add password encryption (128/256-bit) |
| 18 | **Unlock PDF** | Remove password protection |
| 19 | **Sign PDF** | Draw and embed your signature |
| 20 | **Redact PDF** | Permanently remove sensitive information |

### ✏️ Edit
| # | Tool | Description |
|---|------|-------------|
| 21 | **Rotate PDF** | Change page orientation (90°/180°) |
| 22 | **Add Page Numbers** | Number your PDF pages |
| 23 | **Add Watermark** | Overlay text watermark on pages |
| 24 | **Edit Metadata** | Modify PDF title, author, keywords |
| 25 | **Repair PDF** | Fix corrupted PDF files |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Presentation Layer                │
│  ┌───────────┐  ┌───────────┐  ┌─────────────────┐ │
│  │ Fragments │  │  Adapters  │  │  Custom Views    │ │
│  │  (XML UI) │  │(RecyclerView)│  │ (SignatureView) │ │
│  └─────┬─────┘  └───────────┘  └─────────────────┘ │
│        │                                            │
│  ┌─────┴─────────────────────────────────────┐     │
│  │            ViewModels (Hilt)               │     │
│  │  ┌──────────────┐  ┌──────────────────┐   │     │
│  │  │ BaseToolVM   │  │ ToolSpecificVMs  │   │     │
│  │  │ (25 tools)   │  │ (@HiltViewModel) │   │     │
│  │  └──────┬───────┘  └────────┬─────────┘   │     │
│  └─────────┼───────────────────┼─────────────┘     │
├────────────┼───────────────────┼───────────────────┤
│            │      Domain Layer │                   │
│  ┌─────────┴───────────────────┴─────────────┐     │
│  │         ToolProcessor (Factory)            │     │
│  │  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐      │     │
│  │  │Merge│ │Split│ │Comp │ │Conv│ │Sec │ ...│     │
│  │  └────┘ └────┘ └────┘ └────┘ └────┘      │     │
│  └─────────────────┬─────────────────────────┘     │
├────────────────────┼───────────────────────────────┤
│                    │      Data Layer               │
│  ┌─────────────────┴─────────────────────────┐     │
│  │          FileRepository (Hilt)             │     │
│  │  ┌──────────────┐  ┌──────────────────┐   │     │
│  │  │  Room DAO    │  │   FileManager     │   │     │
│  │  │ RecentFiles  │  │  (IO Operations)  │   │     │
│  │  └──────┬───────┘  └────────┬─────────┘   │     │
│  └─────────┼───────────────────┼─────────────┘     │
├─────────────┼───────────────────┼───────────────────┤
│  ┌──────────┴──────┐  ┌────────┴──────────────┐   │
│  │  AppDatabase     │  │  External Storage     │   │
│  │  (Recent Files)  │  │  (PDFox/ folder)      │   │
│  └─────────────────┘  └───────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### Design Patterns
- **MVVM (Model-View-ViewModel)** — Clean separation of concerns
- **Repository Pattern** — Unified data access layer
- **Factory Pattern** — ToolProcessor factory for 25 tools
- **Dependency Injection** — Hilt (Dagger) for all layers
- **Single Activity Architecture** — Jetpack Navigation Component

---

## 🎨 Design System

### Color Palette (Material You)
| Color | Hex | Usage |
|-------|-----|-------|
| 🔵 Primary | `#1A73E8` | Google Blue — Primary actions, navigation |
| 🔴 Secondary | `#EA4335` | Google Red — PDF/danger actions |
| 🟢 Tertiary | `#34A853` | Google Green — Success states |
| 🟡 Warning | `#FBBC04` | Google Yellow — Warnings |
| ⚫ Surface | `#FFFFFF` | Card backgrounds, surfaces |
| ⚪ Background | `#F8F9FA` | App background |

### Category Accent Colors
- 🗂️ **Organize**: `#1A73E8` (Blue)
- ⚡ **Optimize**: `#FBBC04` (Yellow)
- 🔄 **Convert**: `#34A853` (Green)
- 🔒 **Security**: `#EA4335` (Red)
- ✏️ **Edit**: `#9C27B0` (Purple)

### Typography
- **Headings**: Google Sans (fallback: Roboto)
- **Body**: Roboto
- Follows Material 3 type scale

### Shapes
- Cards: **16dp** corner radius
- Buttons: **24dp** (fully rounded)
- Bottom Sheets: **28dp** top corners
- Tool Cards: **12dp** corner radius
- FABs: **16dp** corner radius

---

## 📦 Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin 100% |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 34 (Android 14) |
| **UI** | XML Layouts + ViewBinding |
| **Design** | Material Design 3 (Material You) |
| **Architecture** | MVVM + Repository |
| **DI** | Hilt (Dagger) 2.51.1 |
| **Navigation** | Jetpack Navigation Component |
| **Async** | Kotlin Coroutines + Flow |
| **Local DB** | Room 2.6.1 |
| **PDF Engine** | PdfBox-Android 2.0.27.0 |
| **Office Formats** | Apache POI 5.2.5 |
| **Image Loading** | Coil 2.6.0 |
| **Animations** | Lottie 6.4.0 |
| **Logging** | Timber 5.0.1 |
| **ZIP** | Zip4j 2.11.5 |
| **Build** | Gradle Kotlin DSL |

---

## 📁 Project Structure

```
app/src/main/java/com/pdfox/app/
├── PDfoxApplication.kt              # Application entry point
├── di/                              # Dependency Injection
│   ├── DatabaseModule.kt            # Room providers
│   └── FileModule.kt                # Repository & FileManager
├── data/                            # Data Layer
│   ├── db/
│   │   ├── AppDatabase.kt           # Room database
│   │   ├── RecentFile.kt            # Entity
│   │   └── RecentFileDao.kt         # DAO with Flow
│   └── repository/
│       └── FileRepository.kt        # Unified data access
├── domain/                          # Domain Layer (future use cases)
├── ui/                              # Presentation Layer
│   ├── MainActivity.kt              # Single Activity + Bottom Nav
│   ├── home/
│   │   ├── HomeFragment.kt          # Tool grid + search + chips
│   │   ├── HomeViewModel.kt         # Tools list + filtering
│   │   ├── ToolAdapter.kt           # RecyclerView adapter
│   │   ├── ToolItem.kt              # Data class
│   │   └── ToolSelectionFragment.kt # Category browsing
│   ├── recent/
│   │   ├── RecentFragment.kt        # Recent files list
│   │   ├── RecentViewModel.kt       # Room Flow collection
│   │   └── RecentFileAdapter.kt     # Swipe-to-delete adapter
│   ├── settings/
│   │   └── SettingsFragment.kt      # PreferenceFragmentCompat
│   ├── processing/
│   │   ├── ProcessingFragment.kt    # Processing screen
│   │   └── ToolProcessor.kt         # 25 tool implementations
│   ├── result/
│   │   └── ResultFragment.kt        # Result screen (reusable)
│   ├── viewer/
│   │   ├── PdfViewerFragment.kt     # Built-in PDF viewer
│   │   └── PdfPageAdapter.kt        # Page renderer + zoom
│   └── tools/                       # Individual Tool Screens
│       ├── BaseToolFragment.kt      # Base file picker
│       ├── BaseToolViewModel.kt     # Base processing logic
│       ├── ToolUiState.kt           # Sealed UI state
│       ├── ToolOptionsFragment.kt   # Generic options screen
│       ├── compress/
│       ├── compress/CompressFragment.kt
│       ├── compress/CompressViewModel.kt
│       ├── merge/                   # (5 files)
│       ├── split/                   # (2 files)
│       ├── removepages/             # (3 files)
│       ├── extractpages/            # (3 files)
│       ├── organizepages/           # (5 files)
│       ├── pdftoword/               # (2 files)
│       ├── pdftoppt/                # (2 files)
│       ├── pdftoexcel/              # (2 files)
│       ├── pdftoimage/              # (2 files)
│       ├── pdftopdfa/               # (2 files)
│       ├── wordtopdf/               # (2 files)
│       ├── ppttopdf/                # (2 files)
│       ├── exceltopdf/              # (2 files)
│       ├── imagetopdf/              # (2 files)
│       ├── htmltopdf/               # (2 files)
│       ├── protect/                 # (2 files)
│       ├── unlock/                  # (2 files)
│       ├── sign/                    # (2 files)
│       ├── redact/                  # (2 files)
│       ├── rotate/                  # (2 files)
│       ├── pagenumbers/             # (2 files)
│       ├── watermark/               # (2 files)
│       ├── metadata/                # (2 files)
│       └── repair/                  # (2 files)
└── util/                            # Utilities
    ├── FileManager.kt               # File I/O, thumbnails, conversions
    ├── Extensions.kt                # Kotlin extensions + colors
    └── Result.kt                    # Sealed Result class
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK** 17 or newer
- **Android SDK** 34
- **Kotlin** 1.9.22

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/ash10000000000/I-love-pdf-clone-mobile-app-for-android.git
   cd I-love-pdf-clone-mobile-app-for-android
   ```

2. **Open in Android Studio**
   - File → Open → Select the project directory
   - Wait for Gradle sync to complete

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use **Build → Make Project** in Android Studio (Ctrl+F9 / Cmd+F9)

4. **Run on device/emulator**
   - Connect an Android device (SDK 24+) or start an emulator
   - Click **Run → Run 'app'** (Shift+F10)

### Building from Command Line

```bash
# Debug build
./gradlew assembleDebug

# Release build (with ProGuard)
./gradlew assembleRelease

# Run tests
./gradlew test

# Run lint checks
./gradlew lint
```

---

## 🧩 Key Features Deep-Dive

### 🏠 Home Screen
- **Greeting**: "What would you like to do?"
- **Search Bar**: Real-time tool filtering by name/description
- **Category Chips**: Single-select filtering (All, Organize, Optimize, Convert, Security, Edit)
- **Tool Grid**: 2-column staggered layout with colored accent borders per category
- **25 Tools**: Each with icon, name, and description

### 📁 Recent Files
- **Room Database**: Stores last 20 processed files
- **Thumbnails**: First page rendered via PdfRenderer
- **Swipe-to-Delete**: With undo Snackbar
- **Tap to Preview**: Opens built-in PDF viewer

### ⚙️ Settings
- **Theme Toggle**: Light / Dark / System Default
- **Save Location**: Default output directory picker
- **About**: Version, privacy policy, licenses

### 🔍 Built-in PDF Viewer
- **PdfRenderer**: Native Android rendering
- **Pinch-to-Zoom**: Matrix-based image scaling
- **Page Indicator**: "3 / 12" floating chip
- **Share & Download**: From app bar actions

---

## 🔐 Permissions

| Permission | Max SDK | Purpose |
|------------|---------|---------|
| `READ_EXTERNAL_STORAGE` | 32 | Read PDF files (pre-Android 13) |
| `WRITE_EXTERNAL_STORAGE` | 28 | Save processed files (pre-Android 10) |
| `READ_MEDIA_IMAGES` | - | Read images for Image-to-PDF (Android 13+) |
| `MANAGE_EXTERNAL_STORAGE` | - | Full file access (optional, for advanced features) |

All permissions are requested at runtime using Dexter library. FileProvider is used for sharing output files securely.

---

## 📊 Database Schema

### RecentFile Table
| Column | Type | Description |
|--------|------|-------------|
| `id` | Long (PK, Auto) | Unique identifier |
| `inputFileName` | String | Original file name |
| `inputFilePath` | String? | Source file path |
| `outputFileName` | String | Output file name |
| `outputFilePath` | String | Output file path |
| `toolUsed` | String | Tool identifier (e.g., "merge", "compress") |
| `timestamp` | Long | Processing time (epoch ms) |
| `fileSizeBytes` | Long | Output file size |
| `pageCount` | Int | Number of pages |
| `thumbnailPath` | String? | Cached thumbnail path |
| `outputFormat` | String | Output format (PDF, DOCX, etc.) |

---

## 🛠️ Customization

### Changing Colors
Edit `app/src/main/res/values/colors.xml`:
```xml
<color name="primary">#1A73E8</color>
<color name="secondary">#EA4335</color>
```

### Adding a New Tool
1. Create `NewToolFragment.kt` extending `BaseToolFragment`
2. Create `NewToolViewModel.kt` extending `BaseToolViewModel`
3. Add `ToolProcessor` implementation in `ToolProcessor.kt`
4. Register in `ToolProcessorFactory`
5. Add to `ALL_TOOLS` in `HomeViewModel`
6. Add navigation action in `nav_graph.xml`

---

## 📈 Performance

- **Coroutines**: All file I/O runs on `Dispatchers.IO`
- **ViewBinding**: Zero `findViewById` overhead
- **ListAdapter**: DiffUtil-based efficient RecyclerView updates
- **Coil**: Optimized image loading with caching
- **PdfRenderer**: Native rendering (faster than PDFBox for preview)

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Gradle sync fails | Update Android Studio to latest version |
| Build fails with `kapt` error | Ensure KSP plugin version matches Kotlin version |
| PDFBox crashes | Check ProGuard rules include PDFBox keep rules |
| File picker doesn't show files | Grant storage permissions in device settings |
| Dark theme not applying | Check `AppCompatDelegate.setDefaultNightMode()` in Settings |

---

## 📝 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'feat: Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📧 Contact

- **Repository**: [GitHub](https://github.com/ash10000000000/I-love-pdf-clone-mobile-app-for-android)
- **Issues**: [Bug Reports](https://github.com/ash10000000000/I-love-pdf-clone-mobile-app-for-android/issues)

---

## 🙏 Acknowledgments

- [Apache PDFBox](https://pdfbox.apache.org/) — PDF manipulation engine
- [Apache POI](https://poi.apache.org/) — Office format support
- [Material Design 3](https://m3.material.io/) — Design system
- [Lottie](https://airbnb.design/lottie/) — Animations
- [ilovepdf.com](https://www.ilovepdf.com/) — Feature inspiration

---

<div align="center">
  <strong>Made with ❤️ and ☕</strong><br>
  <sub>Built with Kotlin, Material Design 3, and PdfBox-Android</sub>
</div>
