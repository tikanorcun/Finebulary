# 📚 Finebulary

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)

**Finebulary** is a modern, feature-rich vocabulary learning and language mastery application built for Android using Jetpack Compose. It combines smart spaced repetition, AI-powered word generation, contextual reading (IELTS passages), and smart daily reminders to supercharge your language learning journey.

---

## 📥 Download & Install

You can download the latest ready-to-use APK directly from the releases:

👉 **[Download Latest APK from Releases](../../releases)**

1. Go to the [Releases](../../releases) page.
2. Download the `.apk' from the latest release.
3. Install it on your Android device and start learning!

---

## 🧭 App Menus & Navigation

Finebulary is designed with an intuitive and clean Material 3 interface. Here is an overview of the main menus and screens:

- **🏠 Home Screen**: Your learning dashboard. View daily statistics, quick progress summaries, and jump right into your study sessions.
- **🧠 Review Screen**: Spaced repetition review sessions to test your memory, reinforce weak words, and move items into long-term memory.
- **📖 Academy / IELTS Reading**: Curated academic reading passages designed for advanced learners and IELTS preparation, complete with instant vocabulary extraction.
- **🤖 AI Course Generator**: Create custom learning courses powered by artificial intelligence tailored to your specific goals and proficiency level *(Requires API Key)*.
- **⚙️ Settings & API Configuration**: Manage your preferences, backups, and configure your API keys for advanced features.

---

## 🔑 Configuration & API Keys (Unlocking Full Features)

### 🟢 What works WITHOUT API Keys?
Out of the box, you can fully use core features such as:
- Manual vocabulary entry and organization
- Spaced repetition review sessions (`Review Screen`)
- Tracking your daily learning progress and statistics
- Reading local content and app navigation

### 🤖 What requires API Keys?
To unlock advanced features such as **AI-powered course generation**, **automated definitions/example sentences**, and **Pexels contextual images**, you can easily configure your keys inside the app's **Settings > API Keys** menu:

1. **🤖 Gemini API Key / OpenAI API Key**: 
   - **Used for:** AI-powered word definitions, example sentences, synonyms, and custom AI course generation.
   - **How to get:** Obtain a free API key from [Google AI Studio (Gemini)](https://aistudio.google.com/) or OpenAI.
2. **🖼️ Pexels API Key**:
   - **Used for:** Fetching high-quality contextual images for words to enhance visual memory retention.
   - **How to get:** Register a free developer account on [Pexels API](https://www.pexels.com/api/).

*(Alternatively, if building from source, you can add them to your `local.properties` file in the project root: `GEMINI_API_KEY=...`, `OPENAI_API_KEY=...`, `PEXELS_API_KEY=...`)*

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for more information.
