package com.example.todonotifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todonotifications.databinding.ItemTodoBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodoAdapter(
    private val onItemClick: (TodoItem) -> Unit
) : ListAdapter<TodoItem, TodoAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTodoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTodoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(todo: TodoItem) {
            binding.root.setOnClickListener { onItemClick(todo) }
            binding.textTodoTitle.text = todo.title
            binding.iconRecurring.visibility =
                if (todo.isRecurring) android.view.View.VISIBLE else android.view.View.GONE
            if (todo.dtStart > 0L) {
                val date = Date(todo.dtStart)
                val datePart = SimpleDateFormat("d. MMMM yyyy", Locale.getDefault()).format(date)
                val timePart = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(date)
                binding.textTodoDate.text = "$datePart $timePart"
            } else {
                binding.textTodoDate.text = ""
            }
            if (todo.isRecurring && todo.nextDtStart != null) {
                binding.textTodoNextDate.text = "-> ${SimpleDateFormat("d. MMMM yyyy", Locale.getDefault()).format(Date(todo.nextDtStart))}"
                binding.textTodoNextDate.visibility = android.view.View.VISIBLE
            } else {
                binding.textTodoNextDate.visibility = android.view.View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem) =
            oldItem == newItem
    }
}
