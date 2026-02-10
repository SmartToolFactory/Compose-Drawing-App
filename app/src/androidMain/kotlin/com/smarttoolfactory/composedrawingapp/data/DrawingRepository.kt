package com.smarttoolfactory.composedrawingapp.data

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.smarttoolfactory.composedrawingapp.model.DrawingStroke
import com.smarttoolfactory.composedrawingapp.model.PathProperties
import com.smarttoolfactory.composedrawingapp.model.SavedDrawing
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DrawingRepository(context: Context) {

    private val driver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = context,
        name = "Drawing.db",
        callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.setForeignKeyConstraintsEnabled(true)
                
                // Check if SavedDrawingEntity table exists, create if it doesn't
                val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='SavedDrawingEntity'")
                val tableExists = cursor.moveToFirst()
                cursor.close()
                
                if (!tableExists) {
                    // Create the new tables if they don't exist
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS SavedDrawingEntity (
                            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL UNIQUE,
                            dateCreated INTEGER NOT NULL
                        )
                    """.trimIndent())
                    
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS SavedDrawingStrokeEntity (
                            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            drawingId INTEGER NOT NULL,
                            strokeWidth REAL NOT NULL,
                            color INTEGER NOT NULL,
                            alpha REAL NOT NULL,
                            strokeCap TEXT NOT NULL,
                            strokeJoin TEXT NOT NULL,
                            eraseMode INTEGER NOT NULL,
                            pointsJson TEXT NOT NULL,
                            FOREIGN KEY (drawingId) REFERENCES SavedDrawingEntity(id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                }
                
                // Check if currentDrawingName column exists in DrawingPreferenceEntity, add if it doesn't
                val columnCursor = db.query("PRAGMA table_info(DrawingPreferenceEntity)")
                var hasCurrentDrawingName = false
                while (columnCursor.moveToNext()) {
                    val columnName = columnCursor.getString(1)
                    if (columnName == "currentDrawingName") {
                        hasCurrentDrawingName = true
                        break
                    }
                }
                columnCursor.close()
                
                if (!hasCurrentDrawingName) {
                    db.execSQL("ALTER TABLE DrawingPreferenceEntity ADD COLUMN currentDrawingName TEXT")
                }
            }
            
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                super.onUpgrade(db, oldVersion, newVersion)
                if (oldVersion < 2) {
                    // Migration from version 1 to 2: Add saved drawings tables
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS SavedDrawingEntity (
                            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL UNIQUE,
                            dateCreated INTEGER NOT NULL
                        )
                    """.trimIndent())
                    
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS SavedDrawingStrokeEntity (
                            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                            drawingId INTEGER NOT NULL,
                            strokeWidth REAL NOT NULL,
                            color INTEGER NOT NULL,
                            alpha REAL NOT NULL,
                            strokeCap TEXT NOT NULL,
                            strokeJoin TEXT NOT NULL,
                            eraseMode INTEGER NOT NULL,
                            pointsJson TEXT NOT NULL,
                            FOREIGN KEY (drawingId) REFERENCES SavedDrawingEntity(id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                }
            }
        }
    )
    private val database = Database(driver)
    private val queries = database.drawingQueries
    private val gson = Gson()

    fun saveStrokes(strokes: List<DrawingStroke>) {
        queries.transaction {
            queries.deleteAllStrokes()
            strokes.forEach { stroke ->
                val pointsJson = gson.toJson(stroke.points.map { SerializablePoint(it.x, it.y) })
                val props = stroke.pathProperties
                
                val capStr = when (props.strokeCap) {
                    StrokeCap.Butt -> "Butt"
                    StrokeCap.Round -> "Round"
                    else -> "Square" 
                }
                val joinStr = when (props.strokeJoin) {
                    StrokeJoin.Miter -> "Miter"
                    StrokeJoin.Round -> "Round"
                    else -> "Bevel"
                }

                queries.insertStroke(
                    strokeWidth = props.strokeWidth.toDouble(),
                    color = props.color.value.toLong(),
                    alpha = props.alpha.toDouble(),
                    strokeCap = capStr,
                    strokeJoin = joinStr,
                    eraseMode = if (props.eraseMode) 1L else 0L,
                    pointsJson = pointsJson
                )
            }
        }
    }

    fun getStrokes(): List<DrawingStroke> {
         return queries.getAllStrokes().executeAsList().map { entity ->
             val pointsType = object : TypeToken<List<SerializablePoint>>() {}.type
             val sPoints: List<SerializablePoint> = gson.fromJson(entity.pointsJson, pointsType)
             val points = sPoints.map { Offset(it.x, it.y) }
             
             val cap = when (entity.strokeCap) {
                 "Butt" -> StrokeCap.Butt
                 "Round" -> StrokeCap.Round
                 else -> StrokeCap.Square
             }
             val join = when (entity.strokeJoin) {
                 "Miter" -> StrokeJoin.Miter
                 "Round" -> StrokeJoin.Round
                 else -> StrokeJoin.Bevel
             }

             val props = PathProperties(
                 strokeWidth = entity.strokeWidth.toFloat(),
                 color = Color(entity.color.toULong()),
                 alpha = entity.alpha.toFloat(),
                 strokeCap = cap,
                 strokeJoin = join,
                 eraseMode = entity.eraseMode == 1L
             )
             DrawingStroke(props, points)
         }
    }

    fun savePreference(properties: PathProperties, currentDrawingName: String?) {
         val capStr = when (properties.strokeCap) {
            StrokeCap.Butt -> "Butt"
            StrokeCap.Round -> "Round"
            else -> "Square" 
         }
         val joinStr = when (properties.strokeJoin) {
            StrokeJoin.Miter -> "Miter"
            StrokeJoin.Round -> "Round"
            else -> "Bevel"
         }
         queries.savePreference(
             strokeWidth = properties.strokeWidth.toDouble(),
             color = properties.color.value.toLong(),
             alpha = properties.alpha.toDouble(),
             strokeCap = capStr,
             strokeJoin = joinStr,
             eraseMode = if (properties.eraseMode) 1L else 0L,
             currentDrawingName = currentDrawingName
         )
    }

    fun getPreference(): Pair<PathProperties?, String?> {
        val entity = queries.getPreference().executeAsOneOrNull() ?: return Pair(null, null)
        
        val cap = when (entity.strokeCap) {
             "Butt" -> StrokeCap.Butt
             "Round" -> StrokeCap.Round
             else -> StrokeCap.Square
        }
        val join = when (entity.strokeJoin) {
             "Miter" -> StrokeJoin.Miter
             "Round" -> StrokeJoin.Round
             else -> StrokeJoin.Bevel
        }
        
        val properties = PathProperties(
             strokeWidth = entity.strokeWidth.toFloat(),
             color = Color(entity.color.toULong()),
             alpha = entity.alpha.toFloat(),
             strokeCap = cap,
             strokeJoin = join,
             eraseMode = entity.eraseMode == 1L
        )
        
        return Pair(properties, entity.currentDrawingName)
    }
    
    // Saved Drawings Management
    
    fun getAllSavedDrawings(): List<SavedDrawing> {
        return queries.getAllSavedDrawings().executeAsList().map {
            SavedDrawing(
                id = it.id,
                name = it.name,
                dateCreated = it.dateCreated
            )
        }
    }
    
    fun drawingNameExists(name: String): Boolean {
        return queries.getSavedDrawing(name).executeAsOneOrNull() != null
    }
    
    fun saveDrawing(name: String, strokes: List<DrawingStroke>): Boolean {
        return try {
            queries.transaction {
                // Check if drawing exists
                val existing = queries.getSavedDrawing(name).executeAsOneOrNull()
                val drawingId = if (existing != null) {
                    // Update existing drawing
                    queries.updateSavedDrawingStrokes(existing.id)
                    existing.id
                } else {
                    // Create new drawing
                    queries.insertSavedDrawing(name, System.currentTimeMillis())
                    queries.getLastInsertedDrawingId().executeAsOne()
                }
                
                // Save strokes
                strokes.forEach { stroke ->
                    val pointsJson = gson.toJson(stroke.points.map { SerializablePoint(it.x, it.y) })
                    val props = stroke.pathProperties
                    
                    val capStr = when (props.strokeCap) {
                        StrokeCap.Butt -> "Butt"
                        StrokeCap.Round -> "Round"
                        else -> "Square" 
                    }
                    val joinStr = when (props.strokeJoin) {
                        StrokeJoin.Miter -> "Miter"
                        StrokeJoin.Round -> "Round"
                        else -> "Bevel"
                    }

                    queries.insertSavedDrawingStroke(
                        drawingId = drawingId,
                        strokeWidth = props.strokeWidth.toDouble(),
                        color = props.color.value.toLong(),
                        alpha = props.alpha.toDouble(),
                        strokeCap = capStr,
                        strokeJoin = joinStr,
                        eraseMode = if (props.eraseMode) 1L else 0L,
                        pointsJson = pointsJson
                    )
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun loadDrawing(drawingId: Long): List<DrawingStroke> {
        return queries.getSavedDrawingStrokes(drawingId).executeAsList().map { entity ->
            val pointsType = object : TypeToken<List<SerializablePoint>>() {}.type
            val sPoints: List<SerializablePoint> = gson.fromJson(entity.pointsJson, pointsType)
            val points = sPoints.map { Offset(it.x, it.y) }
            
            val cap = when (entity.strokeCap) {
                "Butt" -> StrokeCap.Butt
                "Round" -> StrokeCap.Round
                else -> StrokeCap.Square
            }
            val join = when (entity.strokeJoin) {
                "Miter" -> StrokeJoin.Miter
                "Round" -> StrokeJoin.Round
                else -> StrokeJoin.Bevel
            }

            val props = PathProperties(
                strokeWidth = entity.strokeWidth.toFloat(),
                color = Color(entity.color.toULong()),
                alpha = entity.alpha.toFloat(),
                strokeCap = cap,
                strokeJoin = join,
                eraseMode = entity.eraseMode == 1L
            )
            DrawingStroke(props, points)
        }
    }
    
    fun deleteDrawing(drawingId: Long): Boolean {
        return try {
            queries.transaction {
                queries.deleteSavedDrawingStrokes(drawingId)
                queries.deleteSavedDrawing(drawingId)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

data class SerializablePoint(val x: Float, val y: Float)
