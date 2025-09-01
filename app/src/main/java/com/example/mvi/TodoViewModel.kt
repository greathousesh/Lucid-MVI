package com.example.mvi

import com.greathouse.mvi.BaseMVIViewModel

class TodoViewModel : BaseMVIViewModel<TodoState, TodoAction, TodoEffect, TodoEvent>(
    reducer = TodoReducer(),
    effectHandler = TodoEffectHandler()
) {
    override fun initialState(): TodoState = TodoState()
    
    // 便捷方法
    fun addTodo(title: String, description: String = "") {
        if (title.isNotBlank()) {
            sendAction(TodoAction.AddTodo(title.trim(), description.trim()))
            sendEffect(TodoEffect.SaveTodos)
        }
    }
    
    fun updateTodo(todo: TodoItem) {
        sendAction(TodoAction.UpdateTodo(todo))
        sendEffect(TodoEffect.SaveTodos)
    }
    
    fun deleteTodo(todoId: String) {
        sendAction(TodoAction.DeleteTodo(todoId))
        sendEffect(TodoEffect.DeleteTodo(todoId))
        sendEffect(TodoEffect.SaveTodos)
    }
    
    fun toggleTodo(todoId: String) {
        sendAction(TodoAction.ToggleTodo(todoId))
        sendEffect(TodoEffect.SaveTodos)
    }
    
    fun setFilter(filter: TodoFilter) {
        sendAction(TodoAction.SetFilter(filter))
    }
    
    fun startEditing(todo: TodoItem) {
        sendAction(TodoAction.StartEditing(todo))
    }
    
    fun cancelEditing() {
        sendAction(TodoAction.CancelEditing)
    }
    
    fun clearCompleted() {
        sendAction(TodoAction.ClearCompleted)
        sendEffect(TodoEffect.SaveTodos)
    }
    
    fun loadTodos() {
        sendAction(TodoAction.LoadTodos)
        sendEffect(TodoEffect.LoadTodos)
    }
    
    // 计算属性
    fun getFilteredTodos(state: TodoState): List<TodoItem> {
        return when (state.filter) {
            TodoFilter.ALL -> state.todos
            TodoFilter.ACTIVE -> state.todos.filter { !it.isCompleted }
            TodoFilter.COMPLETED -> state.todos.filter { it.isCompleted }
        }
    }
    
    fun getStats(state: TodoState): TodoStats {
        val total = state.todos.size
        val completed = state.todos.count { it.isCompleted }
        val active = total - completed
        return TodoStats(total, active, completed)
    }
}

data class TodoStats(
    val total: Int,
    val active: Int,
    val completed: Int
)
