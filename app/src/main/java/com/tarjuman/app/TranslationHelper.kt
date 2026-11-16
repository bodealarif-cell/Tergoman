package com.tarjuman.app

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

object TranslationHelper {

    private val enToArOptions = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.ARABIC)
        .build()

    private val arToEnOptions = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ARABIC)
        .setTargetLanguage(TranslateLanguage.ENGLISH)
        .build()

    private val enToArTranslator = Translation.getClient(enToArOptions)
    private val arToEnTranslator = Translation.getClient(arToEnOptions)

    fun downloadModelsIfNeeded(onStatusUpdate: (String, Boolean) -> Unit) {
        val conditions = DownloadConditions.Builder().build()
        onStatusUpdate("تحميل حزمة الإنجليزية...", false)
        enToArTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                onStatusUpdate("تحميل حزمة العربية...", false)
                arToEnTranslator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        onStatusUpdate("القاموس جاهز ويعمل بدون إنترنت", true)
                    }
                    .addOnFailureListener {
                        onStatusUpdate("تعذر تحميل القاموس العربي — تحقق من الشبكة", false)
                    }
            }
            .addOnFailureListener {
                onStatusUpdate("تحتاج للإنترنت مرة واحدة لتنزيل القاموس", false)
            }
    }

    fun autoTranslate(
        text: String,
        onResult: (translated: String, directionLabel: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            onError("أدخل نصاً أو امسح الشاشة للترجمة")
            return
        }

        val isArabic = containsArabic(cleanText)
        val translator = if (isArabic) arToEnTranslator else enToArTranslator
        val direction = if (isArabic) "العربية  →  English" else "English  →  العربية"

        translator.translate(cleanText)
            .addOnSuccessListener { translatedText ->
                onResult(translatedText, direction)
            }
            .addOnFailureListener {
                onError("القاموس قيد التجهيز، تأكد من اكتمال التحميل")
            }
    }

    private fun containsArabic(text: String): Boolean {
        for (char in text) {
            if (char in '\u0600'..'\u06FF' || char in '\u0750'..'\u077F') {
                return true
            }
        }
        return false
    }
}
