package com.greathouse.mvi.middleware

import com.greathouse.mvi.Middleware
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Middleware that enables time-travel debugging by recording all state changes.
 * Allows you to replay actions and navigate through state history.
 * 
 * @param maxHistorySize Maximum number of states to keep in history (default: 100)
 */
class TimeTravelMiddleware<State, Action>(
    private val maxHistorySize: Int = 100
) : Middleware<State, Action> {
    
    private val _history = mutableListOf<StateSnapshot<State, Action>>()
    private val _currentIndex = MutableStateFlow(-1)
    private var _isReplaying = false
    private var _onStateRestore: ((State) -> Unit)? = null
    
    val history: List<StateSnapshot<State, Action>> get() = _history.toList()
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    val isReplaying: Boolean get() = _isReplaying
    
    data class StateSnapshot<State, Action>(
        val state: State,
        val action: Action?,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    override suspend fun process(
        currentState: State,
        action: Action,
        next: suspend (Action) -> State
    ): State {
        if (_isReplaying) {
            return next(action)
        }
        
        val newState = next(action)
        
        // Record the state change
        recordState(newState, action)
        
        return newState
    }
    
    private fun recordState(state: State, action: Action) {
        val snapshot = StateSnapshot(state, action)
        
        // Remove states after current index if we're not at the end
        if (_currentIndex.value < _history.size - 1) {
            _history.subList(_currentIndex.value + 1, _history.size).clear()
        }
        
        _history.add(snapshot)
        
        // Maintain max history size
        if (_history.size > maxHistorySize) {
            _history.removeAt(0)
        } else {
            _currentIndex.value = _history.size - 1
        }
    }
    
    /**
     * Set the callback for state restoration.
     * This should be called by the ViewModel to enable actual state updates.
     */
    fun setStateRestoreCallback(callback: (State) -> Unit) {
        _onStateRestore = callback
    }
    
    /**
     * Record the initial state.
     * This should be called by the ViewModel with the initial state.
     */
    fun recordInitialState(initialState: State) {
        if (_history.isEmpty()) {
            val snapshot = StateSnapshot<State, Action>(initialState, null)
            _history.add(snapshot)
            _currentIndex.value = 0
        }
    }
    
    /**
     * Jump to a specific state in the history.
     * 
     * @param index The index in the history to jump to
     * @return The state at that index, or null if index is invalid
     */
    fun jumpToState(index: Int): State? {
        if (index < 0 || index >= _history.size) return null
        
        _currentIndex.value = index
        val state = _history[index].state
        _onStateRestore?.invoke(state)
        return state
    }
    
    /**
     * Go back one step in the history.
     * 
     * @return The previous state, or null if already at the beginning
     */
    fun stepBack(): State? {
        val newIndex = _currentIndex.value - 1
        return jumpToState(newIndex)
    }
    
    /**
     * Go forward one step in the history.
     * 
     * @return The next state, or null if already at the end
     */
    fun stepForward(): State? {
        val newIndex = _currentIndex.value + 1
        return jumpToState(newIndex)
    }
    
    /**
     * Replay all actions from the beginning.
     * 
     * @param onStateChange Callback called for each state change during replay
     */
    suspend fun replay(onStateChange: suspend (State, Action?) -> Unit) {
        _isReplaying = true
        
        try {
            _history.forEach { snapshot ->
                onStateChange(snapshot.state, snapshot.action)
            }
        } finally {
            _isReplaying = false
        }
    }
    
    /**
     * Clear all history.
     */
    fun clearHistory() {
        _history.clear()
        _currentIndex.value = -1
    }
    
    /**
     * Get the current state from history.
     */
    fun getCurrentState(): State? {
        val index = _currentIndex.value
        return if (index >= 0 && index < _history.size) {
            _history[index].state
        } else null
    }
}
