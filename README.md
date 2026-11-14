# 📚 Finebulary

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)

**Finebulary** is a modern, feature-rich vocabulary learning and language mastery application built for Android using Jetpack Compose and Material 3. It combines smart spaced repetition, AI-powered word generation, contextual reading (IELTS passages), **P2P course sharing**, **vocabulary duels**, and **interactive notifications** to supercharge your language learning journey.

---

## ✨ Core Features in v2.3

- 🧠 **Smart Vocabulary Review**: Spaced repetition algorithm to help you review words efficiently and retain them in long-term memory.
- 🤝 **P2P Course & Folder Sharing**: Export any vocabulary folder as a `.finebulary` file and share it with friends via WhatsApp, Telegram, or Email, with 1-click import support on the home screen.
- ⚔️ **Rapid-Fire Vocabulary Duel Mode**: Challenge friends using shared folders in a 60-second timed quiz with shareable challenge score cards.
- 🔔 **Interactive Actionable Notifications**: Direct quick-action buttons in push notifications (*"🧠 I Remember"* / *"🔄 Keep Practicing"*) to mark words as learned without opening the app.
- 🤖 **AI-Powered Learning**: Integrated with Google Gemini, OpenAI, Groq, DeepSeek & OpenRouter APIs for automated word definitions, example sentences, and custom course generation.
- 📖 **Academy & IELTS Reading**: Curated academic reading passages with integrated vocabulary extraction and translation tools.
- 🔊 **Pronunciation Audio & Turtle Mode**: Normal and slow (turtle 🐢) audio preview buttons directly inside the word addition dialog.
- 🧭 **In-App Feature Tour**: Interactive walkthrough tour guiding first-time users right after completing onboarding.

---

## 🛠️ Tech Stack & Architecture

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Material 3](https://m3.material.io/)
- **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Local Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- **Asynchronous Operations**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html) & `StateFlow`
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & Gson
- **AI Integration**: Google Generative AI (Gemini SDK), OpenAI, Groq, DeepSeek & OpenRouter

---

## 🔑 Configuration & API Keys (Unlocking Full Features)

### 🟢 What works WITHOUT API Keys?
Core features work out-of-the-box:
- Manual vocabulary entry and organization
- P2P course sharing and importing (`.finebulary` files)
- Rapid-fire vocabulary duels with friends
- Spaced repetition review sessions and daily statistics

### 🤖 What requires API Keys?
To unlock advanced features such as **AI-powered course generation**, **automated definitions**, and **Pexels images**, configure your keys inside **Settings > API Keys** (each provider includes a step-by-step setup guide):
- **Gemini / OpenAI / Groq / DeepSeek / OpenRouter**: For AI course and example generation.
- **Pexels**: For fetching high-quality contextual images.

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for more information.
