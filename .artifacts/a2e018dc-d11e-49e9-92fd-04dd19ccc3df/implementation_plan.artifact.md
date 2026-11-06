# Örnek Cümle Servisi Değişikliği Planı

Kullanıcının örnek cümleleri hangi kaynaktan (Wiktionary veya Yapay Zeka) çekeceğini belirlemesini sağlayan bir "Örnek Cümle Servisi" dropdown seçimi ekleyeceğiz.

## Önerilen Değişiklikler

### 1. Veri Yapısı Güncellemesi
*   **[UserPreferences.kt](file:///C:/Users/apollo/AndroidStudioProjects/Finebulary/app/src/main/java/com/language/finebulary/data/pref/UserPreferences.kt)**:
    *   `EXAMPLE_SENTENCE_ENABLED_KEY` (boolean) yerine `EXAMPLE_SENTENCE_SOURCE_KEY` (string) eklenecek.
    *   `setExampleSentenceEnabled` yerine `setExampleSentenceSource(source: String)` fonksiyonu eklenecek.

### 2. ViewModel Güncellemesi
*   **[HomeViewModel.kt](file:///C:/Users/apollo/AndroidStudioProjects/Finebulary/app/src/main/java/com/language/finebulary/ui/viewmodel/HomeViewModel.kt)**:
    *   `exampleSentenceEnabled` (StateFlow<Boolean>) yerine `exampleSentenceSource` (StateFlow<String>) eklenecek.
    *   `setExampleSentenceSource(source: String)` fonksiyonu eklenecek.

### 3. Arayüz Güncellemesi
*   **[ApiKeysScreen.kt](file:///C:/Users/apollo/AndroidStudioProjects/Finebulary/app/src/main/java/com/language/finebulary/ui/screens/ApiKeysScreen.kt)**:
    *   `SettingsToggle` yerine `OutlinedCard` tabanlı bir dropdown (menu) yapısı eklenerek kullanıcıya "Wiktionary" ve "Yapay Zeka" seçenekleri sunulacak.

### 4. Mantıksal Geçiş
*   **[TranslationManager.kt](file:///C:/Users/apollo/AndroidStudioProjects/Finebulary/app/src/main/java/com/language/finebulary/util/TranslationManager.kt)**:
    *   `getRichDetails` fonksiyonu, artık boolean `fetchExample` yerine `source: String` parametresini alacak. Eğer "Wiktionary" seçiliyse mevcut `DictionaryAPI` mantığı, "Yapay Zeka" seçiliyse yeni eklenecek AI çağrı mantığı tetiklenecek.

## Doğrulama
- Ayarlar ekranından kaynak seçiminin değiştiğinin ve `DataStore`'a kaydedildiğinin doğrulanması.
- Kelime ekleme sırasında seçilen kaynağa göre örnek cümle çekilip çekilmediğinin (loglar üzerinden) kontrolü.
