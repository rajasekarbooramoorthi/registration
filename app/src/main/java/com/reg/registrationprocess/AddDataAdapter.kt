package com.reg.registrationprocess


import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.reg.registrationprocess.dataClass.DynamicField
import com.reg.registrationprocess.databinding.AdapterAddDataBinding

class AddDataAdapter(
    private val list: MutableList<DynamicField>
) : RecyclerView.Adapter<AddDataAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: AdapterAddDataBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = AdapterAddDataBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]

        holder.binding.textInputLayout.hint = item.columnName

        val editText = holder.binding.editText

        // 🔥 Remove old listener safely
        editText.setOnFocusChangeListener(null)

        // 🔥 prevent multiple triggers
        val textWatcher = object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    list[holder.bindingAdapterPosition].value = s.toString()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        if (editText.tag is TextWatcher) {
            editText.removeTextChangedListener(editText.tag as TextWatcher)
        }

        editText.addTextChangedListener(textWatcher)
        editText.tag = textWatcher
        editText.setText(item.value)
    }

    override fun getItemCount() = list.size

    fun getFinalData(): Map<String, String> {
        return list.associate { it.columnName to it.value.trim() }
    }
}