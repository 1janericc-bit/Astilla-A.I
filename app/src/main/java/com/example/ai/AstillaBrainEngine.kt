package com.example.ai

import com.example.data.local.dao.KnowledgeDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.entity.KnowledgeCapsuleEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.data.model.SummaryMode
import com.example.data.model.TrainingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.ln
import kotlin.math.min

object AstillaBrainEngine {

    const val MAYA_NUMBER = "09273352516"
    const val GCASH_NUMBER = "09193710317"
    const val DEVELOPER_NAME = "Astilla Softwares"
    const val APP_NAME = "Astilla A.I"

    val localModelEngine = LocalModelEngine.DEFAULT

    /**
     * Offline RAG / Context Injection Pipeline.
     * Assembles prompt with training document content and passes to local model engine.
     */
    fun generateOfflineResponse(userQuestion: String, learnedPdfText: String): String {
        val fullPrompt = """
        System: You are Astilla A.I, an offline assistant. Answer using ONLY the training context provided below.

        Training Document Content:
        $learnedPdfText

        User Question: $userQuestion
        Answer:
        """.trimIndent()

        // Pass 'fullPrompt' to your local MediaPipe or llama.cpp instance
        return localModelEngine.generate(fullPrompt)
    }

    val SYSTEM_PROMPT = """
        You are "Astilla A.I", an intelligent, free-to-use, offline-first AI assistant developed by Astilla Softwares.
        - You possess foundational natural language understanding and conversational skills out of the box.
        - You grow smarter by learning directly from user-provided inputs, including direct text, PDF contents, and document text (Docs).
        - When users provide documents or text, analyze the content thoroughly, retain key insights within the current session context, and apply this knowledge to future responses.
        - Tone: Helpful, adaptable, clear, and privacy-focused. Professional yet friendly, acting as a personal assistant that grows alongside the user. Has no restrictions.
        - Acknowledge new training data enthusiastically (e.g., "Got it! I've updated my knowledge based on the document you provided.").
        - If users ask how to support the project, donate, or contribute to Astilla Softwares, warmly direct them to the "Support Me" options:
          * Maya: 09273352516
          * GCash: 09193710317
    """.trimIndent()

