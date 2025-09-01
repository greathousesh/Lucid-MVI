package com.greathouse.mvi

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Android test models and utilities for MVI testing
 */

// Test State for Android tests
data class AndroidCounterState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

// Test Actions for Android tests
sealed class AndroidCounterAction {
    object Increment : AndroidCounterAction()
    object Decrement : AndroidCounterAction()
    object Reset : AndroidCounterAction()
    data class SetCount(val count: Int) : AndroidCounterAction()
    object StartLoading : AndroidCounterAction()
    object StopLoading : AndroidCounterAction()
    data class SetError(val error: String) : AndroidCounterAction()
    object ClearError : AndroidCounterAction()
    object UpdateTimestamp : AndroidCounterAction()
}

// Test Effects for Android tests
sealed class AndroidCounterEffect {
    object SaveToPreferences : AndroidCounterEffect()
    object LoadFromPreferences : AndroidCounterEffect()
    data class SaveToDatabase(val count: Int) : AndroidCounterEffect()
    object LoadFromDatabase : AndroidCounterEffect()
    data class ShowToast(val message: String) : AndroidCounterEffect()
    object NetworkSync : AndroidCounterEffect()
}

// Test Events for Android tests
sealed class AndroidCounterEvent {
    object CountSavedToPreferences : AndroidCounterEvent()
    data class CountLoadedFromPreferences(val count: Int) : AndroidCounterEvent()
    object CountSavedToDatabase : AndroidCounterEvent()
    data class CountLoadedFromDatabase(val count: Int) : AndroidCounterEvent()
    data class ToastShown(val message: String) : AndroidCounterEvent()
    object NetworkSyncCompleted : AndroidCounterEvent()
    data class NetworkSyncFailed(val error: String) : AndroidCounterEvent()
    data class ShowError(val message: String) : AndroidCounterEvent()
}

// Android Test Reducer
class AndroidCounterReducer : StateReducer<AndroidCounterState, AndroidCounterAction> {
    override fun reduce(state: AndroidCounterState, action: AndroidCounterAction): AndroidCounterState {
        return when (action) {
            is AndroidCounterAction.Increment -> state.copy(
                count = state.count + 1,
                lastUpdated = System.currentTimeMillis()
            )
            is AndroidCounterAction.Decrement -> state.copy(
                count = state.count - 1,
                lastUpdated = System.currentTimeMillis()
            )
            is AndroidCounterAction.Reset -> state.copy(
                count = 0,
                lastUpdated = System.currentTimeMillis()
            )
            is AndroidCounterAction.SetCount -> state.copy(
                count = action.count,
                lastUpdated = System.currentTimeMillis()
            )
            is AndroidCounterAction.StartLoading -> state.copy(isLoading = true)
            is AndroidCounterAction.StopLoading -> state.copy(isLoading = false)
            is AndroidCounterAction.SetError -> state.copy(error = action.error)
            is AndroidCounterAction.ClearError -> state.copy(error = null)
            is AndroidCounterAction.UpdateTimestamp -> state.copy(lastUpdated = System.currentTimeMillis())
        }
    }
}

// Android Test Effect Handler
class AndroidCounterEffectHandler : EffectHandler<AndroidCounterState, AndroidCounterAction, AndroidCounterEffect, AndroidCounterEvent> {
    
    var shouldFailPreferences = false
    var shouldFailDatabase = false
    var shouldFailNetwork = false
    var preferencesCount = 0
    var databaseCount = 0
    
