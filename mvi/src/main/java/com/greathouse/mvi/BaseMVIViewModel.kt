package com.greathouse.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseMVIViewModel<State, Action, Effect, Event>(
    private val reducer: StateReducer<State, Action>,
    private val effectHandler: EffectHandler<State, Action, Effect, Event>,
    private val middleware: Middleware<State, Action>? = null
) : ViewModel() {

    private val _stateFlow = MutableStateFlow(initialState())
    val stateFlow: StateFlow<State> = _stateFlow

    private val actionChannel = Channel<Action>(Channel.UNLIMITED)
    private val effectChannel = Channel<Effect>(Channel.UNLIMITED)

    private val _eventFlow = MutableSharedFlow<Event>()
    val eventFlow: SharedFlow<Event> = _eventFlow.asSharedFlow()

    protected val latestState: State
        get() = _stateFlow.value

    init {
        viewModelScope.launch {
            for (action in actionChannel) {
                handleAction(action)
            }
        }

        viewModelScope.launch {
            for (effect in effectChannel) {
                handleEffect(effect)
            }
        }
        
        // Setup time travel middleware if available
        setupTimeTravelMiddleware()
    }

    protected abstract fun initialState(): State

    private suspend fun handleAction(action: Action) {
        val newState = if (middleware != null) {
            middleware.process(latestState, action) { processedAction ->
                reducer.reduce(latestState, processedAction)
            }
        } else {
            reducer.reduce(latestState, action)
        }
        
        _stateFlow.emit(newState)
    }

    private suspend fun handleEffect(effect: Effect) {
        withContext(Dispatchers.IO) {
            effectHandler.handle(
                state = latestState,
                effect = effect,
                dispatch = { action -> actionChannel.send(action) },
                emit = { effect -> _eventFlow.emit(effect) }
            )
        }
    }



    fun sendAction(action: Action) {
        viewModelScope.launch { actionChannel.send(action) }
    }

    fun sendEffect(effect: Effect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
    
    /**
     * Get the middleware instance if available.
     * Useful for accessing middleware-specific functionality like time travel debugging.
     */
    fun getMiddleware(): Middleware<State, Action>? = middleware
    
    /**
     * Setup time travel middleware if available.
     */
    private fun setupTimeTravelMiddleware() {
        val timeTravelMiddleware = findTimeTravelMiddleware(middleware)
        timeTravelMiddleware?.let { ttm ->
            ttm.setStateRestoreCallback { state ->
                viewModelScope.launch {
                    _stateFlow.emit(state)
                }
            }
            // Record initial state
            ttm.recordInitialState(initialState())
        }
    }
    
    /**
     * Recursively find TimeTravelMiddleware in the middleware chain.
     */
    private fun findTimeTravelMiddleware(middleware: Middleware<State, Action>?): com.greathouse.mvi.middleware.TimeTravelMiddleware<State, Action>? {
        return when (middleware) {
            is com.greathouse.mvi.middleware.TimeTravelMiddleware<*, *> -> middleware as com.greathouse.mvi.middleware.TimeTravelMiddleware<State, Action>
            is MiddlewareChain<*, *> -> {
                // Search through the chain
                val chain = middleware as MiddlewareChain<State, Action>
                chain.getMiddlewares().firstNotNullOfOrNull { findTimeTravelMiddleware(it) }
            }
            else -> null
        }
    }

    override fun onCleared() {
        super.onCleared()
        actionChannel.close()
        effectChannel.close()
    }
}

interface StateReducer<State, Action> {
    fun reduce(state: State, action: Action): State
}

interface EffectHandler<State, Action, Effect, Event> {
    suspend fun handle(
        state: State,
        effect: Effect,
        dispatch: suspend (Action) -> Unit,
        emit: suspend (Event) -> Unit
    )
}