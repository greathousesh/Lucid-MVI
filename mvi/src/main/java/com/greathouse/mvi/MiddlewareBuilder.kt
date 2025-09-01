package com.greathouse.mvi

import com.greathouse.mvi.middleware.LoggingMiddleware
import com.greathouse.mvi.middleware.TimeTravelMiddleware
import com.greathouse.mvi.middleware.ValidationMiddleware
import com.greathouse.mvi.middleware.ActionTransformMiddleware
import com.greathouse.mvi.middleware.ActionFilterMiddleware

/**
 * Builder class for creating middleware chains with a fluent API.
 * 
 * Example usage:
 * ```
 * val middleware = MiddlewareBuilder<MyState, MyAction>()
 *     .logging("MyApp")
 *     .timeTravel(maxHistorySize = 50)
 *     .validation { state, action -> validateAction(state, action) }
 *     .build()
 * ```
 */
class MiddlewareBuilder<State, Action> {
    
    private val middlewares = mutableListOf<Middleware<State, Action>>()
    
    /**
     * Add a logging middleware to the chain.
     * 
     * @param tag The log tag to use
     * @param logLevel The log level (default: android.util.Log.DEBUG)
     * @param logState Whether to log state changes (default: true)
     */
    fun logging(
        tag: String = "MVI",
        logLevel: Int = android.util.Log.DEBUG,
        logState: Boolean = true
    ): MiddlewareBuilder<State, Action> {
        middlewares.add(LoggingMiddleware(tag, logLevel, logState))
        return this
    }
    
    /**
     * Add a time travel middleware to the chain.
     * 
     * @param maxHistorySize Maximum number of states to keep in history
     */
    fun timeTravel(maxHistorySize: Int = 100): MiddlewareBuilder<State, Action> {
        middlewares.add(TimeTravelMiddleware(maxHistorySize))
        return this
    }
    
    /**
     * Add a validation middleware to the chain.
     * 
     * @param validator Function that validates actions and returns an error message if invalid
     * @param onValidationError Callback called when validation fails
     */
    fun validation(
        validator: (State, Action) -> String?,
        onValidationError: ((State, Action, String) -> Unit)? = null
    ): MiddlewareBuilder<State, Action> {
        middlewares.add(ValidationMiddleware(validator, onValidationError))
        return this
    }
    
    /**
     * Add an action transformation middleware to the chain.
     * 
     * @param transformer Function that transforms actions
     */
    fun transform(
        transformer: (State, Action) -> Action
    ): MiddlewareBuilder<State, Action> {
        middlewares.add(ActionTransformMiddleware(transformer))
        return this
    }
    
    /**
     * Add an action filter middleware to the chain.
     * 
     * @param predicate Function that determines if an action should be processed
     */
    fun filter(
        predicate: (State, Action) -> Boolean
    ): MiddlewareBuilder<State, Action> {
        middlewares.add(ActionFilterMiddleware(predicate))
        return this
    }
    
    /**
     * Add a custom middleware to the chain.
     * 
     * @param middleware The middleware to add
     */
    fun custom(middleware: Middleware<State, Action>): MiddlewareBuilder<State, Action> {
        middlewares.add(middleware)
        return this
    }
    
    /**
     * Build the middleware chain.
     * 
     * @return A single middleware that represents the entire chain, or null if no middleware was added
     */
    fun build(): Middleware<State, Action>? {
        return when (middlewares.size) {
            0 -> null
            1 -> middlewares.first()
            else -> MiddlewareChain(middlewares.toList())
        }
    }
}

/**
 * Extension function to create a middleware builder with a fluent API.
 */
inline fun <State, Action> buildMiddleware(
    builder: MiddlewareBuilder<State, Action>.() -> Unit
): Middleware<State, Action>? {
    return MiddlewareBuilder<State, Action>().apply(builder).build()
}
