package com.example.mvi

import java.util.UUID

// 数据模型
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// 状态
data class TodoState(
    val todos: List<TodoItem> = emptyList(),
    val isLoading: Boolean = false,
    val filter: TodoFilter = TodoFilter.ALL,
    val isAddingTodo: Boolean = false,
    val editingTodo: TodoItem? = null
)

// 过滤器
enum class TodoFilter {
    ALL, ACTIVE, COMPLETED
}

// 动作
sealed class TodoAction {
    data class AddTodo(val title: String, val description: String = "") : TodoAction()
    data class AddTodoInternal(val todo: TodoItem) : TodoAction() // 内部使用，不触发保存
    data class UpdateTodo(val todo: TodoItem) : TodoAction()
    data class DeleteTodo(val todoId: String) : TodoAction()
    data class ToggleTodo(val todoId: String) : TodoAction()
    data class SetFilter(val filter: TodoFilter) : TodoAction()
    data class StartEditing(val todo: TodoItem) : TodoAction()
    object CancelEditing : TodoAction()
    object ClearCompleted : TodoAction()
    object LoadTodos : TodoAction()
    object LoadTodosCompleted : TodoAction()
}

// 副作用
sealed class TodoEffect {
    object SaveTodos : TodoEffect()
    object LoadTodos : TodoEffect()
    data class DeleteTodo(val todoId: String) : TodoEffect()
}

// 事件
sealed class TodoEvent {
    object TodosSaved : TodoEvent()
    object TodosLoaded : TodoEvent()
    data class TodoDeleted(val todoTitle: String) : TodoEvent()
    data class ShowError(val message: String) : TodoEvent()
    data class ShowSuccess(val message: String) : TodoEvent()
}