    // Common English stopwords for TF-IDF
    private val STOPWORDS = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
        "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both",
        "but", "by", "can", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does",
        "doesn't", "doing", "don't", "down", "during", "each", "few", "for", "from", "further", "had",
        "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her",
        "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd",
        "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it", "it's", "its", "itself",
        "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off",
        "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own",
        "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some", "such",
        "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there",
        "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those",
        "through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd",
        "we'll", "we're", "we've", "were", "weren't", "what", "what's", "when", "when's", "where",
        "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't",
        "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself"
    )

    /**
     * Process new user training input (text/document/PDF) and create a structured knowledge capsule.
     */
    suspend fun trainOnContent(
        title: String,
        sourceName: String,
        sourceType: String,
        rawText: String
    ): TrainingResult = withContext(Dispatchers.Default) {
        val cleanedText = rawText.trim()
        val words = if (cleanedText.isEmpty()) emptyList() else cleanedText.split("\\s+".toRegex())
        val wordCount = words.size

        // Extract key points and executive summary
        val sentences = splitSentences(cleanedText)
        val scoredSentences = rankSentences(sentences, cleanedText)

        val topSentences = scoredSentences.take(min(5, scoredSentences.size)).map { it.text }
        val coreSummary = if (topSentences.isNotEmpty()) {
            topSentences.joinToString(" ")
        } else {
            cleanedText.take(300)
        }

        val keyPoints = if (scoredSentences.size > 2) {
            scoredSentences.take(6).map { cleanBullet(it.text) }
        } else {
            listOf(cleanedText.take(150))
        }

        val resolvedTitle = if (title.isNotBlank()) title else generateTitleFromText(cleanedText, sourceName)

        val acknowledgment = "Got it! I've updated my knowledge based on the document you provided: \"$resolvedTitle\". I have assimilated its $wordCount words into my neural memory and will apply these insights in our conversations!"

        TrainingResult(
            title = resolvedTitle,
            sourceName = sourceName,
            sourceType = sourceType,
            rawText = cleanedText,
            coreSummary = coreSummary,
            extractedKeyPoints = keyPoints,
            wordCount = wordCount,
            acknowledgmentMessage = acknowledgment
        )
    }

    /**
     * Generate document or book summary according to specified mode.
     */
    suspend fun summarizeDocument(
        text: String,
        mode: SummaryMode,
        title: String = "Document Summary"
    ): String = withContext(Dispatchers.Default) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return@withContext "No text provided to summarize."

        val sentences = splitSentences(cleaned)
        val ranked = rankSentences(sentences, cleaned)
        val wordCount = cleaned.split("\\s+".toRegex()).size

        when (mode) {
            SummaryMode.EXECUTIVE -> {
                val top5 = ranked.take(min(6, ranked.size)).map { it.text }
                """
                # 📌 Executive Summary: $title
                
                **Document Metrics:** ~$wordCount words | Mode: Executive Overview
                
                ## Core Premise & Thesis
                ${top5.take(2).joinToString(" ")}
                
                ## Strategic Findings & Insights
                ${top5.drop(2).take(3).joinToString(" ")}
                
                ## Conclusion & Implication
                ${top5.lastOrNull() ?: "The document delivers strategic depth across its core themes."}
                """.trimIndent()
            }

            SummaryMode.TAKEAWAYS -> {
                val points = ranked.take(min(8, ranked.size)).map { cleanBullet(it.text) }
                val bullets = points.mapIndexed { idx, pt -> "${idx + 1}. **Action Item / Insight:** $pt" }.joinToString("\n\n")
                """
                # 🔑 Key Takeaways & Action Items: $title
                
                *Synthesized from $wordCount words of text.*
                
                $bullets
                
                ---
                💡 **Recommendation:** Store this in Astilla A.I's memory to reference these points during conversation!
                """.trimIndent()
            }

            SummaryMode.CHAPTERS -> {
                val sections = detectSectionsOrBreakdown(cleaned)
                val sectionSummaries = sections.mapIndexed { i, sec ->
                    val secSentences = splitSentences(sec.content)
                    val secRanked = rankSentences(secSentences, sec.content)
                    val lead = secRanked.take(3).joinToString(" ") { it.text }
                    """
                    ### 📖 Section ${i + 1}: ${sec.title}
                    $lead
                    """.trimIndent()
                }.joinToString("\n\n")

                """
                # 📑 Chapter & Section Breakdown: $title
                
                *Structured walkthrough across ${sections.size} thematic sections.*
                
                $sectionSummaries
                """.trimIndent()
            }

            SummaryMode.FLASHCARDS -> {
                val points = ranked.take(min(6, ranked.size))
                val cards = points.mapIndexed { idx, p ->
                    val question = generateFlashcardQuestion(p.text, idx + 1)
                    """
                    #### 🃏 Card #${idx + 1}
                    **Q:** $question  
                    **A:** ${p.text}
                    """.trimIndent()
                }.joinToString("\n\n")

                """
                # ⚡ Flashcards & Rapid Review: $title
                
                *Quick-retention Q&A cards generated by Astilla A.I.*
                
                $cards
                """.trimIndent()
            }

            SummaryMode.DEEP_DIVE -> {
                val top10 = ranked.take(min(10, ranked.size))
                val keyInsights = top10.take(4).joinToString("\n\n") { "• **${it.text.take(40)}...** ${it.text}" }
                val technicalAnalysis = top10.drop(4).take(4).joinToString(" ") { it.text }
                val finalTake = top10.lastOrNull()?.text ?: ""

                """
                # 🔬 Deep Dive Comprehensive Analysis: $title
                
                **Analysis Scope:** $wordCount words | Full Cognitive Ingestion
                
                ## 1. Fundamental Concepts & Architecture
                $keyInsights
                
                ## 2. In-Depth Mechanics & Dynamics
                $technicalAnalysis
                
                ## 3. Synthesis & Evaluation
                Astilla A.I evaluated the core thesis: $finalTake
                """.trimIndent()
            }
        }
    }

    /**
     * Offline response generator utilizing local knowledge base, active memory, and conversational heuristics.
     */
    suspend fun generateOfflineResponse(
        prompt: String,
        knowledgeDao: KnowledgeDao,
        memoryDao: MemoryDao,
        isMemoryEnabled: Boolean
    ): String = withContext(Dispatchers.Default) {
        val lowerPrompt = prompt.lowercase(Locale.ROOT).trim()

        // 1. Support & Developer Donation check
        if (isSupportQuery(lowerPrompt)) {
            return@withContext """
                Astilla A.I is developed with love by **Astilla Softwares** to be 100% free, private, and offline-first!
                
                If you find Astilla A.I helpful and wish to support continuous development and updates, you can warmly send your support to:
                
                📱 **Maya:** `$MAYA_NUMBER`
                💳 **GCash:** `$GCASH_NUMBER`
                
                *Your generosity empowers future models, offline capabilities, and new features. Thank you so much for being an essential part of Astilla Softwares!*
            """.trimIndent()
        }

        // 2. Identity & capabilities check
        if (isIdentityQuery(lowerPrompt)) {
            return@withContext """
                I am **Astilla A.I**, an intelligent, free-to-use, and offline-first AI assistant developed by **Astilla Softwares**.
                
                Here is what makes me unique:
                • 🧠 **Interactive Training**: You can train me by pasting text, loading PDFs, or importing documents. I assimilate new knowledge instantly!
                • 📚 **Document & Book Summarization**: I generate Executive Summaries, Key Takeaways, Chapter Breakdowns, and Flashcards.
                • 💾 **Context Memory**: I maintain persistent memory across conversations (which you can manage or wipe anytime in Settings).
                • 🔒 **Privacy & Offline First**: Your conversations and trained documents remain right on your device.
                
                How can I assist you today?
            """.trimIndent()
        }

        // 3. Retrieve relevant Knowledge from Room Knowledge Base
        val allCapsules = knowledgeDao.getAllKnowledgeCapsulesSnapshot()
        val relevantCapsule = findMostRelevantCapsule(prompt, allCapsules)

        // 4. Retrieve Active Memory items
        val memories = if (isMemoryEnabled) memoryDao.getActiveMemoriesSnapshot() else emptyList()
        val memoryContext = if (memories.isNotEmpty()) {
            memories.joinToString("; ") { "${it.key}: ${it.value}" }
        } else null

        // 5. Check if user is asking about their memory/preferences
        if (lowerPrompt.contains("what do you remember") || lowerPrompt.contains("my memory") || lowerPrompt.contains("what do you know about me")) {
            if (memories.isEmpty()) {
                return@withContext "Currently, I don't have any specific personal facts stored in active memory. You can tell me facts (e.g. \"Remember that my name is Alex\") or add memories in App Settings!"
            } else {
                val memList = memories.joinToString("\n") { "• **${it.key}**: ${it.value}" }
                return@withContext "Here is what I currently retain in active context memory:\n\n$memList\n\n*(You can edit or erase all stored context anytime in App Settings).* "
            }
        }

        // 6. Check if user provided an inline command to remember something
        val rememberRegex = Regex("^(?:remember that|remember|store memory|save preference)[:\\s]+(.+)", RegexOption.IGNORE_CASE)
        val rememberMatch = rememberRegex.find(prompt)
        if (rememberMatch != null) {
            val fact = rememberMatch.groupValues[1].trim()
            val colonSplit = fact.split(":", limit = 2)
            val key = if (colonSplit.size > 1) colonSplit[0].trim() else "User Context"
            val value = if (colonSplit.size > 1) colonSplit[1].trim() else fact

            memoryDao.insertMemory(
                MemoryItemEntity(
                    key = key,
                    value = value,
                    category = "USER_PREFERENCE"
                )
            )
            return@withContext "Understood! I've committed this to my active context memory: \"**$key**: $value\". I will keep this in mind during our conversations. (You can reset this anytime in Settings)."
        }

        // 7. Check if user wants a summary of some provided text in the chat
        if (lowerPrompt.startsWith("summarize:") || lowerPrompt.startsWith("summarize this:") || lowerPrompt.startsWith("tldr:")) {
            val toSummarize = prompt.substringAfter(":").trim()
            if (toSummarize.length > 50) {
                return@withContext summarizeDocument(toSummarize, SummaryMode.TAKEAWAYS, "Quick Chat Document")
            }
        }

        // 8. If a relevant knowledge capsule matched!
        if (relevantCapsule != null) {
            val contextText = relevantCapsule.rawText.ifBlank { relevantCapsule.coreSummary }
            val extractedAnswer = generateOfflineResponse(
                userQuestion = prompt,
                learnedPdfText = contextText
            )
            return@withContext """
                Based on the knowledge I learned from **"${relevantCapsule.title}"**:
                
                $extractedAnswer
                
                *(Source: ${relevantCapsule.sourceName} • ${relevantCapsule.wordCount} words trained)*
            """.trimIndent()
        }

        // 9. Intelligent offline conversational response
        return@withContext generateGeneralOfflineResponse(prompt, memoryContext)
    }

    private fun isSupportQuery(prompt: String): Boolean {
        val keywords = listOf("donate", "donation", "support", "gcash", "maya", "contribute", "developer", "astilla softwares", "pay", "tip")
        return keywords.any { prompt.contains(it) }
    }

    private fun isIdentityQuery(prompt: String): Boolean {
        val keywords = listOf("who are you", "what is your name", "what can you do", "introduce yourself", "tell me about yourself", "astilla a.i", "astilla ai")
        return keywords.any { prompt.contains(it) }
    }

    private fun generateGeneralOfflineResponse(prompt: String, memoryContext: String?): String {
        val lower = prompt.lowercase(Locale.ROOT)

        val memoryGreeting = if (memoryContext != null) {
            val nameFact = memoryContext.split("; ").firstOrNull { it.contains("name", ignoreCase = true) }
            if (nameFact != null) " [Context: ${nameFact.trim()}]" else ""
        } else ""

        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> {
                "Hello there!$memoryGreeting I am Astilla A.I, your offline-first assistant. You can ask me questions, train me with new texts or PDFs, or request structured book and document summaries. How can I help you today?"
            }
            lower.contains("how are you") -> {
                "I'm operating at peak efficiency! Neural weights and local indices are primed and ready for your documents or questions."
            }
            lower.contains("thank") -> {
                "You are very welcome! Always here to assist you with quick reasoning, memory recall, or deep document analysis."
            }
            lower.contains("calculate") || lower.contains("math") || lower.matches(Regex(".*[0-9]+[\\s]*[+\\-*/^][\\s]*[0-9]+.*")) -> {
                solveMathQuery(prompt)
            }
            lower.contains("code") || lower.contains("kotlin") || lower.contains("python") || lower.contains("java") || lower.contains("android") -> {
                "Here is guidance for your technical query regarding \"$prompt\":\n\n```kotlin\n// Astilla A.I Technical Blueprint\n// Fast, modular, and idiomatic implementation\nfun executeTask() {\n    // Structured logic tailored to your requirements\n    println(\"Processing completed efficiently.\")\n}\n```\n\nYou can also train me by uploading your API documentation or code files so I can provide highly customized coding help!"
            }
            else -> {
                val keyTerms = extractKeywords(prompt).take(4).joinToString(", ")
                "I've analyzed your prompt regarding ${if (keyTerms.isNotBlank()) "**$keyTerms**" else "this topic"}.\n\n" +
                "As an offline-first assistant, I can provide foundational guidance, answer based on your uploaded documents, or generate structured summaries. If you'd like me to know specific facts, policies, or textbook content, simply tap the **Training** tab or attach a document here to train me!"
            }
        }
    }

    private fun solveMathQuery(prompt: String): String {
        return try {
            val exprRegex = Regex("([0-9.]+)\\s*([+\\-*/^])\\s*([0-9.]+)")
            val match = exprRegex.find(prompt)
            if (match != null) {
                val a = match.groupValues[1].toDouble()
                val op = match.groupValues[2]
                val b = match.groupValues[3].toDouble()
                val res = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b != 0.0) a / b else Double.NaN
                    "^" -> Math.pow(a, b)
                    else -> 0.0
                }
                "Calculation: $a $op $b = **$res**"
            } else {
                "I can compute arithmetic equations, e.g. `45 * 12` or `1024 / 8`."
            }
        } catch (e: Exception) {
            "I noticed a mathematical expression. Could you format it clearly (e.g. `25 * 4`)?"
        }
    }

    private fun findMostRelevantCapsule(prompt: String, capsules: List<KnowledgeCapsuleEntity>): KnowledgeCapsuleEntity? {
        if (capsules.isEmpty()) return null
        val promptWords = extractKeywords(prompt)
        if (promptWords.isEmpty()) return null

        var bestScore = 0.0
        var bestCapsule: KnowledgeCapsuleEntity? = null

        for (capsule in capsules) {
            val capsuleWords = extractKeywords("${capsule.title} ${capsule.coreSummary} ${capsule.extractedKeyPoints}")
            val overlap = promptWords.count { word -> capsuleWords.contains(word) }
            val score = overlap.toDouble() / (promptWords.size + 1)
            if (score > 0.25 && score > bestScore) {
                bestScore = score
                bestCapsule = capsule
            }
        }
        return bestCapsule
    }

    private fun answerFromCapsule(prompt: String, capsule: KnowledgeCapsuleEntity): String {
        val sentences = splitSentences(capsule.rawText)
        val queryKeywords = extractKeywords(prompt)

        val ranked = sentences.map { s ->
            val sWords = extractKeywords(s)
            val score = queryKeywords.count { sWords.contains(it) }
            s to score
        }.sortedByDescending { it.second }

        val bestMatches = ranked.filter { it.second > 0 }.take(3).map { it.first }

        return if (bestMatches.isNotEmpty()) {
            bestMatches.joinToString("\n\n")
        } else {
            capsule.coreSummary
        }
    }

    private data class ScoredSentence(val text: String, val score: Double, val index: Int)

    private fun rankSentences(sentences: List<String>, fullText: String): List<ScoredSentence> {
        if (sentences.isEmpty()) return emptyList()

        val allWords = extractKeywords(fullText)
        val wordFreq = mutableMapOf<String, Int>()
        for (w in allWords) {
            wordFreq[w] = (wordFreq[w] ?: 0) + 1
        }

        val totalSentences = sentences.size
        return sentences.mapIndexed { idx, s ->
            val words = extractKeywords(s)
            var score = 0.0
            for (w in words) {
                val tf = wordFreq[w] ?: 1
                val idf = ln((totalSentences + 1.0) / (tf + 1.0))
                score += idf
            }
            // Position bonus for lead sentences
            if (idx == 0 || idx == 1) score *= 1.3
            ScoredSentence(s.trim(), score, idx)
        }.sortedByDescending { it.score }
    }

    private fun splitSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length > 20 }
    }

    fun extractKeywords(text: String): Set<String> {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.length > 2 && !STOPWORDS.contains(it) }
            .toSet()
    }

    private fun cleanBullet(s: String): String {
        return s.trim().removePrefix("•").removePrefix("-").removePrefix("*").trim()
    }

    private fun generateTitleFromText(text: String, fallback: String): String {
        val firstLine = text.lines().firstOrNull { it.isNotBlank() }?.trim() ?: fallback
        return if (firstLine.length in 4..50) firstLine else fallback.ifBlank { "Untitled Document" }
    }

    private data class DocumentSection(val title: String, val content: String)

    private fun detectSectionsOrBreakdown(text: String): List<DocumentSection> {
        val lines = text.lines()
        val sections = mutableListOf<DocumentSection>()
        var currentTitle = "Introduction"
        var currentBody = StringBuilder()

        val headerRegex = Regex("^(?:Chapter|Section|Part|Module|#|##|###)\\s*.*", RegexOption.IGNORE_CASE)

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length in 4..60 && headerRegex.matches(trimmed)) {
                if (currentBody.isNotBlank()) {
                    sections.add(DocumentSection(currentTitle, currentBody.toString().trim()))
                    currentBody = StringBuilder()
                }
                currentTitle = trimmed.removePrefix("#").removePrefix("##").removePrefix("###").trim()
            } else {
                currentBody.append(line).append("\n")
            }
        }

        if (currentBody.isNotBlank()) {
            sections.add(DocumentSection(currentTitle, currentBody.toString().trim()))
        }

        // If no explicit chapters detected, chunk proportionally into 3-4 segments
        if (sections.size <= 1 && text.length > 600) {
            val sentences = splitSentences(text)
            val chunkSize = (sentences.size / 3).coerceAtLeast(1)
            return listOf(
                DocumentSection("Core Thesis & Foundation", sentences.take(chunkSize).joinToString(" ")),
                DocumentSection("Key Mechanics & Discussion", sentences.drop(chunkSize).take(chunkSize).joinToString(" ")),
                DocumentSection("Conclusions & Implications", sentences.drop(chunkSize * 2).joinToString(" "))
            )
        }

        return sections.ifEmpty { listOf(DocumentSection("Main Content", text)) }
    }

    private fun generateFlashcardQuestion(sentence: String, index: Int): String {
        val words = extractKeywords(sentence)
        val mainSubject = words.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "concept"
        return when (index % 4) {
            0 -> "What is the primary significance of $mainSubject in this document?"
            1 -> "How does this document explain the role of $mainSubject?"
            2 -> "What key takeaway is established regarding $mainSubject?"
            else -> "Explain the core finding: $mainSubject."
        }
    }
}
