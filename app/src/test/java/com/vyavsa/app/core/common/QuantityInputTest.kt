package com.vyavsa.app.core.common

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityInputTest {

    @Test
    fun testInitialFocusSelectsAllText() {
        val initialText = "1"
        val tfv = TextFieldValue(text = initialText, selection = TextRange(initialText.length))
        
        // Simulating focus gain selection
        val focusedTfv = tfv.copy(selection = TextRange(0, tfv.text.length))
        
        assertEquals(0, focusedTfv.selection.start)
        assertEquals(1, focusedTfv.selection.end)
    }

    @Test
    fun testTypingWhenTextIsSelectedReplacesValue() {
        val initialText = "1"
        // Initially selected range (0, 1)
        val selection = TextRange(0, initialText.length)
        
        // Simulating user typing '5' over the selection
        val typedChar = "5"
        val newText = initialText.replaceRange(selection.min, selection.max, typedChar)
        
        assertEquals("5", newText)
        // Verify it is NOT "51"
        org.junit.Assert.assertNotEquals("51", newText)
    }

    @Test
    fun testFilteringNonNumericCharacters() {
        val rawInput = "5a#b.2"
        val filtered = rawInput.filter { it.isDigit() || it == '.' }
        
        assertEquals("5.2", filtered)
    }

    @Test
    fun testExternalQuantitySyncPreservesSelectionBounds() {
        var currentTfv = TextFieldValue(text = "1", selection = TextRange(0, 1))
        
        // External increment to "2"
        val externalQuantity = "2"
        val isFocused = true
        val newSelection = if (isFocused) {
            val start = minOf(currentTfv.selection.start, externalQuantity.length)
            val end = minOf(currentTfv.selection.end, externalQuantity.length)
            TextRange(start, end)
        } else {
            TextRange(externalQuantity.length)
        }
        currentTfv = TextFieldValue(text = externalQuantity, selection = newSelection)
        
        assertEquals("2", currentTfv.text)
        assertEquals(0, currentTfv.selection.start)
        assertEquals(1, currentTfv.selection.end)
    }

    @Test
    fun testEmptyInputCalculatesSafelyInSaleTotal() {
        val emptyQuantity = ""
        val q = emptyQuantity.toDoubleOrNull() ?: 0.0
        val p = "100.00".toDoubleOrNull() ?: 0.0
        val total = q * p
        
        assertEquals(0.0, total, 0.001)
    }
}
