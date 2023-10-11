package com.smarttoolfactory.composedrawingapp.ui.menu

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smarttoolfactory.composedrawingapp.*
import com.smarttoolfactory.composedrawingapp.R
import com.smarttoolfactory.composedrawingapp.model.PathProperties
import com.smarttoolfactory.composedrawingapp.ui.ColorSlider
import com.smarttoolfactory.composedrawingapp.ui.ColorWheel
import com.smarttoolfactory.composedrawingapp.ui.theme.Blue400
import kotlin.math.roundToInt

@Composable
fun LayoutInfoMenus(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            onClick = {
                // TODO:
            }
        ) {
            Icon(
                Icons.Filled.TouchApp,
                contentDescription = null,
                tint = Color.LightGray
//                tint = if (currentDrawMode == DrawMode.Touch) Color.Black else Color.LightGray
            )
        }
        IconButton(
            onClick = {
                // TODO:
                Toast.makeText(context, "Let's test the toast", Toast.LENGTH_SHORT)
                    .show()
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_eraser_black_24dp),
                contentDescription = null,
                tint = Color.LightGray
//                tint = if (currentDrawMode == DrawMode.Erase) Color.Black else Color.LightGray
            )
        }
    }
}
