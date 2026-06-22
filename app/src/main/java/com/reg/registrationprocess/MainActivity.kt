package com.reg.registrationprocess

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reg.registrationprocess.DBUtils.REGISTRATION_TABLE
import com.reg.registrationprocess.interfaces.AdapterClickView
import org.apache.poi.ss.usermodel.WorkbookFactory
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File

class MainActivity : AppCompatActivity(), AdapterClickView {

    private lateinit var dbHelper: DynamicDBHelper

    private
    val DEFAULT_BG_COLOR: Int = Color.YELLOW

    private
    val DEFAULT_FG_COLOR: Int = Color.RED

    private val textHighlighter: TextHighlighter = TextHighlighter()
        .setBackgroundColor(DEFAULT_BG_COLOR)
        .setForegroundColor(DEFAULT_FG_COLOR)
    private lateinit var adapter: EmployeeAdapter
    private lateinit var recycler: RecyclerView
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
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.hide()
        setToolbarInsetsActivity()
        dbHelper = DynamicDBHelper(this)

        recycler = findViewById(R.id.recyclerView)
        recycler.layoutManager =
            LinearLayoutManager(this)
        loadData()
        findViewById<AppCompatImageView>(R.id.ic_import)
            .setOnClickListener {
                picker.launch("*/*")
            }

        findViewById<AppCompatImageView>(R.id.ic_share)
            .setOnClickListener {
                val path = dbHelper.exportTableToExcel(this)

                if (path != null) {
                    Toast.makeText(
                        this,
                        "Excel Saved Successfully\n$path",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Export Failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
                path?.let {
                    openExcelFile(it)
                }
            }

        findViewById<AppCompatImageView>(R.id.ic_add)
            .setOnClickListener {
                val intent = Intent(this@MainActivity, ActivityAdd::class.java)
                intent.putExtra("id", "")
                startActivity(intent)
            }

        findViewById<AppCompatImageView>(R.id.ic_reset)
            .setOnLongClickListener {
                showCustomDialog()
                true
            }

        findViewById<SearchView>(R.id.searchView)
            .setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?) = true
                    override fun onQueryTextChange(
                        newText: String?
                    ): Boolean {
                        val isReady = dbHelper.isTableExists()

                        if (isReady) {

                            val result =
                                dbHelper.search(
                                    REGISTRATION_TABLE,
                                    newText ?: "",
                                )
                            println("Countt--->" + result.size)
                            adapter = EmployeeAdapter(result, this@MainActivity)
                            recycler.adapter = adapter
                            adapter.notifyDataSetChanged()
                        }
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
        val columns = mutableListOf<String>()
        val nameCount = mutableMapOf<String, Int>()

        for (i in 0 until header.lastCellNum) {

            val rawName = header.getCell(i)?.toString().orEmpty()
            val cleanName = normalizeColumnName(rawName)

            val count = nameCount.getOrDefault(cleanName, 0)

            val finalName = if (count == 0) {
                cleanName
            } else {
                "${cleanName}_$count"
            }

            nameCount[cleanName] = count + 1
            columns.add(finalName)
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
        loadData()
    }

    private fun normalizeColumnName(input: String): String {
        var name = input.trim().lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), "_")

        if (name.isEmpty()) name = "col"
        if (name.first().isDigit()) name = "col_$name"

        return name
    }

    fun loadData() {
        val isReady = dbHelper.isTableExists()

        if (isReady) {
            val result =
                dbHelper.getAllData(
                    REGISTRATION_TABLE
                )
            adapter = EmployeeAdapter(result, this)

            recycler.adapter = adapter
        }
    }

    override fun checkBox(id: Int, status: Int) {
        dbHelper.updateTokenStatus(id, status)
    }


    override fun edit(id: Int) {
        val intent = Intent(this@MainActivity, ActivityAdd::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }

    override fun callIntentInit(mobile: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                // data = ("tel:$mobile").toUri()
                data = ("tel:9943095354").toUri()
            }
            startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                100
            )
        }
    }

    fun openExcelFile(file: File) {

        val uri = FileProvider.getUriForFile(
            this,
            "${this.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                uri,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open Excel file", Toast.LENGTH_LONG).show()
        }
    }

    private fun showCustomDialog() {

        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.dialog_custom)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // dialog.setCancelable(false)

        val txtTitle = dialog.findViewById<TextView>(R.id.txtTitle)
        val txtMessage = dialog.findViewById<TextView>(R.id.txtMessage)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnOk = dialog.findViewById<Button>(R.id.btnOk)

        txtTitle?.text = "Delete Item"
        txtMessage?.text = "Are you sure you want to delete this item?"

        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }

        btnOk?.setOnClickListener {
            dbHelper.deleteAllData()
            Toast.makeText(
                this,
                "All data has been reset successfully",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }

        dialog.show()


    }

}