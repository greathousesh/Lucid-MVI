package com.greathouse.mvi.middleware

import com.greathouse.mvi.Middleware

/**
 * Middleware that validates actions before they are processed.
 * Can prevent invalid actions from reaching the reducer.
 * 
 * @param validator Function that validates actions and returns an error message if invalid
 * @param onValidationError Callback called when validation fails
 */
class ValidationMiddleware<State, Action>(
    private val validator: (State, Action) -> String?,
    private val onValidationError: ((State, Action, String) -> Unit)? = null
) : Middleware<State, Action> {
    
    override suspend fun process(
        currentState: State,
        action: Action,
        next: suspend (Action) -> State
    ): State {
        val errorMessage = validator(currentState, action)
        
        return if (errorMessage != null) {
            // Validation failed
            onValidationError?.invoke(currentState, action, errorMessage)
            currentState // Return current state unchanged
        } else {
            // Validation passed
            next(action)
        }
    }
}

/**
 * Middleware that transforms actions before they reach the reducer.
 * Useful for normalizing data or converting between action types.
 * 
 * @param transformer Function that transforms actions
 */
class ActionTransformMiddleware<State, Action>(
    private val transformer: (State, Action) -> Action
) : Middleware<State, Action> {
    
    override suspend fun process(
        currentState: State,
        action: Action,
        next: suspend (Action) -> State
    ): State {
        val transformedAction = transformer(currentState, action)
        return next(transformedAction)
    }
}

/**
 * Middleware that filters actions based on a predicate.
 * Actions that don't match the predicate are ignored.
 * 
 * @param predicate Function that determines if an action should be processed
 */
class ActionFilterMiddleware<State, Action>(
    private val predicate: (State, Action) -> Boolean
) : Middleware<State, Action> {
    
    override suspend fun process(
        currentState: State,
        action: Action,
        next: suspend (Action) -> State
    ): State {
        return if (predicate(currentState, action)) {
            next(action)
        } else {
            currentState // Return current state unchanged
        }
    }
}
