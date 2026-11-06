package com.language.finebulary.domain.usecase

import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class DetectLanguageUseCase @Inject constructor() {
    suspend operator fun invoke(text: String): String? {
        val languageIdentifier = LanguageIdentification.getClient()
        return suspendCancellableCoroutine { continuation ->
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode: String ->
                    if (languageCode == "und") {
                        continuation.resume(null)
                    } else {
                        continuation.resume(languageCode)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }
}