    override suspend fun handle(
        state: AndroidCounterState,
        effect: AndroidCounterEffect,
        dispatch: suspend (AndroidCounterAction) -> Unit,
        emit: suspend (AndroidCounterEvent) -> Unit
    ) {
        when (effect) {
            is AndroidCounterEffect.SaveToPreferences -> {
                dispatch(AndroidCounterAction.StartLoading)
                try {
                    if (shouldFailPreferences) {
                        throw Exception("Preferences save failed")
                    }
                    // Simulate preferences save
                    kotlinx.coroutines.delay(50)
                    preferencesCount = state.count
                    emit(AndroidCounterEvent.CountSavedToPreferences)
                } catch (e: Exception) {
                    emit(AndroidCounterEvent.ShowError(e.message ?: "Preferences error"))
                } finally {
                    dispatch(AndroidCounterAction.StopLoading)
                }
            }
            
            is AndroidCounterEffect.LoadFromPreferences -> {
                dispatch(AndroidCounterAction.StartLoading)
                try {
                    if (shouldFailPreferences) {
                        throw Exception("Preferences load failed")
                    }
                    // Simulate preferences load
                    kotlinx.coroutines.delay(50)
                    emit(AndroidCounterEvent.CountLoadedFromPreferences(preferencesCount))
                    dispatch(AndroidCounterAction.SetCount(preferencesCount))
                } catch (e: Exception) {
                    emit(AndroidCounterEvent.ShowError(e.message ?: "Preferences error"))
                } finally {
                    dispatch(AndroidCounterAction.StopLoading)
                }
            }
            
            is AndroidCounterEffect.SaveToDatabase -> {
                dispatch(AndroidCounterAction.StartLoading)
                try {
                    if (shouldFailDatabase) {
                        throw Exception("Database save failed")
                    }
                    // Simulate database save
                    kotlinx.coroutines.delay(100)
                    databaseCount = effect.count
                    emit(AndroidCounterEvent.CountSavedToDatabase)
                } catch (e: Exception) {
                    emit(AndroidCounterEvent.ShowError(e.message ?: "Database error"))
                } finally {
                    dispatch(AndroidCounterAction.StopLoading)
                }
            }
            
            is AndroidCounterEffect.LoadFromDatabase -> {
                dispatch(AndroidCounterAction.StartLoading)
                try {
                    if (shouldFailDatabase) {
                        throw Exception("Database load failed")
                    }
                    // Simulate database load
                    kotlinx.coroutines.delay(100)
                    emit(AndroidCounterEvent.CountLoadedFromDatabase(databaseCount))
                    dispatch(AndroidCounterAction.SetCount(databaseCount))
                } catch (e: Exception) {
                    emit(AndroidCounterEvent.ShowError(e.message ?: "Database error"))
                } finally {
                    dispatch(AndroidCounterAction.StopLoading)
                }
            }
            
            is AndroidCounterEffect.ShowToast -> {
                // Simulate toast showing
                kotlinx.coroutines.delay(10)
                emit(AndroidCounterEvent.ToastShown(effect.message))
            }
            
            is AndroidCounterEffect.NetworkSync -> {
                dispatch(AndroidCounterAction.StartLoading)
                try {
                    if (shouldFailNetwork) {
                        throw Exception("Network sync failed")
                    }
                    // Simulate network operation
                    kotlinx.coroutines.delay(200)
                    emit(AndroidCounterEvent.NetworkSyncCompleted)
                } catch (e: Exception) {
                    emit(AndroidCounterEvent.NetworkSyncFailed(e.message ?: "Network error"))
                } finally {
                    dispatch(AndroidCounterAction.StopLoading)
                }
            }
        }
    }
}

// Android Test ViewModel
class AndroidTestCounterViewModel(
    reducer: StateReducer<AndroidCounterState, AndroidCounterAction> = AndroidCounterReducer(),
    effectHandler: EffectHandler<AndroidCounterState, AndroidCounterAction, AndroidCounterEffect, AndroidCounterEvent> = AndroidCounterEffectHandler()
) : BaseMVIViewModel<AndroidCounterState, AndroidCounterAction, AndroidCounterEffect, AndroidCounterEvent>(reducer, effectHandler) {
    
    override fun initialState(): AndroidCounterState = AndroidCounterState()
    
    // Action methods
    fun increment() = sendAction(AndroidCounterAction.Increment)
    fun decrement() = sendAction(AndroidCounterAction.Decrement)
    fun reset() = sendAction(AndroidCounterAction.Reset)
    fun setCount(count: Int) = sendAction(AndroidCounterAction.SetCount(count))
    fun updateTimestamp() = sendAction(AndroidCounterAction.UpdateTimestamp)
    
    // Effect methods
    fun saveToPreferences() = sendEffect(AndroidCounterEffect.SaveToPreferences)
    fun loadFromPreferences() = sendEffect(AndroidCounterEffect.LoadFromPreferences)
    fun saveToDatabase(count: Int) = sendEffect(AndroidCounterEffect.SaveToDatabase(count))
    fun loadFromDatabase() = sendEffect(AndroidCounterEffect.LoadFromDatabase)
    fun showToast(message: String) = sendEffect(AndroidCounterEffect.ShowToast(message))
    fun syncWithNetwork() = sendEffect(AndroidCounterEffect.NetworkSync)
}

// Test utility for collecting states and events
class StateEventCollector<State, Event>(
    private val viewModel: BaseMVIViewModel<State, *, *, Event>,
    private val lifecycleOwner: LifecycleOwner
) {
    private val _states = mutableListOf<State>()
    private val _events = mutableListOf<Event>()
    
    val states: List<State> get() = _states.toList()
    val events: List<Event> get() = _events.toList()
    
    fun startCollecting() {
        lifecycleOwner.lifecycleScope.launch {
            viewModel.stateFlow.collect { state ->
                _states.add(state)
            }
        }
        
        lifecycleOwner.lifecycleScope.launch {
            viewModel.eventFlow.collect { event ->
                _events.add(event)
            }
        }
    }
    
    fun clearCollections() {
        _states.clear()
        _events.clear()
    }
}
