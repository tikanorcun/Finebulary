# 📚 Finebulary

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)

**Finebulary** is a modern, feature-rich vocabulary learning and language mastery application built for Android using Jetpack Compose. It combines smart spaced repetition, AI-powered word generation, contextual reading (IELTS passages), and smart daily reminders to supercharge your language learning journey.

---

## 📥 Download & Install (For Users)

Looking to try the app without building from source? You can download the latest ready-to-use APK directly from the releases:

👉 **[Download Latest APK from Releases](../../releases)**

1. Go to the [Releases](../../releases) page.
2. Download the `app-debug.apk` file from the latest release.
3. Install it on your Android device and start learning!

---

## ✨ Core Features

- 🧠 **Smart Vocabulary Review**: Spaced repetition algorithm to help you review words efficiently and retain them in long-term memory.
- 🤖 **AI-Powered Learning**: Integrated with Google Gemini & OpenAI APIs for automated word definitions, example sentences, synonyms, and contextual learning.
- 📖 **IELTS Reading Passages**: Read curated IELTS passages with integrated vocabulary extraction and translation tools.
- 🔔 **Smart Daily Reminders**: Background WorkManager notifications that prompt you with random words or due review counts throughout the day.
- ⚡ **Quick Add Service**: Instant access to add new words on the go.
- 🔊 **Text-to-Speech**: Native audio pronunciation for every word.

---

## 🛠️ Tech Stack & Architecture (For Developers)

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Material 3](https://m3.material.io/)
- **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Local Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- **Asynchronous Operations**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html) & `StateFlow`
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & Gson
- **AI Integration**: Google Generative AI (Gemini SDK) & OpenAI API

---

## 🚀 Getting Started for Development

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/tikanorcun/Finebulary.git
   ```
2. **Open in Android Studio**:
   - Open Android Studio and select **Open**.
   - Choose the cloned `Finebulary` folder.
3. **Configure API Keys (Optional)**:
   - Create a `local.properties` file in the root directory if it doesn't exist.
   - Add your API keys if you want to use AI features:
     ```properties
     GEMINI_API_KEY=your_gemini_api_key_here
     OPENAI_API_KEY=your_openai_api_key_here
     ```
4. **Build and Run**:
   - Sync project with Gradle files and click **Run** on your emulator or physical device.

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for more information.
