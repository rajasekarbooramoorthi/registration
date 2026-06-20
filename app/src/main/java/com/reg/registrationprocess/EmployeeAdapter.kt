package com.reg.registrationprocess

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.reg.registrationprocess.DBUtils.TOKEN_ISSUED_MARKED_AS
import com.reg.registrationprocess.DBUtils.TOKEN_NUMBER
import com.reg.registrationprocess.databinding.AdapterScreenBinding
import com.reg.registrationprocess.interfaces.AdapterClickView

class EmployeeAdapter(
    private val list: List<Map<String, String>>,
    private val listenerInterface: AdapterClickView
) : RecyclerView.Adapter<EmployeeAdapter.Holder>() {

    inner class Holder(val binding: AdapterScreenBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Holder {

        val binding = AdapterScreenBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return Holder(binding)
    }

    override fun onBindViewHolder(
        holder: Holder,
        position: Int
    ) {
        val row = list[position]
        holder.binding.txtData.text =
            row.entries.joinToString("\n") {
                "${it.key} :  ${it.value}"
            }

        holder.binding.checkbox.setOnCheckedChangeListener(null)
        holder.binding.checkbox.isChecked =
            row[TOKEN_ISSUED_MARKED_AS]?.toIntOrNull() == 1
        val safePos = holder.bindingAdapterPosition

        val serialNumber = list[safePos][TOKEN_NUMBER]?.toIntOrNull() ?: 0
        val serialNumberText = (serialNumber + 1).toString()
        val autoInId = list[safePos][TOKEN_NUMBER]?.toInt() ?: 0
        holder.binding.textSerialNumber.text = serialNumberText
        holder.binding.txtEmail.text = list[safePos]["Email"] ?: list[safePos]["email"]
        holder.binding.checkbox.setOnCheckedChangeListener { _, isChecked ->

            val status = if (isChecked) 1 else 0

            val safePos = holder.bindingAdapterPosition
            if (safePos == RecyclerView.NO_POSITION) return@setOnCheckedChangeListener

            val id = list[safePos][TOKEN_NUMBER]?.toIntOrNull()
                ?: return@setOnCheckedChangeListener

            listenerInterface.checkBox(id, status)
        }
        holder.binding.icAdd.setOnClickListener {
            listenerInterface.edit(autoInId)
        }
    }

    override fun getItemCount() = list.size
}