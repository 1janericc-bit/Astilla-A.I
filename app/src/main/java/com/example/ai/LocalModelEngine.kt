package com.example.ai

import java.util.Locale
import kotlin.math.ln
import kotlin.math.max

/**
 * Local AI Model Engine for on-device inference (e.g. MediaPipe / llama.cpp / local RAG).
 * Operates strictly on-device with zero external network connectivity.
 */
class LocalModelEngine {

    companion object {
        val DEFAULT = LocalModelEngine()
    }

    /**
     * Executes local inference on the assembled prompt containing System instructions,
     * Training Document Content, and the User Question.
     */
    fun generate(fullPrompt: String): String {
        val (context, question) = parsePrompt(fullPrompt)

        if (context.isBlank()) {
            return "No training context was provided in the prompt."
        }

        return inferFromContext(question, context)
    }

    private fun parsePrompt(fullPrompt: String): Pair<String, String> {
        val contextMarker = "Training Document Content:"
        val questionMarker = "User Question:"
        val answerMarker = "Answer:"

        val contextStartIndex = fullPrompt.indexOf(contextMarker)
        val questionStartIndex = fullPrompt.indexOf(questionMarker)
        val answerStartIndex = fullPrompt.indexOf(answerMarker)

        val context = if (contextStartIndex != -1 && questionStartIndex != -1) {
            fullPrompt.substring(contextStartIndex + contextMarker.length, questionStartIndex).trim()
        } else {
            ""
        }

        val question = if (questionStartIndex != -1) {
            val endIdx = if (answerStartIndex != -1 && answerStartIndex > questionStartIndex) {
                answerStartIndex
            } else {
                fullPrompt.length
            }
            fullPrompt.substring(questionStartIndex + questionMarker.length, endIdx).trim()
        } else {
            fullPrompt.trim()
        }

        return Pair(context, question)
    }

    private fun inferFromContext(question: String, context: String): String {
        val cleanQuestion = question.trim()
        val lowerQ = cleanQuestion.lowercase(Locale.ROOT)

        val sentences = context
            .replace("\r\n", "\n")
            .split(Regex("(?<=[.!?\\n])\\s+"))
            .map { it.trim() }
            .filter { it.length > 15 }

        if (sentences.isEmpty()) {
            return context.take(400)
        }

        // Check if user is asking for general summary of the learned content
        if (lowerQ.contains("summarize") || lowerQ.contains("summary") || lowerQ.contains("tell me about") || lowerQ.contains("overview")) {
            val topSentences = rankSentences(sentences, context).take(4).map { it.text }
            return topSentences.joinToString(" ")
        }

        // Score sentences against the specific user question
        val questionKeywords = extractKeywords(cleanQuestion)
        if (questionKeywords.isEmpty()) {
            return sentences.take(3).joinToString(" ")
        }

        val scored = sentences.map { sentence ->
            val lowerS = sentence.lowercase(Locale.ROOT)
            var score = 0.0

            for (kw in questionKeywords) {
                if (lowerS.contains(kw)) {
                    score += 3.0
                }
            }

            // Word overlap boost
            val sentenceWords = lowerS.split(Regex("\\W+")).toSet()
            val overlap = sentenceWords.intersect(questionKeywords).size
            score += overlap * 2.0

            ScoredItem(sentence, score)
        }.sortedByDescending { it.score }

        val bestMatches = scored.filter { it.score > 0 }.take(3)
        return if (bestMatches.isNotEmpty()) {
            bestMatches.joinToString("\n\n") { it.text }
        } else {
            // Fallback: provide most salient informative sentence from the training context
            sentences.firstOrNull() ?: context.take(250)
        }
    }

    private fun rankSentences(sentences: List<String>, fullText: String): List<ScoredItem> {
        val words = fullText.lowercase(Locale.ROOT).split(Regex("\\W+")).filter { it.length > 2 }
        val freqMap = mutableMapOf<String, Int>()
        for (w in words) {
            freqMap[w] = (freqMap[w] ?: 0) + 1
        }

        return sentences.map { sentence ->
            val sWords = sentence.lowercase(Locale.ROOT).split(Regex("\\W+")).filter { it.length > 2 }
            var score = 0.0
            for (w in sWords) {
                val f = freqMap[w] ?: 0
                if (f > 0) {
                    score += ln(1.0 + f)
                }
            }
            ScoredItem(sentence, score / max(1, sWords.size))
        }.sortedByDescending { it.score }
    }

    private fun extractKeywords(text: String): Set<String> {
        val stopwords = setOf(
            "the", "is", "at", "which", "on", "a", "an", "and", "or", "in", "for", "to", "what", "how",
            "why", "when", "who", "where", "tell", "me", "about", "can", "you", "does", "do", "of",
            "with", "from", "using", "only", "based", "according"
        )
        return text.lowercase(Locale.ROOT)
            .split(Regex("\\W+"))
            .filter { it.length > 2 && it !in stopwords }
            .toSet()
    }

    private data class ScoredItem(val text: String, val score: Double)
}
