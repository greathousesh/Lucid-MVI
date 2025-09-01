package com.greathouse.mvi

/**
 * Middleware interface for intercepting and processing actions in the MVI flow.
 * 
 * Middleware allows you to:
 * - Log actions and state changes
 * - Implement time-travel debugging
 * - Add analytics tracking
 * - Validate actions
 * - Transform actions before they reach the reducer
 * 
 * @param State The state type
 * @param Action The action type
 */
interface Middleware<State, Action> {
    
    /**
     * Process an action before it reaches the reducer.
     * 
     * @param currentState The current state before the action is processed
     * @param action The action being processed
     * @param next Function to continue processing the action through the chain
     * @return The resulting state after processing
     */
    suspend fun process(
        currentState: State,
        action: Action,
        next: suspend (Action) -> State
    ): State
}

/**
 * A simple middleware that just passes the action through without modification.
 * Useful as a base class or for testing.
 */
class PassThroughMiddleware<State, Action> : Middleware<State, Action> {
    override suspend fun process(
        currentState: State,
        action: Action,
        next: suspend (Action) -> State
    ): State = next(action)
}

/**
 * Combines multiple middleware into a single middleware chain.
 * Middleware are executed in the order they are provided.
 */
class MiddlewareChain<State, Action>(
    private val middlewares: List<Middleware<State, Action>>
) : Middleware<State, Action> {
    
    override suspend fun process(
        currentState: State,
        action: Action,
        next: suspend (Action) -> State
    ): State {
        return processWithIndex(0, currentState, action, next)
    }
    
    private suspend fun processWithIndex(
        index: Int,
        currentState: State,
        action: Action,
        finalNext: suspend (Action) -> State
    ): State {
        return if (index >= middlewares.size) {
            finalNext(action)
        } else {
            middlewares[index].process(currentState, action) { nextAction ->
                processWithIndex(index + 1, currentState, nextAction, finalNext)
            }
        }
    }
}
