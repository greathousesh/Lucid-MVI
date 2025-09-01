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
    private val effectHandler: EffectHandler<State, Action, Effect, Event>
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
    }

    protected abstract fun initialState(): State

    private suspend fun handleAction(action: Action) {
        setState { reducer.reduce(this, action) }
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

    protected suspend fun setState(reducer: State.() -> State) {
        val newState = latestState.reducer()
        _stateFlow.emit(newState)
    }

    fun sendAction(action: Action) {
        viewModelScope.launch { actionChannel.send(action) }
    }

    fun sendEffect(effect: Effect) {
        viewModelScope.launch { effectChannel.send(effect) }
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