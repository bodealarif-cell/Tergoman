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

    fun downloadModelsIfNeeded(onStatusUpdate: (String) -> Unit) {
        val conditions = DownloadConditions.Builder().build()
        onStatusUpdate("جاري تحميل قاموس الإنجليزية -> العربية...")
        enToArTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                onStatusUpdate("جاري تحميل قاموس العربية -> الإنجليزية...")
                arToEnTranslator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        onStatusUpdate("✅ قواميس ترجمان جاهزة وتعمل أوفلاين بالكامل!")
                    }
                    .addOnFailureListener { e ->
                        onStatusUpdate("⚠️ تعذر تحميل القاموس العربي: ${e.localizedMessage}")
                    }
            }
            .addOnFailureListener { e ->
                onStatusUpdate("⚠️ تأكد من الاتصال بالإنترنت لأول مرة: ${e.localizedMessage}")
            }
    }

    /**
     * يكتشف لغة النص تلقائياً (عربي أو إنجليزي) ويترجمه للغة المقابلة
     */
    fun autoTranslate(text: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            onError("لا يوجد نص لترجمته.")
            return
        }

        val isArabic = containsArabic(cleanText)
        val translator = if (isArabic) arToEnTranslator else enToArTranslator

        translator.translate(cleanText)
            .addOnSuccessListener { translatedText ->
                onResult(translatedText)
            }
            .addOnFailureListener { exception ->
                onError("جاري تجهيز القاموس أو حدث خطأ: ${exception.localizedMessage}")
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
