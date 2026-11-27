# 📚 Finebulary

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Version](https://img.shields.io/badge/Version-v2.3.7-blue?style=flat-square)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)

**Finebulary** is a modern, feature-rich vocabulary learning and language mastery application built for Android using Jetpack Compose and Material 3. It combines smart spaced repetition, AI-powered word generation, AI micro-stories, offline idiom dictionaries, IELTS Academy reading passages, **P2P course sharing**, **frozen snapshot duels**, **home screen widgets**, and **gamified streak tracking** to supercharge your language learning journey.

---

## ✨ Key Features (v2.3.7)

- 🧠 **Smart SRS & Review Capping**: Scientific Spaced Repetition System with session capping (20 words) to prevent review fatigue, and weak-word prioritization.
- 📚 **Offline Idioms & Phrasal Verbs Dictionary**: Built-in 100% offline dictionary featuring 150+ common expressions (*e.g., "a load of waffle" -> "boş konuşma"*) with multi-meaning Google Translate lookup for accurate non-literal translations.
- 💾 **100% Complete Backup & Restore**: Full JSON backup and restore covering courses, words, SRS states, IELTS Academy reading passages, test progress, daily streaks, voice API keys, theme palettes, and study targets.
- 📁 **Word Folder Migration & File Association**: Move single words or batch selections across folders/courses with an interactive folder selector dialog. Tap any `.finebulary` file in external apps to open and auto-import instantly.
- 🧙‍♂️ **Academy Mode Wizard & Dynamic AI Generation**: Card-based guided wizard with custom CEFR level pills (`A1`-`C2`), topic suggestions, and dynamic 4-step AI generation progress screen (*Selecting vocabulary -> Drafting passage -> Translating -> Crafting exam questions*).
- 🎙️ **Karaoke Reader & Sentence Shadowing**: Read IELTS articles with live sentence-by-sentence reading highlights, adjustable speed controls (0.75x - 1.5x), and mic-based pronunciation shadowing with accuracy scoring (%).
- 📖 **AI Micro-Stories Generator**: Select words in any folder and let AI generate a short 3-sentence story with **bold target words** and native translations to boost contextual memory.
- ⚔️ **Frozen Snapshot Vocabulary Duels**: Challenge friends using 100% fair immutable duel packets, QR code side-by-side scanning, challenge link sharing, and optional post-duel deck saving.
- 🔥 **Gamification (Daily Streak & Badges)**: Stay motivated with a daily streak counter (🔥) and unlockable achievement badges (*Streak Master*, *Vocab Hero*, *Duel Champion*, *IELTS Scholar*).
- 📱 **"Word of the Day" Home Screen Widget**: Native Android AppWidget displaying daily vocabulary directly on your home screen with tap-to-open integration.
- 🖼️ **Pexels Multi-Image Carousel**: Fetch and choose from up to 4 image options when adding or translating new words.
- 🔊 **Smart Audio & Cloud Fallback**: Automatic, seamless fallback from Cloud TTS (OpenAI, ElevenLabs, Azure, Deepgram) to local device TTS if offline or unconfigured.
- 🤝 **P2P Course & Folder Sharing**: Export any vocabulary folder as a `.finebulary` file and share it with friends via WhatsApp, Telegram, or Email with 1-click import.
- 🔔 **Interactive Actionable Notifications**: Direct quick-action buttons in push notifications (*"🧠 I Remember"* / *"🔄 Keep Practicing"*).
- 🤖 **Multi-Provider AI**: Integrated with Google Gemini, OpenAI, Groq, DeepSeek & OpenRouter APIs for automated word definitions, example sentences, and course generation.
- 🧭 **In-App Feature Tour**: Interactive walkthrough tour guiding first-time users.

---

## 🛠️ Tech Stack & Architecture

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Material 3](https://m3.material.io/)
- **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Local Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- **Preferences**: [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- **Asynchronous Operations**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html) & `StateFlow`
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & Gson & OkHttp
- **Speech Recognition**: Android SpeechRecognizer API
- **Widgets**: Android AppWidgetProvider Framework
- **AI Integration**: Google Generative AI (Gemini SDK), OpenAI, Groq, DeepSeek & OpenRouter

---

## 🔑 Configuration & API Keys

### 🟢 What works WITHOUT API Keys?
Core features work out-of-the-box for free:
- 100% offline Idioms & Phrasal Verbs dictionary lookup
- Free Google Translate & MyMemory translation & dictionary lookups
- Manual vocabulary entry, editing, and folder migration
- Full JSON backup and restore (courses, words, passages, streaks, settings)
- P2P course sharing and importing (`.finebulary` files)
- Rapid-fire frozen snapshot vocabulary duels with QR codes
- Spaced repetition review sessions and daily statistics
- Local Device TTS speech synthesis

### 🤖 What requires API Keys?
To unlock advanced features, configure your keys inside **Settings > Connectivity & AI**:
- **Gemini / OpenAI / Groq / DeepSeek / OpenRouter**: For AI course generation, automated definitions, and AI micro-stories.
- **Pexels**: For fetching high-quality contextual images.
- **OpenAI TTS / ElevenLabs / Azure / Deepgram**: For ultra-realistic cloud voices.

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for more information.
