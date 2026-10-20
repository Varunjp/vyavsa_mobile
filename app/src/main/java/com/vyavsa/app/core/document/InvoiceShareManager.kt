package com.vyavsa.app.core.document

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.vyavsa.app.core.printer.InvoicePrintData
import java.io.File

/**
 * Handles generating and dispatching invoice PDFs through Android's native
 * ACTION_SEND share flow with secure FileProvider content URIs.
 */
object InvoiceShareManager {

    suspend fun shareInvoicePdf(
        context: Context,
        printData: InvoicePrintData
    ): Result<Unit> {
        val pdfResult = InvoicePdfGenerator.generateInvoicePdf(context, printData)
        if (pdfResult.isFailure) {
            return Result.failure(pdfResult.exceptionOrNull() ?: Exception("Failed to generate PDF document"))
        }

        val pdfFile = pdfResult.getOrNull()
            ?: return Result.failure(Exception("Generated invoice PDF file is null"))

        return dispatchShareIntent(context, pdfFile, printData.invoiceNumber)
    }

    private fun dispatchShareIntent(
        context: Context,
        pdfFile: File,
        invoiceNumber: String
    ): Result<Unit> {
        return try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice #$invoiceNumber")
                putExtra(Intent.EXTRA_TEXT, "Invoice #$invoiceNumber from Vyavsa Bill Book")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Invoice PDF via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
