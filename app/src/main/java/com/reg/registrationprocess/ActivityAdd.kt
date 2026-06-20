package com.reg.registrationprocess

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reg.registrationprocess.DBUtils.REGISTRATION_TABLE
import com.reg.registrationprocess.dataClass.DynamicField
import com.reg.registrationprocess.databinding.ActivityAddBinding

class ActivityAdd : AppCompatActivity() {

    private lateinit var binding: ActivityAddBinding
    private lateinit var adapter: AddDataAdapter
    private lateinit var dbHelper: DynamicDBHelper
    private var id: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setToolbarInsetsActivity()
        dbHelper = DynamicDBHelper(this)
        binding.recyclerviewAdd.layoutManager = LinearLayoutManager(this)

        id = intent.getIntExtra("id", 0)




        if (id == 0) {
            val columns = dbHelper.getColumnNames(REGISTRATION_TABLE)
            val list = columns.map {
                DynamicField(it)
            }.toMutableList()
            adapter = AddDataAdapter(list)
            binding.recyclerviewAdd.adapter = adapter
        } else {
            val row = dbHelper.getDataByTokenNumber(REGISTRATION_TABLE, id)

            val list = row?.map { (key, value) ->
                DynamicField(
                    columnName = key,
                    value = value
                )
            }?.toMutableList() ?: mutableListOf()

            adapter = AddDataAdapter(list)
            binding.recyclerviewAdd.adapter = adapter
        }

        // SAVE BUTTON
        binding.btnSave.setOnClickListener {
            val data = adapter.getFinalData()
            if (id == 0) {

                dbHelper.insertRow(REGISTRATION_TABLE, data)
                Toast.makeText(
                    this,
                    "Inserted successfully",
                    Toast.LENGTH_LONG
                ).show()
            } else {

                dbHelper.updateRowByTokenNumber(REGISTRATION_TABLE, data)

                Toast.makeText(
                    this,
                    "Updated successfully",
                    Toast.LENGTH_LONG
                ).show()
            }

            finish()
        }
    }
}