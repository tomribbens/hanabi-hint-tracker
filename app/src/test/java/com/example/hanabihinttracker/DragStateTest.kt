package com.example.hanabihinttracker

import org.junit.Assert.assertEquals
import org.junit.Test

class DragStateTest {
    @Test
    fun dragCanCrossTheEntireHandInEitherDirection() {
        assertEquals(5, dragTargetIndex(startIndex = 0, offsetX = 550f, slotStep = 100f, cardCount = 6))
        assertEquals(0, dragTargetIndex(startIndex = 5, offsetX = -550f, slotStep = 100f, cardCount = 6))
    }

    @Test
    fun dragCrossesBoundaryAtHalfASlot() {
        assertEquals(1, dragTargetIndex(startIndex = 1, offsetX = 49f, slotStep = 100f, cardCount = 5))
        assertEquals(2, dragTargetIndex(startIndex = 1, offsetX = 51f, slotStep = 100f, cardCount = 5))
    }

    @Test
    fun moveItemProducesTheVisibleDropOrder() {
        assertEquals(listOf(2, 3, 4, 5, 6, 1), moveItem(listOf(1, 2, 3, 4, 5, 6), 0, 5))
        assertEquals(listOf(6, 1, 2, 3, 4, 5), moveItem(listOf(1, 2, 3, 4, 5, 6), 5, 0))
    }
}
