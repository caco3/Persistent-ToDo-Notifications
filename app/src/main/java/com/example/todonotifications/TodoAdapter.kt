package com.example.todonotifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todonotifications.databinding.ItemTodoBinding
import java.text.DateFormat
import java.util.Date

class TodoAdapter(
    private val onItemClick: (TodoItem) -> Unit,
    private val onDeleteClick: (TodoItem) -> Unit
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
            if (todo.isRecurring) {
                binding.iconRecurring.visibility = android.view.View.VISIBLE
                binding.btnDelete.visibility = android.view.View.GONE
            } else {
                binding.iconRecurring.visibility = android.view.View.GONE
                binding.btnDelete.visibility = android.view.View.VISIBLE
                binding.btnDelete.setOnClickListener { onDeleteClick(todo) }
            }
            binding.textTodoTitle.text = todo.title
            binding.textTodoDate.text = if (todo.dtStart > 0L) {
                val date = Date(todo.dtStart)
                val datePart = DateFormat.getDateInstance(DateFormat.SHORT).format(date)
                val timePart = DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
                "$datePart $timePart"
            } else {
                ""
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
