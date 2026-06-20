package com.reg.registrationprocess

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reg.registrationprocess.DBUtils.REGISTRATION_TABLE
import com.reg.registrationprocess.interfaces.AdapterClickView
import org.apache.poi.ss.usermodel.WorkbookFactory

class MainActivity : AppCompatActivity(), AdapterClickView {

    private lateinit var dbHelper: DynamicDBHelper

    private lateinit var adapter: EmployeeAdapter

    private val dataList =
        mutableListOf<Map<String, String>>()

    private val picker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri?.let {
                importExcel(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)

        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setToolbarInsetsActivity()
        dbHelper = DynamicDBHelper(this)

        val recycler =
            findViewById<RecyclerView>(R.id.recyclerView)

        recycler.layoutManager =
            LinearLayoutManager(this)

        val result =
            dbHelper.getAllData(
                REGISTRATION_TABLE
            )
        adapter = EmployeeAdapter(result, this)

        recycler.adapter = adapter

        findViewById<AppCompatImageView>(R.id.ic_import)
            .setOnClickListener {
                picker.launch("*/*")
            }

        findViewById<AppCompatImageView>(R.id.ic_add)
            .setOnClickListener {
                startActivity(Intent(this@MainActivity, ActivityAdd::class.java))
            }

        findViewById<AppCompatImageView>(R.id.ic_reset)
            .setOnLongClickListener {
                dbHelper.deleteAllData()
                Toast.makeText(
                    this,
                    "All data has been reset successfully",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }

        findViewById<SearchView>(R.id.searchView)
            .setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {

                    override fun onQueryTextSubmit(query: String?) = true

                    override fun onQueryTextChange(
                        newText: String?
                    ): Boolean {

                        val result =
                            dbHelper.search(
                                REGISTRATION_TABLE,
                                newText ?: "",
                            )

                        dataList.clear()
                        println("Countt--->" + result.size)
                        dataList.addAll(result)

                        adapter.notifyDataSetChanged()
                        return true
                    }
                })
    }

    private fun importExcel(uri: Uri) {

        val input = contentResolver.openInputStream(uri)

        val workbook = WorkbookFactory.create(input)
        val sheet = workbook.getSheetAt(0)

        val header = sheet.getRow(0)

        // 🔥 remove duplicate + normalize columns
        val seen = mutableSetOf<String>()
        val columns = mutableListOf<String>()

        for (i in 0 until header.lastCellNum) {

            val rawName = header.getCell(i)?.toString() ?: ""
            val cleanName = normalizeColumnName(rawName)

            if (cleanName.isNotEmpty() && !seen.contains(cleanName)) {
                seen.add(cleanName)
                columns.add(cleanName)
            }
        }

        // 🔥 create table (with system auto id inside helper)
        dbHelper.createDynamicTable(
            REGISTRATION_TABLE,
            columns
        )

        // 🔥 insert rows
        for (r in 1..sheet.lastRowNum) {

            val row = sheet.getRow(r) ?: continue

            val map = mutableMapOf<String, String>()

            for (c in columns.indices) {

                val cellValue = row.getCell(c)?.toString() ?: ""
                map[columns[c]] = cellValue
            }

            dbHelper.insertRow(
                REGISTRATION_TABLE,
                map
            )
        }

        workbook.close()
        input?.close()
    }

    private fun normalizeColumnName(input: String): String {
        var name = input.trim().lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), "_")

        if (name.isEmpty()) name = "col"
        if (name.first().isDigit()) name = "col_$name"

        return name
    }


    override fun checkBox(id: Int, status: Int) {
        dbHelper.updateTokenStatus(id, status)
    }

}