package com.smarttoolfactory.composedrawingapp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.smarttoolfactory.composedrawingapp.model.PathProperties
import org.junit.Test
import org.junit.Assert.*

/**
 * Basic local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class BasicUnitTest {
    
    @Test
    fun pathProperties_defaultValues_areCorrect() {
        val pathProperties = PathProperties()
        
        assertEquals(Color.Black, pathProperties.color)
        assertEquals(10f, pathProperties.strokeWidth)
        assertEquals(1f, pathProperties.alpha)
        assertEquals(StrokeCap.Round, pathProperties.strokeCap)
        assertEquals(StrokeJoin.Round, pathProperties.strokeJoin)
        assertFalse(pathProperties.eraseMode)
    }
    
    @Test
    fun pathProperties_customValues_areSetCorrectly() {
        val pathProperties = PathProperties(
            color = Color.Red,
            strokeWidth = 20f,
            alpha = 0.5f,
            strokeCap = StrokeCap.Butt,
            strokeJoin = StrokeJoin.Miter,
            eraseMode = true
        )
        
        assertEquals(Color.Red, pathProperties.color)
        assertEquals(20f, pathProperties.strokeWidth)
        assertEquals(0.5f, pathProperties.alpha)
        assertEquals(StrokeCap.Butt, pathProperties.strokeCap)
        assertEquals(StrokeJoin.Miter, pathProperties.strokeJoin)
        assertTrue(pathProperties.eraseMode)
    }
    
    @Test
    fun drawMode_values_areDistinct() {
        assertNotEquals(DrawMode.Draw, DrawMode.Touch)
        assertNotEquals(DrawMode.Draw, DrawMode.Erase)
        assertNotEquals(DrawMode.Touch, DrawMode.Erase)
    }
}