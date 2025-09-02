package com.greathouse.mvi.middleware

import com.greathouse.mvi.middleware.TimeTravelMiddleware
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class TimeTravelMiddlewareTest {
    
    private data class TestState(val value: Int = 0)
    private sealed class TestAction {
        object Increment : TestAction()
        object Decrement : TestAction()
        data class SetValue(val value: Int) : TestAction()
    }
    
    private lateinit var middleware: TimeTravelMiddleware<TestState, TestAction>
    
    @Before
    fun setup() {
        middleware = TimeTravelMiddleware(maxHistorySize = 5)
    }
    
    @Test
    fun `should record initial state`() = runTest {
        // Given
        val initialState = TestState(0)
        
        // When
        middleware.recordInitialState(initialState)
        
        // Then
        val history = middleware.history
        assertEquals(1, history.size)
        assertEquals(initialState, history[0].state)
        assertNull(history[0].action)
        assertEquals(0, middleware.currentIndex.value)
    }
    
    @Test
    fun `should record state changes through process`() = runTest {
        // Given
        val initialState = TestState(0)
        val action = TestAction.Increment
        val newState = TestState(1)
        val next: suspend (TestAction) -> TestState = { newState }
        
        middleware.recordInitialState(initialState)
        
        // When
        val result = middleware.process(initialState, action, next)
        
        // Then
        assertEquals(newState, result)
        val history = middleware.history
        assertEquals(2, history.size)
        assertEquals(initialState, history[0].state)
        assertEquals(newState, history[1].state)
        assertEquals(action, history[1].action)
        assertEquals(1, middleware.currentIndex.value)
    }
    
    @Test
    fun `should maintain max history size`() = runTest {
        // Given
        val initialState = TestState(0)
        middleware.recordInitialState(initialState)
        val next: suspend (TestAction) -> TestState = { TestState(it.hashCode()) }
        
        // When - Add more than max history size
        repeat(6) { i ->
            middleware.process(TestState(i), TestAction.SetValue(i), next)
        }
        
        // Then
        val history = middleware.history
        assertEquals(5, history.size) // Should not exceed maxHistorySize
    }
    
    @Test
    fun `should jump to specific state`() = runTest {
        // Given
        val states = listOf(TestState(0), TestState(1), TestState(2))
        var restoredState: TestState? = null
        
        middleware.setStateRestoreCallback { state -> restoredState = state }
        middleware.recordInitialState(states[0])
        
        val next: suspend (TestAction) -> TestState = { action ->
            when (action) {
                is TestAction.SetValue -> TestState(action.value)
                else -> TestState(0)
            }
        }
        
        // Build history
        middleware.process(states[0], TestAction.SetValue(1), next)
        middleware.process(states[1], TestAction.SetValue(2), next)
        
        // When
        val result = middleware.jumpToState(1)
        
        // Then
        assertEquals(states[1], result)
        assertEquals(states[1], restoredState)
        assertEquals(1, middleware.currentIndex.value)
    }
    
    @Test
    fun `should return null for invalid jump index`() = runTest {
        // Given
        middleware.recordInitialState(TestState(0))
        
        // When & Then
        assertNull(middleware.jumpToState(-1))
        assertNull(middleware.jumpToState(10))
    }
    
    @Test
    fun `should step back in history`() = runTest {
        // Given
        val states = listOf(TestState(0), TestState(1), TestState(2))
        var restoredState: TestState? = null
        
        middleware.setStateRestoreCallback { state -> restoredState = state }
        middleware.recordInitialState(states[0])
        
        val next: suspend (TestAction) -> TestState = { action ->
            when (action) {
                is TestAction.SetValue -> TestState(action.value)
                else -> TestState(0)
            }
        }
        
        // Build history
        middleware.process(states[0], TestAction.SetValue(1), next)
        middleware.process(states[1], TestAction.SetValue(2), next)
        
        // When
        val result = middleware.stepBack()
        
        // Then
        assertEquals(states[1], result)
        assertEquals(states[1], restoredState)
        assertEquals(1, middleware.currentIndex.value)
    }
    
    @Test
    fun `should step forward in history`() = runTest {
        // Given
        val states = listOf(TestState(0), TestState(1), TestState(2))
        var restoredState: TestState? = null
        
        middleware.setStateRestoreCallback { state -> restoredState = state }
        middleware.recordInitialState(states[0])
        
        val next: suspend (TestAction) -> TestState = { action ->
            when (action) {
                is TestAction.SetValue -> TestState(action.value)
                else -> TestState(0)
            }
        }
        
        // Build history and step back
        middleware.process(states[0], TestAction.SetValue(1), next)
        middleware.process(states[1], TestAction.SetValue(2), next)
        middleware.stepBack() // Now at index 1
        
        // When
        val result = middleware.stepForward()
        
        // Then
        assertEquals(states[2], result)
        assertEquals(states[2], restoredState)
        assertEquals(2, middleware.currentIndex.value)
    }
    
    @Test
    fun `should return null when stepping back at beginning`() = runTest {
        // Given
        middleware.recordInitialState(TestState(0))
        
        // When
        val result = middleware.stepBack()
        
        // Then
        assertNull(result)
        assertEquals(0, middleware.currentIndex.value)
    }
    
    @Test
    fun `should return null when stepping forward at end`() = runTest {
        // Given
        middleware.recordInitialState(TestState(0))
        
        // When
        val result = middleware.stepForward()
        
        // Then
        assertNull(result)
        assertEquals(0, middleware.currentIndex.value)
    }
    
    @Test
    fun `should clear history`() = runTest {
        // Given
        middleware.recordInitialState(TestState(0))
        val next: suspend (TestAction) -> TestState = { TestState(1) }
        middleware.process(TestState(0), TestAction.Increment, next)
        
        // When
        middleware.clearHistory()
        
        // Then
        assertTrue(middleware.history.isEmpty())
        assertEquals(-1, middleware.currentIndex.value)
    }
    
    @Test
    fun `should not record during replay`() = runTest {
        // Given
        middleware.recordInitialState(TestState(0))
        val next: suspend (TestAction) -> TestState = { TestState(1) }
        middleware.process(TestState(0), TestAction.Increment, next)
        val initialHistorySize = middleware.history.size
        
        // When
        middleware.replay { state, action ->
            // This should not add to history
        }
        
        // Then
        assertEquals(initialHistorySize, middleware.history.size)
        assertFalse(middleware.isReplaying)
    }
    
    @Test
    fun `should handle exceptions during replay`() = runTest {
        // Given
        middleware.recordInitialState(TestState(0))
        val next: suspend (TestAction) -> TestState = { TestState(1) }
        middleware.process(TestState(0), TestAction.Increment, next)
        
        // When & Then
        try {
            middleware.replay { _, _ ->
                throw RuntimeException("Test exception")
            }
            fail("Expected exception to be thrown")
        } catch (e: RuntimeException) {
            assertEquals("Test exception", e.message)
            assertFalse(middleware.isReplaying) // Should reset flag even on exception
        }
    }
}
