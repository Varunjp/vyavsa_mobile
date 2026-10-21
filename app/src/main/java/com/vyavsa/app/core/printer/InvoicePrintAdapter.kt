package com.vyavsa.app.core.printer

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.vyavsa.app.core.document.InvoicePdfGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * PrintDocumentAdapter implementing Android's standard printing framework.
 * Supports Android POS terminals with native print services as well as standard
 * network/cloud printers.
 */
class InvoicePrintAdapter(
    private val context: Context,
    private val printData: InvoicePrintData
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder("Invoice_${printData.invoiceNumber}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()

        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (destination == null) {
            callback?.onWriteFailed("Invalid file descriptor")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pdfResult = InvoicePdfGenerator.generateInvoicePdf(context, printData)
                if (pdfResult.isFailure) {
                    callback?.onWriteFailed(pdfResult.exceptionOrNull()?.message ?: "Failed to generate PDF")
                    return@launch
                }

                val pdfFile = pdfResult.getOrNull()
                if (pdfFile == null || !pdfFile.exists()) {
                    callback?.onWriteFailed("Generated PDF file is missing")
                    return@launch
                }

                FileInputStream(pdfFile).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }

                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback?.onWriteFailed(e.message ?: "Error writing print document")
            }
        }
    }

    companion object {
        fun print(context: Context, printData: InvoicePrintData): Result<Unit> {
            return try {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    ?: return Result.failure(IllegalStateException("Print service not available on this device"))

                val jobName = "Invoice_${printData.invoiceNumber}"
                printManager.print(
                    jobName,
                    InvoicePrintAdapter(context, printData),
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build()
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
