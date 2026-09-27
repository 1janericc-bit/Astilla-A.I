package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AstillaBrainEngine
import com.example.data.model.SummaryMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Astilla A.I", appName)
    }

    @Test
    fun `verify Astilla developer support credentials`() {
        assertEquals("09273352516", AstillaBrainEngine.MAYA_NUMBER)
        assertEquals("09193710317", AstillaBrainEngine.GCASH_NUMBER)
        assertEquals("Astilla Softwares", AstillaBrainEngine.DEVELOPER_NAME)
    }

    @Test
    fun `verify document training extraction and summarization`() = runBlocking {
        val sampleDoc = """
            Artificial Intelligence has evolved dramatically over recent years.
            Modern language models now utilize deep contextual representations to solve complex logical problems.
            Astilla A.I focuses on privacy-preserving on-device computing.
            By retaining local context, users maintain total control of their data without external transmission.
        """.trimIndent()

        val training = AstillaBrainEngine.trainOnContent(
            title = "AI Privacy Architecture",
            sourceName = "architecture.txt",
            sourceType = "DOC",
            rawText = sampleDoc
        )

        assertEquals("AI Privacy Architecture", training.title)
        assertTrue(training.wordCount > 10)
        assertTrue(training.acknowledgmentMessage.contains("Got it!"))

        val summary = AstillaBrainEngine.summarizeDocument(
            text = sampleDoc,
            mode = SummaryMode.EXECUTIVE,
            title = "AI Privacy Architecture"
        )
        assertTrue(summary.contains("Executive Summary"))
    }

    @Test
    fun `verify offline RAG pipeline response generation`() {
        val learnedText = """
            Astilla Softwares was founded to build decentralized and offline-first intelligence.
            The flagship system is called Astilla A.I which operates entirely on-device.
            It uses local neural parsing to extract clean text from PDF documents.
        """.trimIndent()

        val question = "What is the flagship system called?"
        val answer = AstillaBrainEngine.generateOfflineResponse(
            userQuestion = question,
            learnedPdfText = learnedText
        )

        assertTrue(answer.contains("Astilla A.I", ignoreCase = true))
    }

    @Test
    fun `verify local PDF decoding with iText`() {
        // Create an in-memory PDF using iText
        val outputStream = java.io.ByteArrayOutputStream()
        val document = com.itextpdf.text.Document()
        com.itextpdf.text.pdf.PdfWriter.getInstance(document, outputStream)
        document.open()
        document.add(com.itextpdf.text.Paragraph("Astilla A.I Local Document Intelligence."))
        document.add(com.itextpdf.text.Paragraph("Page 1: Zero data leaves the user device."))
        document.close()

        val pdfBytes = outputStream.toByteArray()
        val extractedText = com.example.ai.PdfDocumentExtractor.extractPdfTextOffline(pdfBytes)

        assertTrue(extractedText.contains("Astilla A.I Local Document Intelligence"))
        assertTrue(extractedText.contains("Zero data leaves the user device"))
    }
}
