package com.greathouse.mvi

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class BaseMVIViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @get:Rule
    val mainDispatcherRule = TestDispatcherRule(testDispatcher)
    
    private lateinit var viewModel: TestCounterViewModel
    private lateinit var effectHandler: CounterEffectHandler
    
    @Before
    fun setUp() {
        effectHandler = CounterEffectHandler()
        viewModel = TestCounterViewModel(
            reducer = CounterReducer(),
            effectHandler = effectHandler
        )
    }
    
    @Test
    fun `initial state should be set correctly`() {
        // Then
        val currentState = viewModel.stateFlow.value
        assertEquals(CounterState(), currentState)
        assertEquals(0, currentState.count)
        assertFalse(currentState.isLoading)
        assertNull(currentState.error)
    }
    
    @Test
    fun `sendAction should update state through reducer`() = runTest(testDispatcher) {
        // Given & When & Then
        viewModel.stateFlow.test {
            // Initial state
            assertEquals(CounterState(count = 0), awaitItem())
            
            // Increment action
            viewModel.increment()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(CounterState(count = 1), awaitItem())
            
            // Decrement action
            viewModel.decrement()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(CounterState(count = 0), awaitItem())
            
            // Set count action
            viewModel.setCount(42)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(CounterState(count = 42), awaitItem())
            
            // Reset action
            viewModel.reset()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(CounterState(count = 0), awaitItem())
        }
    }
    
    @Test
    fun `multiple actions should be processed in order`() = runTest(testDispatcher) {
        // Given & When & Then
        viewModel.stateFlow.test {
            // Initial state
            assertEquals(CounterState(count = 0), awaitItem())
            
            // Send multiple actions
            viewModel.increment()
            viewModel.increment()
            viewModel.increment()
            
            // Advance the test dispatcher to process all pending coroutines
            testDispatcher.scheduler.advanceUntilIdle()
            
            // Should receive updates in order
            assertEquals(CounterState(count = 1), awaitItem())
            assertEquals(CounterState(count = 2), awaitItem())
            assertEquals(CounterState(count = 3), awaitItem())
        }
    }
    
    @Test
    fun `sendEffect should trigger effect handler and emit events`() = runTest {
        // Given
        effectHandler.shouldFailSave = false
        
        // When & Then
        viewModel.eventFlow.test {
            viewModel.saveCount()
            
            // Should receive CountSaved event
            val event = awaitItem()
            assertEquals(CounterEvent.CountSaved, event)
        }
    }
    
    @Test
    fun `effect handler should dispatch actions that update state`() = runTest {
        // Given
        effectHandler.shouldFailSave = false
        
        // When & Then
        viewModel.stateFlow.test {
            // Initial state
            assertEquals(CounterState(count = 0, isLoading = false), awaitItem())
            
            viewModel.saveCount()
            
            // Should start loading
            assertEquals(CounterState(count = 0, isLoading = true), awaitItem())
            
            // Should stop loading after save completes
            assertEquals(CounterState(count = 0, isLoading = false), awaitItem())
        }
    }
    
    @Test
    fun `effect handler failure should emit error event`() = runTest {
        // Given
        effectHandler.shouldFailSave = true
        
        // When & Then
        viewModel.eventFlow.test {
            viewModel.saveCount()
            
            // Should receive error event
            val event = awaitItem()
            assertTrue(event is CounterEvent.ShowError)
            assertEquals("Save failed", (event as CounterEvent.ShowError).message)
        }
    }
    
    @Test
    fun `load effect should update state and emit event`() = runTest {
        // Given
        effectHandler.savedCount = 25
        effectHandler.shouldFailLoad = false
        
        // When & Then - Test state changes
        viewModel.stateFlow.test {
            // Initial state
            assertEquals(CounterState(count = 0, isLoading = false), awaitItem())
            
            viewModel.loadCount()
            
            // Should start loading
            assertEquals(CounterState(count = 0, isLoading = true), awaitItem())
            
            // Should set loaded count and stop loading
            assertEquals(CounterState(count = 25, isLoading = true), awaitItem())
            assertEquals(CounterState(count = 25, isLoading = false), awaitItem())
        }
        
        // Test events
        viewModel.eventFlow.test {
            viewModel.loadCount()
            
            val event = awaitItem()
            assertTrue(event is CounterEvent.CountLoaded)
            assertEquals(25, (event as CounterEvent.CountLoaded).count)
        }
    }
    
    @Test
    fun `validation effect should handle invalid input - events`() = runTest {
        // Test events
        viewModel.eventFlow.test {
            viewModel.validateCount(-5)
            
            val event = awaitItem()
            assertTrue(event is CounterEvent.ValidationFailed)
            assertEquals("Count cannot be negative", (event as CounterEvent.ValidationFailed).message)
        }
    }
    
    @Test
    fun `validation effect should handle invalid input - state`() = runTest {
        // Test state changes
        viewModel.stateFlow.test {
            // Initial state
            assertEquals(CounterState(count = 0, isLoading = false, error = null), awaitItem())
            
            viewModel.validateCount(-5)
            
            // Should set error in state
            val newState = awaitItem()
            assertEquals("Count cannot be negative", newState.error)
        }
    }
    
    @Test
    fun `validation effect should handle count exceeding limit`() = runTest {
        // When & Then
        viewModel.eventFlow.test {
            viewModel.validateCount(150)
            
            val event = awaitItem()
            assertTrue(event is CounterEvent.ValidationFailed)
            assertEquals("Count cannot exceed 100", (event as CounterEvent.ValidationFailed).message)
        }
    }
    
    @Test
    fun `state should not emit if new state equals old state`() = runTest {
        // When & Then
        viewModel.stateFlow.test {
            // Initial state
            assertEquals(CounterState(count = 0), awaitItem())
            
            // Set count to 5
            viewModel.setCount(5)
            assertEquals(CounterState(count = 5), awaitItem())
            
            // Set same count again - should not emit
            viewModel.setCount(5)
            
            // Wait a bit and ensure no new emissions
            expectNoEvents()
        }
    }
    
    @Test
    fun `concurrent actions should be handled correctly`() = runTest {
        // When & Then
        viewModel.stateFlow.test {
            // Initial state
            assertEquals(CounterState(count = 0), awaitItem())
            
            // Send multiple concurrent actions
            repeat(10) {
                viewModel.increment()
            }
            
            // Should receive all updates
            repeat(10) { i ->
                assertEquals(CounterState(count = i + 1), awaitItem())
            }
        }
    }
    
    @Test
    fun `viewModel should handle actions correctly after initialization`() = runTest(testDispatcher) {
        // Given
        val initialState = viewModel.stateFlow.value
        
        // When & Then
        // ViewModel should have correct initial state
        assertEquals(CounterState(), initialState)
        
        // New actions should work correctly
        viewModel.increment()
        
        // Advance the test dispatcher to process the action
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify state changed
        assertNotEquals(initialState, viewModel.stateFlow.value)
        assertEquals(1, viewModel.stateFlow.value.count)
    }
}
