package com.greathouse.mvi.middleware

import android.util.Log
import com.greathouse.mvi.Middleware

/**
 * Middleware that logs all actions and state changes.
 * Useful for debugging and development.
 * 
 * @param tag The log tag to use
 * @param logLevel The log level (default: Log.DEBUG)
 * @param logState Whether to log state changes (default: true)
 */
class LoggingMiddleware<State, Action>(
    private val tag: String = "MVI",
    private val logLevel: Int = Log.DEBUG,
    private val logState: Boolean = true
) : Middleware<State, Action> {
    
    override suspend fun process(
        currentState: State,
        action: Action,
        next: suspend (Action) -> State
    ): State {
        val startTime = System.currentTimeMillis()
        
        log("🎯 Action: ${action!!::class.simpleName} - $action")
        
        if (logState) {
            log("📊 Current State: $currentState")
        }
        
        val newState = next(action)
        
        val duration = System.currentTimeMillis() - startTime
        
        if (logState && newState != currentState) {
            log("📈 New State: $newState")
        }
        
        log("⏱️ Processing time: ${duration}ms")
        log("─".repeat(50))
        
        return newState
    }
    
    private fun log(message: String) {
        when (logLevel) {
            Log.VERBOSE -> Log.v(tag, message)
            Log.DEBUG -> Log.d(tag, message)
            Log.INFO -> Log.i(tag, message)
            Log.WARN -> Log.w(tag, message)
            Log.ERROR -> Log.e(tag, message)
        }
    }
}
