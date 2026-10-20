package com.vyavsa.app.core.printer

/**
 * Standard ESC/POS command sequences supported by thermal receipt printers.
 */
object EscPosCommands {
    // Hardware control
    val INIT = byteArrayOf(0x1B, 0x40) // ESC @: Initialize printer

    // Text Alignment
    val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0: Left justify
    val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1: Center justify
    val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // ESC a 2: Right justify

    // Font Sizing & Weight
    val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // ESC E 1: Bold on
    val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // ESC E 0: Bold off
    val DOUBLE_SIZE_ON = byteArrayOf(0x1D, 0x21, 0x11) // GS ! 0x11: Double height + width
    val DOUBLE_HEIGHT_ON = byteArrayOf(0x1D, 0x21, 0x01) // GS ! 0x01: Double height
    val DOUBLE_WIDTH_ON = byteArrayOf(0x1D, 0x21, 0x10) // GS ! 0x10: Double width
    val NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00) // GS ! 0x00: Normal character size

    // Line Feeds & Spacing
    val LINE_FEED = byteArrayOf(0x0A) // LF: Line Feed
    fun feedLines(count: Int): ByteArray = ByteArray(count) { 0x0A }

    // Paper Cutting
    val CUT_PARTIAL = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // GS V 'B' 0: Partial cut
    val CUT_FULL = byteArrayOf(0x1D, 0x56, 0x00) // GS V 0: Full cut

    /**
     * Converts a 2D raster image into standard ESC/POS raster bit image format:
     * GS v 0 m xL xH yL yH d1...dk
     *
     * Centers the bitmap, applies luminance thresholding for high-contrast thermal printing,
     * and restores left alignment after printing.
     */
    fun generateRasterBitImage(raster: RasterImage, maxWidthDots: Int): ByteArray {
        if (raster.width <= 0 || raster.height <= 0) return ByteArray(0)

        // Scale proportionally if original width exceeds maxWidthDots
        val scale = if (raster.width > maxWidthDots) {
            maxWidthDots.toDouble() / raster.width
        } else {
            1.0
        }
        val scaledWidth = maxOf(1, (raster.width * scale).toInt())
        val scaledHeight = maxOf(1, (raster.height * scale).toInt())

        // Width in bytes must be rounded up to whole bytes (8 dots per byte)
        val bytesPerRow = (scaledWidth + 7) / 8
        val targetHeightDots = scaledHeight

        // ESC/POS GS v 0 header
        val header = byteArrayOf(
            0x1D, 0x76, 0x30, 0x00, // GS v 0 m (m=0: normal mode)
            (bytesPerRow and 0xFF).toByte(),
            ((bytesPerRow shr 8) and 0xFF).toByte(),
            (targetHeightDots and 0xFF).toByte(),
            ((targetHeightDots shr 8) and 0xFF).toByte()
        )

        val rasterBytes = ByteArray(bytesPerRow * targetHeightDots)
        var byteIdx = 0

        for (y in 0 until targetHeightDots) {
            val srcY = ((y.toDouble() / targetHeightDots) * raster.height).toInt().coerceIn(0, raster.height - 1)
            for (xByte in 0 until bytesPerRow) {
                var currentByte = 0
                for (bit in 0..7) {
                    val dotX = xByte * 8 + bit
                    if (dotX < scaledWidth) {
                        val srcX = ((dotX.toDouble() / scaledWidth) * raster.width).toInt().coerceIn(0, raster.width - 1)
                        val pixel = raster.getPixel(srcX, srcY)
                        val alpha = (pixel shr 24) and 0xFF
                        if (alpha >= 128) {
                            val r = (pixel shr 16) and 0xFF
                            val g = (pixel shr 8) and 0xFF
                            val b = pixel and 0xFF
                            // Standard luminance formula
                            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                            if (luminance < 160) {
                                // 1 = Black dot printed
                                currentByte = currentByte or (1 shl (7 - bit))
                            }
                        }
                    }
                }
                rasterBytes[byteIdx++] = currentByte.toByte()
            }
        }

        return ALIGN_CENTER + header + rasterBytes + LINE_FEED + ALIGN_LEFT
    }
}
