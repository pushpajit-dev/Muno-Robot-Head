# Muno Robo 🤖

Animated 3D glossy TV-headed robot with reactive LED-matrix faces, bottom sheet customizer, left-side shape dock, bottom emotions carousel, and right-side Sketchware block development studio.

Built for **Android Studio** with ready-to-install debug APK.

---

## 📱 Pre-Built Android APK
The compiled debug APK is ready to install on your Android device or emulator:
- **Location:** `app/build/outputs/apk/debug/app-debug.apk`

### To install via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛠️ Opening in Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose this folder:
   `C:\Users\pushp\.gemini\antigravity\scratch\muno-robo`
3. Android Studio will automatically sync the Gradle project.
4. Click **Run** (`Shift + F10`) to deploy to your connected device or emulator.

---

## ✨ Features & Architecture

### 1. Left-Side Shape Dock
- Live animated mini robot previews for all four shapes:
  - **Rectangle** (Classic TV)
  - **Square**
  - **Circle**
  - **Hexagon**
- Interactive magnification curve and tooltip labels.

### 2. Main Stage & Poking Interaction
- Real-time 2D Canvas shader simulation with physics, glossy reflections, LED matrix, and soft floor shadow.
- Follows touch and mouse pointers with eyes and head tilt.
- **Poke / Tap:** triggers cheerful hop, 360° spin, and spring bounce animation!

### 3. Bottom Emotions Carousel
- Live animated mini-canvases for all 9 states:
  - **Idle**, **Thinking**, **Searching**, **Listening**, **Speaking**, **Working**, **Happy**, **Error**, **Sleeping**.
  - All rendered in real-time in the current shape and custom colors.

### 4. Bottom Sheet View (Properties & Customizer)
- Slide-up bottom sheet overlay with grab handle and peek summary:
  - **Shell Colors:** 10 swatches (Cobalt, Tomato, Tangerine, Mustard, Mint, Teal, Grape, Bubblegum, Graphite, Porcelain) + Custom Color Picker.
  - **Trim Colors:** 5 swatches (Steel, Silver, Brass, Copper, Black) + Custom Color Picker.
  - **Screen Colors:** 5 swatches (White, Ice, Matrix, Amber, Neon) + Custom Color Picker.
  - **Antenna:** Segmented On/Off toggle.
  - **Speed:** 0.5×, 1×, 1.5×, 2× + Play/Pause toggle.
  - **Live Code Snippet:** `<RobotHead ... />` with syntax highlighting and Copy button.
  - **Reset Defaults button.**

### 5. Right-Side Development Studio Drawer (Sketchware & Blocks)
- **Sketchware Blocks & Flow:** Visual interlocking puzzle blocks (Event hat blocks, Action blocks, Setter blocks). Includes **Run Sequence** to execute behavior chains on the live robot.
- **Block Creator & Manager:** Form to create custom blocks. Block library with Run, Share, Rename, Duplicate, and Delete actions.
- **Phase JSON & Assets:** Dynamic JSON schema generator. Includes live JSON parser editor to reconfigure the robot on the fly.
- **Projects & Sessions:** Continuous auto-save of active session. Save, load, rename, and export custom project configurations with built-in presets.
- **Android Code Export:** Ready-to-use Kotlin WebView snippets and Sketchware component specifications.

### 6. Branding & Themes
- Top-left header branding: **Muno Robo** (original credits removed).
- Dark Mode (`#0a0a0b`) and Light Mode (`#f3f3f1`) matching the original aesthetic, persisted across launches.

---

## 💻 Running the Web Development Server
If you want to test or modify the front-end interface in your browser:
```bash
cd web
npm run dev
```
To re-bundle into the Android app:
```bash
cd web
npm run build
# Copy to Android assets:
Copy-Item -Path "dist\*" -Destination "..\app\src\main\assets\www\" -Recurse -Force
```
To re-build the APK:
```bash
.\gradlew.bat assembleDebug
```
