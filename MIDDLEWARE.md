# Lucid MVI Middleware

Middleware provides a powerful way to intercept and process actions in your MVI flow. This enables features like logging, time-travel debugging, validation, and analytics tracking.

## Overview

Middleware sits between action dispatch and the reducer, allowing you to:
- Log actions and state changes
- Implement time-travel debugging
- Validate actions before processing
- Transform actions
- Filter actions
- Add analytics tracking

## Basic Usage

### Creating a ViewModel with Middleware

```kotlin
class MyViewModel : BaseMVIViewModel<MyState, MyAction, MyEffect, MyEvent>(
    reducer = MyReducer(),
    effectHandler = MyEffectHandler(),
    middleware = buildMiddleware {
        logging("MyApp")
        timeTravel(maxHistorySize = 50)
        validation { state, action -> validateAction(state, action) }
    }
)
```

### Using Individual Middleware

```kotlin
// Logging middleware
val loggingMiddleware = LoggingMiddleware<MyState, MyAction>(
    tag = "MyApp",
    logLevel = Log.DEBUG,
    logState = true
)

// Time travel middleware
val timeTravelMiddleware = TimeTravelMiddleware<MyState, MyAction>(
    maxHistorySize = 100
)

// Validation middleware
val validationMiddleware = ValidationMiddleware<MyState, MyAction>(
    validator = { state, action ->
        when (action) {
            is MyAction.InvalidAction -> "This action is not allowed"
            else -> null
        }
    },
    onValidationError = { state, action, error ->
        Log.w("MyApp", "Validation failed: $error")
    }
)
```

## Built-in Middleware

### 1. LoggingMiddleware

Logs all actions and state changes for debugging purposes.

```kotlin
middleware = buildMiddleware {
    logging(
        tag = "MyApp",           // Log tag
        logLevel = Log.DEBUG,    // Log level
        logState = true          // Whether to log state changes
    )
}
```

### 2. TimeTravelMiddleware

Enables time-travel debugging by recording state history.

```kotlin
middleware = buildMiddleware {
    timeTravel(maxHistorySize = 50)
}

// Access time travel functionality
val timeTravelMiddleware = viewModel.getMiddleware() as? TimeTravelMiddleware
timeTravelMiddleware?.stepBack()        // Go back one step
timeTravelMiddleware?.stepForward()     // Go forward one step
timeTravelMiddleware?.jumpToState(5)    // Jump to specific state
timeTravelMiddleware?.clearHistory()    // Clear history
```

### 3. ValidationMiddleware

Validates actions before they reach the reducer.

```kotlin
middleware = buildMiddleware {
    validation(
        validator = { state, action ->
            when (action) {
                is MyAction.AddItem -> {
                    if (action.item.name.isBlank()) "Item name cannot be empty"
                    else null
                }
                else -> null
            }
        },
        onValidationError = { state, action, error ->
            // Handle validation error
            Log.w("MyApp", "Action validation failed: $error")
        }
    )
}
```

### 4. ActionTransformMiddleware

Transforms actions before they reach the reducer.

```kotlin
middleware = buildMiddleware {
    transform { state, action ->
        when (action) {
            is MyAction.LegacyAction -> MyAction.NewAction(action.data)
            else -> action
        }
    }
}
```

### 5. ActionFilterMiddleware

Filters actions based on a predicate.

```kotlin
middleware = buildMiddleware {
    filter { state, action ->
        when (action) {
            is MyAction.RestrictedAction -> state.isUserAuthorized
            else -> true
        }
    }
}
```

## Custom Middleware

You can create custom middleware by implementing the `Middleware` interface:

```kotlin
class AnalyticsMiddleware<State, Action> : Middleware<State, Action> {
    override suspend fun process(
        currentState: State,
        action: Action,
        next: suspend (Action) -> State
    ): State {
        // Track action in analytics
        Analytics.track("action_dispatched", mapOf(
            "action_type" to action::class.simpleName,
            "timestamp" to System.currentTimeMillis()
        ))
        
        val newState = next(action)
        
        // Track state change if needed
        if (newState != currentState) {
            Analytics.track("state_changed")
        }
        
        return newState
    }
}

// Use custom middleware
middleware = buildMiddleware {
    custom(AnalyticsMiddleware())
    logging("MyApp")
}
```

## Middleware Chain

Middleware are executed in the order they are added to the chain:

```kotlin
middleware = buildMiddleware {
    logging("MyApp")           // Executes first
    validation { ... }         // Executes second
    transform { ... }          // Executes third
    timeTravel()              // Executes last
}
```

## Time Travel Debugging

The `TimeTravelMiddleware` provides powerful debugging capabilities:

### Basic Time Travel

```kotlin
class MyViewModel : BaseMVIViewModel<...>(...) {
    fun getTimeTravelMiddleware(): TimeTravelMiddleware<MyState, MyAction>? {
        return getMiddleware() as? TimeTravelMiddleware<MyState, MyAction>
    }
    
    fun stepBack(): MyState? = getTimeTravelMiddleware()?.stepBack()
    fun stepForward(): MyState? = getTimeTravelMiddleware()?.stepForward()
    fun jumpToState(index: Int): MyState? = getTimeTravelMiddleware()?.jumpToState(index)
}
```

### Accessing History

```kotlin
val history = viewModel.getTimeTravelMiddleware()?.history ?: emptyList()
history.forEach { snapshot ->
    println("Action: ${snapshot.action}, State: ${snapshot.state}, Time: ${snapshot.timestamp}")
}
```

### Replay Actions

```kotlin
viewModel.getTimeTravelMiddleware()?.replay { state, action ->
    println("Replaying: $action -> $state")
}
```

## Best Practices

1. **Order Matters**: Place validation middleware early in the chain to prevent invalid actions from being processed.

2. **Performance**: Be mindful of middleware performance, especially in production builds. Consider disabling debug middleware in release builds.

3. **State Immutability**: Middleware should not modify the state directly. Always use the `next` function to process actions.

4. **Error Handling**: Implement proper error handling in custom middleware to prevent crashes.

5. **Testing**: Test middleware separately from your ViewModels to ensure they work correctly in isolation.

## Example: Complete Setup

```kotlin
class TodoViewModel : BaseMVIViewModel<TodoState, TodoAction, TodoEffect, TodoEvent>(
    reducer = TodoReducer(),
    effectHandler = TodoEffectHandler(),
    middleware = buildMiddleware {
        // Log all actions and state changes
        logging("TodoMVI", Log.DEBUG, true)
        
        // Enable time travel debugging
        timeTravel(maxHistorySize = 50)
        
        // Validate actions
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
        
        // Filter actions based on state
        filter { state, action ->
            when (action) {
                is TodoAction.DeleteTodo -> !state.isLoading
                else -> true
            }
        }
    }
) {
    // ViewModel implementation...
}
```

This setup provides comprehensive debugging and validation capabilities while maintaining clean separation of concerns.
