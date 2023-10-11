package com.smarttoolfactory.composedrawingapp.model

import androidx.compose.ui.graphics.Path
import android.graphics.Path as AndroidPath
import androidx.compose.ui.graphics.asComposePath

data class MyLine(
    val path: Path,
    val pathProperties: PathProperties = PathProperties.DEFAULT
) {


    companion object {
        val EMPTY = MyLine(Path())

        fun createFromAndroidPath(
            path: AndroidPath,
            pathProperties: PathProperties = PathProperties.DEFAULT
        ): MyLine {
            return MyLine(path.asComposePath(), pathProperties)
        }
    }
}