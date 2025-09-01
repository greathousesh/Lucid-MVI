package com.example.mvi

import com.greathouse.mvi.StateReducer

class TodoReducer : StateReducer<TodoState, TodoAction> {
    override fun reduce(state: TodoState, action: TodoAction): TodoState {
        return when (action) {
            is TodoAction.AddTodo -> {
                val newTodo = TodoItem(
                    title = action.title,
                    description = action.description
                )
                state.copy(
                    todos = state.todos + newTodo,
                    isAddingTodo = false
                )
            }
            

            is TodoAction.UpdateTodo -> {
                state.copy(
                    todos = state.todos.map { todo ->
                        if (todo.id == action.todo.id) action.todo else todo
                    },
                    editingTodo = null
                )
            }
            
            is TodoAction.DeleteTodo -> {
                state.copy(
                    todos = state.todos.filter { it.id != action.todoId }
                )
            }
            
            is TodoAction.ToggleTodo -> {
                state.copy(
                    todos = state.todos.map { todo ->
                        if (todo.id == action.todoId) {
                            todo.copy(isCompleted = !todo.isCompleted)
                        } else {
                            todo
                        }
                    }
                )
            }
            
            is TodoAction.SetFilter -> {
                state.copy(filter = action.filter)
            }
            
            is TodoAction.StartEditing -> {
                state.copy(editingTodo = action.todo)
            }
            
            is TodoAction.CancelEditing -> {
                state.copy(editingTodo = null)
            }
            
            is TodoAction.ClearCompleted -> {
                state.copy(
                    todos = state.todos.filter { !it.isCompleted }
                )
            }
            
            is TodoAction.LoadTodos -> {
                state.copy(isLoading = true)
            }
            
            is TodoAction.LoadTodosCompleted -> {
                state.copy(
                    isLoading = false,
                    todos = action.todos
                )
            }
        }
    }
}
