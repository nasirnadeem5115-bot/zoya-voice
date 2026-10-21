# Zoya Voice 🌟

**Young • Confident • Witty • Sassy**  
Real-time Voice-to-Voice AI Assistant for Android

> Like a close girlfriend who actually listens, teases you, and gets things done.

**Repo:** https://github.com/nasirnadeem5115-bot/zoya-voice

---

## ✅ Current Status (Ready for local test)

| Step | Feature | Status |
|------|---------|--------|
| 1 | Project + Dark neon UI + Navigation | Done |
| 2 | Settings + DataStore (Gemini/Grok/OpenRouter keys) | Done |
| 3 | Gemini Live Audio Engine (mic → Live → speaker) | Done |
| 4 | Build fixes + launcher icons + tool calling (open website) | Done |

---

## 📱 How to Build & Install (Test on your phone)

### Option A – Android Studio (Recommended)

1. Open **Android Studio** (Ladybug / latest)
2. **File → Open** → select the cloned `zoya-voice` folder
3. Wait for Gradle Sync (it will download dependencies)
4. Connect a **real Android phone** (USB debugging ON)
5. Click **Run** (green play button)
6. On first launch:
   - Go to **Settings** tab
   - Paste your **Gemini API Key**
   - Tap **Save Settings**
7. Go back to **Home** → tap the glowing Orb / Mic
8. Allow **Microphone** permission
9. Start talking to Zoya

### Option B – Generate APK manually

```bash
git clone https://github.com/nasirnadeem5115-bot/zoya-voice.git
cd zoya-voice
# Open once in Android Studio so it generates gradlew
# Then:
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

Install the APK on your phone and test.

---

## ⚠️ Important Notes for Testing

- **Real device required** – emulator microphone is limited
- You need a valid **Gemini API key** from [Google AI Studio](https://aistudio.google.com)
- First time the model name may need updating if Google renames the Live model
- Internet + Microphone permission are mandatory

---

## 🛠 Features already in the app

- Dark futuristic UI + animated glowing orb
- Continuous voice session with interruption support
- Settings: Gemini + Grok + OpenRouter keys (saved locally)
- Triggers page (custom phrases)
- Chat page
- Tool calling: open website in browser
- Zoya personality (witty, sassy, flirty girlfriend style)

---

## Next possible steps

- Better VAD / interruption polish
- Foreground service for background voice
- More tools (reminders, search, etc.)
- Release signed APK

---

**Made with attitude**  
Zoya Voice • 2026
