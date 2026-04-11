package com.example.todonotifications

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todonotifications.databinding.ItemTodoBinding

class TodoAdapter(
    private val onComplete: (TodoItem) -> Unit,
    private val onDelete: (TodoItem) -> Unit
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
            binding.textTodoTitle.text = todo.title

            if (todo.isCompleted) {
                binding.textTodoTitle.paintFlags =
                    binding.textTodoTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.textTodoTitle.alpha = 0.45f
                binding.checkboxComplete.isChecked = true
                binding.checkboxComplete.isEnabled = false
            } else {
                binding.textTodoTitle.paintFlags =
                    binding.textTodoTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.textTodoTitle.alpha = 1.0f
                binding.checkboxComplete.isChecked = false
                binding.checkboxComplete.isEnabled = true
                binding.checkboxComplete.setOnClickListener { onComplete(todo) }
            }

            binding.btnDelete.setOnClickListener { onDelete(todo) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem) =
            oldItem == newItem
    }
}
