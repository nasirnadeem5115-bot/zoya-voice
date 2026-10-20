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
- Settings screen with secure local storage
- Chat history view
- Function calling ready (`openWebsite`, search, reminders...)
- Hinglish + natural expressive personality
- Completely free core features (bring your own keys)

---

## 📱 Tech Stack

| Layer              | Technology                          |
|--------------------|-------------------------------------|
| Language           | Kotlin                              |
| UI                 | Jetpack Compose + Material 3        |
| Architecture       | MVVM + Clean                        |
| Voice              | Gemini Live API (Native Audio)      |
| Audio              | AudioRecord + AudioTrack (PCM 16kHz / 24kHz) |
| DI                 | Hilt / Manual                       |
| Storage            | DataStore Preferences               |
| Min SDK            | 26 (Android 8.0)                    |
| Target SDK         | 35                                  |

---

## 📂 Project Structure

```
app/
├── src/main/
│   ├── java/com/zoya/voice/
│   │   ├── ui/               # Screens & Composables
│   │   ├── voice/            # Gemini Live + Audio engine
│   │   ├── data/             # Settings, Triggers, Repository
│   │   ├── domain/           # Models & UseCases
│   │   └── di/               # Dependency injection
│   ├── res/
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

---

## ⚙️ Getting Started

1. Clone the repo
2. Open in **Android Studio** (Ladybug or newer recommended)
3. Add your Gemini API key in Settings after first launch
4. Run on a real device (microphone required)

```bash
git clone https://github.com/nasirnadeem5115-bot/zoya-voice.git
```

---

## 📝 Personality

Zoya talks like a close girlfriend:
- Flirty, playful, slightly teasing
- Smart & emotionally responsive
- Bold witty one-liners + light sarcasm
- Never robotic, never boring

---

## 🛠 Roadmap

- [x] Project foundation
- [ ] Dark neon UI + Glowing Orb
- [ ] Settings (multi API keys)
- [ ] Gemini Live full integration
- [ ] Interruptions + continuous session
- [ ] Triggers system
- [ ] Function calling tools
- [ ] Chat history
- [ ] APK release

---

**Made with attitude**  
Zoya Voice • 2026