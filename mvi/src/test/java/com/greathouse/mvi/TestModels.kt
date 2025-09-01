package com.greathouse.mvi

/**
 * Test models for MVI testing
 */

// Test State
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

// Test Actions
sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
    data class SetCount(val count: Int) : CounterAction()
    object StartLoading : CounterAction()
    object StopLoading : CounterAction()
    data class SetError(val error: String) : CounterAction()
    object ClearError : CounterAction()
}

// Test Effects
sealed class CounterEffect {
    object SaveCount : CounterEffect()
    object LoadCount : CounterEffect()
    data class ValidateCount(val count: Int) : CounterEffect()
}

// Test Events
sealed class CounterEvent {
    object CountSaved : CounterEvent()
    data class CountLoaded(val count: Int) : CounterEvent()
    data class ValidationFailed(val message: String) : CounterEvent()
    data class ShowError(val message: String) : CounterEvent()
}

// Test Reducer
class CounterReducer : StateReducer<CounterState, CounterAction> {
    override fun reduce(state: CounterState, action: CounterAction): CounterState {
        return when (action) {
            is CounterAction.Increment -> state.copy(count = state.count + 1)
            is CounterAction.Decrement -> state.copy(count = state.count - 1)
            is CounterAction.Reset -> state.copy(count = 0)
            is CounterAction.SetCount -> state.copy(count = action.count)
            is CounterAction.StartLoading -> state.copy(isLoading = true)
            is CounterAction.StopLoading -> state.copy(isLoading = false)
            is CounterAction.SetError -> state.copy(error = action.error)
            is CounterAction.ClearError -> state.copy(error = null)
        }
    }
}

// Test Effect Handler
class CounterEffectHandler : EffectHandler<CounterState, CounterAction, CounterEffect, CounterEvent> {
    
    var shouldFailSave = false
    var shouldFailLoad = false
    var savedCount = 0
    
    override suspend fun handle(
        state: CounterState,
        effect: CounterEffect,
        dispatch: suspend (CounterAction) -> Unit,
        emit: suspend (CounterEvent) -> Unit
    ) {
        when (effect) {
            is CounterEffect.SaveCount -> {
                dispatch(CounterAction.StartLoading)
                try {
                    if (shouldFailSave) {
                        throw Exception("Save failed")
                    }
                    // Simulate save operation
                    kotlinx.coroutines.delay(10)
                    savedCount = state.count
                    emit(CounterEvent.CountSaved)
                } catch (e: Exception) {
                    emit(CounterEvent.ShowError(e.message ?: "Unknown error"))
                } finally {
                    dispatch(CounterAction.StopLoading)
                }
            }
            
            is CounterEffect.LoadCount -> {
                dispatch(CounterAction.StartLoading)
                try {
                    if (shouldFailLoad) {
                        throw Exception("Load failed")
                    }
                    // Simulate load operation
                    kotlinx.coroutines.delay(10)
                    emit(CounterEvent.CountLoaded(savedCount))
                    dispatch(CounterAction.SetCount(savedCount))
                } catch (e: Exception) {
                    emit(CounterEvent.ShowError(e.message ?: "Unknown error"))
                } finally {
                    dispatch(CounterAction.StopLoading)
                }
            }
            
            is CounterEffect.ValidateCount -> {
                if (effect.count < 0) {
                    emit(CounterEvent.ValidationFailed("Count cannot be negative"))
                    dispatch(CounterAction.SetError("Count cannot be negative"))
                } else if (effect.count > 100) {
                    emit(CounterEvent.ValidationFailed("Count cannot exceed 100"))
                    dispatch(CounterAction.SetError("Count cannot exceed 100"))
                }
            }
        }
    }
}

// Test ViewModel
class TestCounterViewModel(
    reducer: StateReducer<CounterState, CounterAction> = CounterReducer(),
    effectHandler: EffectHandler<CounterState, CounterAction, CounterEffect, CounterEvent> = CounterEffectHandler()
) : BaseMVIViewModel<CounterState, CounterAction, CounterEffect, CounterEvent>(reducer, effectHandler) {
    
    override fun initialState(): CounterState = CounterState()
    
    fun increment() = sendAction(CounterAction.Increment)
    fun decrement() = sendAction(CounterAction.Decrement)
    fun reset() = sendAction(CounterAction.Reset)
    fun setCount(count: Int) = sendAction(CounterAction.SetCount(count))
    fun saveCount() = sendEffect(CounterEffect.SaveCount)
    fun loadCount() = sendEffect(CounterEffect.LoadCount)
    fun validateCount(count: Int) = sendEffect(CounterEffect.ValidateCount(count))
}
