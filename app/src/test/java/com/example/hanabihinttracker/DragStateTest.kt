package com.example.hanabihinttracker

import org.junit.Assert.assertEquals
import org.junit.Test

class DragStateTest {
    @Test
    fun dragCanCrossTheEntireHandInEitherDirection() {
        assertEquals(4, dragTargetIndex(startIndex = 0, offsetX = 450f, slotStep = 100f, cardCount = 5))
        assertEquals(0, dragTargetIndex(startIndex = 4, offsetX = -450f, slotStep = 100f, cardCount = 5))
    }

    @Test
    fun dragCrossesBoundaryAtHalfASlot() {
        assertEquals(1, dragTargetIndex(startIndex = 1, offsetX = 49f, slotStep = 100f, cardCount = 5))
        assertEquals(2, dragTargetIndex(startIndex = 1, offsetX = 51f, slotStep = 100f, cardCount = 5))
    }

    @Test
    fun moveItemProducesTheVisibleDropOrder() {
        assertEquals(listOf(2, 3, 4, 5, 1), moveItem(listOf(1, 2, 3, 4, 5), 0, 4))
        assertEquals(listOf(5, 1, 2, 3, 4), moveItem(listOf(1, 2, 3, 4, 5), 4, 0))
    }
}
