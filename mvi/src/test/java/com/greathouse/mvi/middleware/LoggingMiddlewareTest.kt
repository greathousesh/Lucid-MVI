package com.greathouse.mvi.middleware

import android.util.Log
import com.greathouse.mvi.middleware.LoggingMiddleware
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class LoggingMiddlewareTest {
    
    private data class TestState(val value: Int = 0)
    private sealed class TestAction {
        object Increment : TestAction()
        data class SetValue(val value: Int) : TestAction()
    }
    
    private lateinit var middleware: LoggingMiddleware<TestState, TestAction>
    
    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `should log action with default settings`() = runTest {
        // Given
        middleware = LoggingMiddleware("TestTag")
        val currentState = TestState(5)
        val action = TestAction.Increment
        val nextState = TestState(6)
        val next: suspend (TestAction) -> TestState = { nextState }
        
        // When
        val result = middleware.process(currentState, action, next)
        
        // Then
        assertEquals(nextState, result)
        verify { Log.d("TestTag", match { it.contains("Action: Increment") }) }
        verify { Log.d("TestTag", match { it.contains("Current State: TestState(value=5)") }) }
        verify { Log.d("TestTag", match { it.contains("New State: TestState(value=6)") }) }
        verify { Log.d("TestTag", match { it.contains("Processing time:") }) }
    }
    
    @Test
    fun `should not log state when logState is false`() = runTest {
        // Given
        middleware = LoggingMiddleware("TestTag", logState = false)
        val currentState = TestState(5)
        val action = TestAction.Increment
        val nextState = TestState(6)
        val next: suspend (TestAction) -> TestState = { nextState }
        
        // When
        val result = middleware.process(currentState, action, next)
        
        // Then
        assertEquals(nextState, result)
        verify { Log.d("TestTag", match { it.contains("Action: Increment") }) }
        verify(exactly = 0) { Log.d("TestTag", match { it.contains("Current State:") }) }
        verify(exactly = 0) { Log.d("TestTag", match { it.contains("New State:") }) }
        verify { Log.d("TestTag", match { it.contains("Processing time:") }) }
    }
    
    @Test
    fun `should not log new state when state unchanged`() = runTest {
        // Given
        middleware = LoggingMiddleware("TestTag")
        val currentState = TestState(5)
        val action = TestAction.SetValue(5)
        val nextState = TestState(5) // Same state
        val next: suspend (TestAction) -> TestState = { nextState }
        
        // When
        val result = middleware.process(currentState, action, next)
        
        // Then
        assertEquals(nextState, result)
        verify { Log.d("TestTag", match { it.contains("Action: SetValue") }) }
        verify { Log.d("TestTag", match { it.contains("Current State: TestState(value=5)") }) }
        verify(exactly = 0) { Log.d("TestTag", match { it.contains("New State:") }) }
    }
    
    @Test
    fun `should use custom log level`() = runTest {
        // Given
        middleware = LoggingMiddleware("TestTag", logLevel = Log.INFO)
        val currentState = TestState(5)
        val action = TestAction.Increment
        val nextState = TestState(6)
        val next: suspend (TestAction) -> TestState = { nextState }
        
        // When
        val result = middleware.process(currentState, action, next)
        
        // Then
        assertEquals(nextState, result)
        verify { Log.i("TestTag", match { it.contains("Action: Increment") }) }
        verify(exactly = 0) { Log.d(any(), any()) }
    }
    
    @Test
    fun `should handle exceptions in next function`() = runTest {
        // Given
        middleware = LoggingMiddleware("TestTag")
        val currentState = TestState(5)
        val action = TestAction.Increment
        val exception = RuntimeException("Test exception")
        val next: suspend (TestAction) -> TestState = { throw exception }
        
        // When & Then
        try {
            middleware.process(currentState, action, next)
            fail("Expected exception to be thrown")
        } catch (e: RuntimeException) {
            assertEquals("Test exception", e.message)
            verify { Log.d("TestTag", match { it.contains("Action: Increment") }) }
        }
    }
}
