package com.example.mvi

import com.greathouse.mvi.EffectHandler
import kotlinx.coroutines.delay

class TodoEffectHandler : EffectHandler<TodoState, TodoAction, TodoEffect, TodoEvent> {
    
    // 模拟数据存储
    private val savedTodos = mutableListOf<TodoItem>()
    
    override suspend fun handle(
        state: TodoState,
        effect: TodoEffect,
        dispatch: suspend (TodoAction) -> Unit,
        emit: suspend (TodoEvent) -> Unit
    ) {
        when (effect) {
            is TodoEffect.SaveTodos -> {
                try {
                    // 模拟保存延迟
                    delay(500)
                    savedTodos.clear()
                    savedTodos.addAll(state.todos)
                    emit(TodoEvent.TodosSaved)
                    emit(TodoEvent.ShowSuccess("Todos saved successfully"))
                } catch (e: Exception) {
                    emit(TodoEvent.ShowError("Failed to save todos: ${e.message}"))
                }
            }
            
            is TodoEffect.LoadTodos -> {
                try {
                    // 模拟加载延迟
                    delay(800)
                    
                    // 如果没有保存的todos，创建一些示例数据
                    if (savedTodos.isEmpty()) {
                        savedTodos.addAll(getSampleTodos())
                    }
                    
                    // 直接将加载的数据通过LoadTodosCompleted返回给state
                    dispatch(TodoAction.LoadTodosCompleted(savedTodos.toList()))
                    emit(TodoEvent.TodosLoaded)
                } catch (e: Exception) {
                    // 发生错误时也要重置loading状态，传入空列表
                    dispatch(TodoAction.LoadTodosCompleted(emptyList()))
                    emit(TodoEvent.ShowError("Failed to load todos: ${e.message}"))
                }
            }
            
            is TodoEffect.DeleteTodo -> {
                try {
                    delay(200)
                    val deletedTodo = state.todos.find { it.id == effect.todoId }
                    if (deletedTodo != null) {
                        savedTodos.removeAll { it.id == effect.todoId }
                        emit(TodoEvent.TodoDeleted(deletedTodo.title))
                    }
                } catch (e: Exception) {
                    emit(TodoEvent.ShowError("Failed to delete todo: ${e.message}"))
                }
            }
        }
    }
    
    private fun getSampleTodos(): List<TodoItem> {
        return listOf(
            TodoItem(
                title = "Learn MVI Architecture",
                description = "Understand the Model-View-Intent pattern",
                isCompleted = true
            ),
            TodoItem(
                title = "Build Todo App",
                description = "Create a comprehensive todo application using MVI",
                isCompleted = false
            ),
            TodoItem(
                title = "Add Compose UI",
                description = "Implement modern UI with Jetpack Compose",
                isCompleted = false
            ),
            TodoItem(
                title = "Write Tests",
                description = "Add unit and integration tests",
                isCompleted = false
            )
        )
    }
}
