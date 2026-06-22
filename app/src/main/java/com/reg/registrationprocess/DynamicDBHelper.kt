package com.reg.registrationprocess

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import com.reg.registrationprocess.DBUtils.REGISTRATION_DATABASE
import com.reg.registrationprocess.DBUtils.REGISTRATION_TABLE
import com.reg.registrationprocess.DBUtils.TOKEN_ISSUED_MARKED_AS
import com.reg.registrationprocess.DBUtils.TOKEN_NUMBER
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

class DynamicDBHelper(context: Context) :
    SQLiteOpenHelper(context, REGISTRATION_DATABASE, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {}

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    private fun normalizeColumnName(input: String): String {
        var name = input.trim().lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), "_")

        if (name.isEmpty()) name = "col"

        if (name.first().isDigit()) {
            name = "col_$name"
        }

        return name
    }

    fun createDynamicTable(tableName: String, columns: List<String>) {

        println("Creating Table-->" + columns)

        val db = writableDatabase

        val cleanColumns = columns.map { normalizeColumnName(it) }
            .distinct()

        val sql = buildString {

            append("CREATE TABLE IF NOT EXISTS $tableName (")

            // 🔥 Auto ID
            append("$TOKEN_NUMBER INTEGER PRIMARY KEY AUTOINCREMENT, ")

            // 🔥 Default status column (0 by default)
            append("$TOKEN_ISSUED_MARKED_AS INTEGER DEFAULT 0")

            if (cleanColumns.isNotEmpty()) {
                append(", ")
            }

            // 🔥 Dynamic columns
            cleanColumns.forEachIndexed { index, col ->
                append("[$col] VARCHAR")

                if (index != cleanColumns.size - 1) {
                    append(", ")
                }
            }

            append(")")
        }

        db.execSQL(sql)
    }

    // ✅ INSERT ROW (no need to insert id)
    fun insertRow(tableName: String, data: Map<String, String>) {
        println(data)
        val db = writableDatabase
        val cv = ContentValues()

        data.forEach { (key, value) ->
            val cleanKey = normalizeColumnName(key)
            cv.put(cleanKey, value)
        }

        db.insert(tableName, null, cv)
    }

    fun insertRowNew(tableName: String, data: Map<String, String>) {
        println(data)
        val db = writableDatabase
        val cv = ContentValues()

        data.forEach { (key, value) ->
            //  val cleanKey = normalizeColumnName(key)
            cv.put(key, value)
        }

        db.insert(tableName, null, cv)
    }


    // 🔥 SEARCH (includes serial number too)
    fun search(tableName: String, text: String): MutableList<Map<String, String>> {

        val db = readableDatabase
        val list = mutableListOf<Map<String, String>>()
        val lowerText = text.lowercase()

        val cursor = db.rawQuery("SELECT * FROM $tableName", null)

        while (cursor.moveToNext()) {

            val row = mutableMapOf<String, String>()
            var matchFound = false

            for (i in 0 until cursor.columnCount) {

                val key = cursor.getColumnName(i)
                val value = cursor.getString(i) ?: ""

                row[key] = value

                if (value.lowercase().contains(lowerText)) {
                    matchFound = true
                }
            }

            if (matchFound) {
                list.add(row)
            }
        }

        cursor.close()

        return list
    }

    fun deleteAllData() {
        val db = writableDatabase

        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table'",
            null
        )

        while (cursor.moveToNext()) {
            val tableName = cursor.getString(0)

            if (tableName != "android_metadata" &&
                tableName != "sqlite_sequence"
            ) {
                db.execSQL("DROP TABLE IF EXISTS $tableName")
            }
        }
        cursor.close()
    }

    fun getColumnNames(tableName: String): List<String> {
        val db = readableDatabase
        val columns = mutableListOf<String>()

        val cursor = db.rawQuery("SELECT * FROM $tableName LIMIT 1", null)

        for (i in 0 until cursor.columnCount) {

            val column = cursor.getColumnName(i)

            // Ignore auto increment id
            if (column != TOKEN_NUMBER) {
                columns.add(column)
            }
        }

        cursor.close()

        return columns
    }

    fun updateTokenStatus(id: Int, status: Int) {

        val db = writableDatabase

        db.execSQL(
            "UPDATE $REGISTRATION_TABLE SET $TOKEN_ISSUED_MARKED_AS = $status WHERE $TOKEN_NUMBER = $id"
        )
    }

    fun deleteRow(id: Int) {
        val db = writableDatabase
        db.execSQL(
            "DELETE FROM $REGISTRATION_TABLE WHERE $TOKEN_NUMBER = ?",
            arrayOf(id)
        )
    }

    fun getAllData(tableName: String): MutableList<Map<String, String>> {

        val db = readableDatabase
        val list = mutableListOf<Map<String, String>>()

        val cursor = db.rawQuery("SELECT * FROM $tableName", null)

        while (cursor.moveToNext()) {

            val row = mutableMapOf<String, String>()

            for (i in 0 until cursor.columnCount) {

                val key = cursor.getColumnName(i)
                val value = cursor.getString(i) ?: ""

                row[key] = value
            }

            list.add(row)
        }

        cursor.close()

        return list
    }

    fun updateRowByTokenNumber(
        tableName: String,
        data: Map<String, String>
    ): Int {
        println("updateRowByTokenNumber->" + data)
        val db = writableDatabase
        val cv = ContentValues()

        // Get token number
        val tokenNumber = data[TOKEN_NUMBER]?.toIntOrNull() ?: return 0

        // Get all existing columns from the table
        val existingColumns = getColumnNames(tableName).toMutableSet()

        // Add primary key manually because getColumnNames() excludes it
        existingColumns.add(TOKEN_NUMBER)

        data.forEach { (key, value) ->

            val column = normalizeColumnName(key)

            // Ignore primary key
            if (column == TOKEN_NUMBER) return@forEach

            // Ignore columns that do not exist in the table
            if (!existingColumns.contains(column)) return@forEach

            cv.put(column, value)
        }

        return db.update(
            tableName,
            cv,
            "$TOKEN_NUMBER = ?",
            arrayOf(tokenNumber.toString())
        )
    }

    fun getDataByTokenNumber(
        tableName: String,
        tokenNumber: Int
    ): MutableMap<String, String>? {

        val db = readableDatabase
        val row = mutableMapOf<String, String>()

        val cursor = db.rawQuery(
            "SELECT * FROM $tableName WHERE $TOKEN_NUMBER = ?",
            arrayOf(tokenNumber.toString())
        )

        if (cursor.moveToFirst()) {
            for (i in 0 until cursor.columnCount) {
                row[cursor.getColumnName(i)] = cursor.getString(i) ?: ""
            }
        }

        cursor.close()

        return row.ifEmpty { null }
    }

    fun isTableExists(): Boolean {

        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(REGISTRATION_TABLE)
        )

        val exists = cursor.count > 0
        cursor.close()

        return exists
    }

    fun exportTableToExcel(
        context: Context,
    ): File? {

        return try {

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet(REGISTRATION_TABLE)
            val db = readableDatabase

            val cursor = db.rawQuery("SELECT * FROM $REGISTRATION_TABLE", null)

            val maxColumnLengths = IntArray(cursor.columnCount)

            // Header
            val headerRow = sheet.createRow(0)

            for (i in 0 until cursor.columnCount) {
                val header = cursor.getColumnName(i)
                headerRow.createCell(i).setCellValue(header)
                maxColumnLengths[i] = header.length
            }

            // Data
            var rowIndex = 1

            while (cursor.moveToNext()) {

                val row = sheet.createRow(rowIndex++)

                for (i in 0 until cursor.columnCount) {

                    val value = cursor.getString(i) ?: ""
                    row.createCell(i).setCellValue(value)

                    if (value.length > maxColumnLengths[i]) {
                        maxColumnLengths[i] = value.length
                    }
                }
            }

            cursor.close()

            // Column width
            for (i in maxColumnLengths.indices) {
                sheet.setColumnWidth(
                    i,
                    (maxColumnLengths[i] + 3).coerceAtMost(50) * 256
                )
            }

            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (dir != null && !dir.exists()) dir.mkdirs()

            val file = File(dir, "$REGISTRATION_TABLE.xlsx")

            FileOutputStream(file).use {
                workbook.write(it)
            }

            workbook.close()

            if (file.exists()) file else null

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportTableToExcel_(
        context: Context,
    ): String {

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(REGISTRATION_TABLE)
        val db = readableDatabase

        val cursor = db.rawQuery("SELECT * FROM $REGISTRATION_TABLE", null)

        // Store maximum length of each column
        val maxColumnLengths = IntArray(cursor.columnCount)

        // Header Row
        val headerRow = sheet.createRow(0)

        for (i in 0 until cursor.columnCount) {
            val header = cursor.getColumnName(i)
            headerRow.createCell(i).setCellValue(header)
            maxColumnLengths[i] = header.length
        }

        // Data Rows
        var rowIndex = 1

        while (cursor.moveToNext()) {

            val row = sheet.createRow(rowIndex++)

            for (i in 0 until cursor.columnCount) {

                val value = cursor.getString(i) ?: ""

                row.createCell(i).setCellValue(value)

                if (value.length > maxColumnLengths[i]) {
                    maxColumnLengths[i] = value.length
                }
            }
        }

        cursor.close()

        // Set column width based on longest value
        for (i in maxColumnLengths.indices) {

            // Maximum width allowed by Excel
            val width = ((maxColumnLengths[i] + 3).coerceAtMost(50)) * 256

            sheet.setColumnWidth(i, width)
        }

        // Save file
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw Exception("Unable to access Documents directory")

        if (!dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, "$REGISTRATION_TABLE.xlsx")

        FileOutputStream(file).use { output ->
            workbook.write(output)
        }

        workbook.close()

        return file.absolutePath
    }
}