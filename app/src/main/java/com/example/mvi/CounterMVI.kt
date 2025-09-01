package com.example.mvi

import com.greathouse.mvi.EffectHandler
import com.greathouse.mvi.StateReducer
import kotlinx.coroutines.delay

// State
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false
)

// Actions
sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
    object StartSaving : CounterAction()
    object FinishSaving : CounterAction()
}

// Effects
sealed class CounterEffect {
    object SaveCount : CounterEffect()
}

// Events
sealed class CounterEvent {
    object CountSaved : CounterEvent()
    data class ShowError(val message: String) : CounterEvent()
}

// Reducer
class CounterReducer : StateReducer<CounterState, CounterAction> {
    override fun reduce(state: CounterState, action: CounterAction): CounterState {
        return when (action) {
            is CounterAction.Increment -> state.copy(count = state.count + 1)
            is CounterAction.Decrement -> state.copy(count = state.count - 1)
            is CounterAction.Reset -> state.copy(count = 0)
            is CounterAction.StartSaving -> state.copy(isLoading = true)
            is CounterAction.FinishSaving -> state.copy(isLoading = false)
        }
    }
}

// Effect Handler
class CounterEffectHandler : EffectHandler<CounterState, CounterAction, CounterEffect, CounterEvent> {
    override suspend fun handle(
        state: CounterState,
        effect: CounterEffect,
        dispatch: suspend (CounterAction) -> Unit,
        emit: suspend (CounterEvent) -> Unit
    ) {
        when (effect) {
            is CounterEffect.SaveCount -> {
                try {
                    dispatch(CounterAction.StartSaving)
                    // 模拟网络请求
                    delay(2000)
                    // 模拟保存成功
                    dispatch(CounterAction.FinishSaving)
                    emit(CounterEvent.CountSaved)
                } catch (e: Exception) {
                    dispatch(CounterAction.FinishSaving)
                    emit(CounterEvent.ShowError(e.message ?: "Unknown error"))
                }
            }
        }
    }
}
