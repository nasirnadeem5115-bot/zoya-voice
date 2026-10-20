# Zoya Voice 🌟

**Young • Confident • Witty • Sassy**  
Real-time Voice-to-Voice AI Assistant for Android

> Like a close girlfriend who actually listens, teases you, and gets things done.

---

## 🚀 Features

- **Pure Voice-to-Voice** using Gemini Live API (audio-in → audio-out)
- Continuous conversation + smart interruption handling
- Stunning dark futuristic UI with animated glowing orb
- Real-time waveform visualizer
- Custom **Triggers** (phrase → action)
- Multi-provider API keys: **Gemini** + **Grok (xAI)** + **OpenRouter**
- Settings screen with secure local storage (DataStore)
- Chat history view
- Function calling ready (`openWebsite`, etc.)
- Hinglish + natural expressive personality
- Completely free core features (bring your own keys)

---

## 📱 Tech Stack

| Layer              | Technology                          |
|--------------------|-------------------------------------|
| Language           | Kotlin                              |
| UI                 | Jetpack Compose + Material 3        |
| Voice Engine       | Custom Gemini Live WebSocket client |
| Audio Capture      | AudioRecord (PCM 16 kHz mono)       |
| Audio Playback     | AudioTrack (PCM 24 kHz)             |
| Storage            | DataStore Preferences               |
| Network            | OkHttp WebSocket                    |
| Min SDK            | 26                                  |
| Target SDK         | 35                                  |

---

## 📂 Current Progress

- [x] Project foundation + dark neon UI
- [x] Bottom navigation (Home / Chat / Triggers / Settings)
- [x] Glowing Orb + Waveform
- [x] Settings with multi API keys + DataStore
- [x] Triggers system with persistence
- [x] **Gemini Live Audio Engine** (Step 3)
- [ ] Advanced interruptions + background service
- [ ] Full tool calling (open browser, reminders…)
- [ ] APK release build

---

## ⚙️ How to Run

1. Clone the repo
2. Open in **Android Studio**
3. Sync Gradle
4. Run on a **real device** (emulator mic is limited)
5. Go to **Settings** → paste your **Gemini API Key** → Save
6. Back to Home → tap the Orb / Mic → allow microphone permission

```bash
git clone https://github.com/nasirnadeem5115-bot/zoya-voice.git
```

---

## 📝 Personality

Zoya talks like a close girlfriend:
- Flirty, playful, slightly teasing
- Smart & emotionally responsive
- Bold witty one-liners + light sarcasm
- Never robotic

---

**Made with attitude**  
Zoya Voice • 2026