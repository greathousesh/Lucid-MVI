package com.example.mvi

import android.util.Log
import com.greathouse.mvi.BaseMVIViewModel
import com.greathouse.mvi.buildMiddleware
import com.greathouse.mvi.middleware.TimeTravelMiddleware

class TodoViewModel : BaseMVIViewModel<TodoState, TodoAction, TodoEffect, TodoEvent>(
    reducer = TodoReducer(),
    effectHandler = TodoEffectHandler(),
    middleware = buildMiddleware {
        logging("TodoMVI", Log.DEBUG, true)
        timeTravel(maxHistorySize = 50)
        validation({ state, action ->
            when (action) {
                is TodoAction.AddTodo -> {
                    if (action.title.isBlank()) "Todo title cannot be empty" else null
                }
                is TodoAction.UpdateTodo -> {
                    if (action.todo.title.isBlank()) "Todo title cannot be empty" else null
                }
                else -> null
            }
        }, { _, action, error ->
            Log.w("TodoMVI", "Action validation failed: $action - $error")
        })
    }
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
    
    // Time Travel Debugging Methods
    fun getTimeTravelMiddleware(): TimeTravelMiddleware<TodoState, TodoAction>? {
        val middleware = getMiddleware()
        return when (middleware) {
            is TimeTravelMiddleware<*, *> -> middleware as TimeTravelMiddleware<TodoState, TodoAction>
            is com.greathouse.mvi.MiddlewareChain<*, *> -> {
                val chain = middleware as com.greathouse.mvi.MiddlewareChain<TodoState, TodoAction>
                chain.getMiddlewares().firstNotNullOfOrNull { 
                    it as? TimeTravelMiddleware<TodoState, TodoAction>
                }
            }
            else -> null
        }
    }
    
    fun stepBack(): TodoState? {
        return getTimeTravelMiddleware()?.stepBack()
    }
    
    fun stepForward(): TodoState? {
        return getTimeTravelMiddleware()?.stepForward()
    }
    
    fun jumpToState(index: Int): TodoState? {
        return getTimeTravelMiddleware()?.jumpToState(index)
    }
    
    fun getHistory(): List<TimeTravelMiddleware.StateSnapshot<TodoState, TodoAction>> {
        return getTimeTravelMiddleware()?.history ?: emptyList()
    }
    
    fun clearHistory() {
        getTimeTravelMiddleware()?.clearHistory()
    }
}

data class TodoStats(
    val total: Int,
    val active: Int,
    val completed: Int
)
