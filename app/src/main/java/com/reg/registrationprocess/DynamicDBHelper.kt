package com.reg.registrationprocess

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.reg.registrationprocess.DBUtils.REGISTRATION_DATABASE
import com.reg.registrationprocess.DBUtils.REGISTRATION_TABLE
import com.reg.registrationprocess.DBUtils.TOKEN_ISSUED_MARKED_AS
import com.reg.registrationprocess.DBUtils.TOKEN_NUMBER

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

    // ✅ CREATE TABLE WITH SERIAL NUMBER
    fun createDynamicTable(tableName: String, columns: List<String>) {

        val db = writableDatabase

        val cleanColumns = columns.map { normalizeColumnName(it) }
            .distinct()

        val sql = buildString {

            append("CREATE TABLE IF NOT EXISTS $tableName (")

            // 🔥 SERIAL NUMBER COLUMN
            append("$TOKEN_NUMBER INTEGER PRIMARY KEY AUTOINCREMENT, $TOKEN_ISSUED_MARKED_AS INTEGER DEFAULT 0 ,")
            cleanColumns.forEachIndexed { index, col ->
                append("[$col] TEXT")

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

        val db = writableDatabase
        val cv = ContentValues()

        data.forEach { (key, value) ->
            val cleanKey = normalizeColumnName(key)
            cv.put(cleanKey, value)
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
}