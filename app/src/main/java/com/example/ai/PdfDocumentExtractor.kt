package com.example.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class ExtractedDocument(
    val fileName: String,
    val text: String,
    val mimeType: String,
    val charCount: Int,
    val wordCount: Int
)

object PdfDocumentExtractor {

    /**
     * Local PDF Decoding (Text Extraction) using iText parser library.
     * Extracts clean text page by page without raw byte clutter or external transmission.
     */
    fun extractPdfTextOffline(filePath: String): String {
        val pdfReader = PdfReader(filePath)
        val numberOfPages = pdfReader.numberOfPages
        var extractedText = ""

        for (i in 1..numberOfPages) {
            // Extracts clean text page by page (no endstream/endobj binary clutter)
            extractedText += PdfTextExtractor.getTextFromPage(pdfReader, i) + "\n"
        }
        pdfReader.close()
        return extractedText
    }

    /**
     * Local PDF Decoding from an InputStream.
     */
    fun extractPdfTextOffline(inputStream: InputStream): String {
        val pdfReader = PdfReader(inputStream)
        val numberOfPages = pdfReader.numberOfPages
        var extractedText = ""

        for (i in 1..numberOfPages) {
            extractedText += PdfTextExtractor.getTextFromPage(pdfReader, i) + "\n"
        }
        pdfReader.close()
        return extractedText
    }

    /**
     * Local PDF Decoding from in-memory ByteArray.
     */
    fun extractPdfTextOffline(pdfBytes: ByteArray): String {
        val pdfReader = PdfReader(pdfBytes)
        val numberOfPages = pdfReader.numberOfPages
        var extractedText = ""

        for (i in 1..numberOfPages) {
            extractedText += PdfTextExtractor.getTextFromPage(pdfReader, i) + "\n"
        }
        pdfReader.close()
        return extractedText
    }

    suspend fun extractFromUri(context: Context, uri: Uri): Result<ExtractedDocument> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val fileName = queryFileName(context, uri) ?: "Document"
            val mimeType = contentResolver.getType(uri) ?: "text/plain"

            val stream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file stream for $uri"))

            val text = stream.use { inputStream ->
                if (mimeType.contains("pdf", ignoreCase = true) || fileName.endsWith(".pdf", ignoreCase = true)) {
                    // Local PDF decoding using iText parser library
                    extractPdfTextOffline(inputStream)
                } else {
                    // Standard text / markdown / csv reader
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        reader.readText()
                    }
                }
            }

            val cleanedText = text.trim()
            val words = if (cleanedText.isEmpty()) 0 else cleanedText.split("\\s+".toRegex()).size

            Result.success(
                ExtractedDocument(
                    fileName = fileName,
                    text = cleanedText,
                    mimeType = mimeType,
                    charCount = cleanedText.length,
                    wordCount = words
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return it.getString(nameIndex)
                    }
                }
            }
        }
        return uri.lastPathSegment
    }
}
