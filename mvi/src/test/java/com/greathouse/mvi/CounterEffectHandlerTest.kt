package com.greathouse.mvi

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class CounterEffectHandlerTest {
    
    private lateinit var effectHandler: CounterEffectHandler
    private lateinit var testState: CounterState
    private val dispatchedActions = mutableListOf<CounterAction>()
    private val emittedEvents = mutableListOf<CounterEvent>()
    
    @Before
    fun setUp() {
        effectHandler = CounterEffectHandler()
        testState = CounterState(count = 10)
        dispatchedActions.clear()
        emittedEvents.clear()
    }
    
    private suspend fun mockDispatch(action: CounterAction) {
        dispatchedActions.add(action)
    }
    
    private suspend fun mockEmit(event: CounterEvent) {
        emittedEvents.add(event)
    }
    
    @Test
    fun `saveCount effect should save successfully and emit CountSaved event`() = runTest {
        // Given
        val effect = CounterEffect.SaveCount
        effectHandler.shouldFailSave = false
        
        // When
        effectHandler.handle(testState, effect, ::mockDispatch, ::mockEmit)
        
        // Then
        assertEquals(2, dispatchedActions.size)
        assertEquals(CounterAction.StartLoading, dispatchedActions[0])
        assertEquals(CounterAction.StopLoading, dispatchedActions[1])
        
        assertEquals(1, emittedEvents.size)
        assertEquals(CounterEvent.CountSaved, emittedEvents[0])
        
        assertEquals(testState.count, effectHandler.savedCount)
    }
    
    @Test
    fun `saveCount effect should handle failure and emit ShowError event`() = runTest {
        // Given
        val effect = CounterEffect.SaveCount
        effectHandler.shouldFailSave = true
        
        // When
        effectHandler.handle(testState, effect, ::mockDispatch, ::mockEmit)
        
        // Then
        assertEquals(2, dispatchedActions.size)
        assertEquals(CounterAction.StartLoading, dispatchedActions[0])
        assertEquals(CounterAction.StopLoading, dispatchedActions[1])
        
        assertEquals(1, emittedEvents.size)
        assertTrue(emittedEvents[0] is CounterEvent.ShowError)
        assertEquals("Save failed", (emittedEvents[0] as CounterEvent.ShowError).message)
    }
    
    @Test
    fun `loadCount effect should load successfully and emit CountLoaded event`() = runTest {
        // Given
        val effect = CounterEffect.LoadCount
        effectHandler.shouldFailLoad = false
        effectHandler.savedCount = 25
        
        // When
        effectHandler.handle(testState, effect, ::mockDispatch, ::mockEmit)
        
        // Then
        assertEquals(3, dispatchedActions.size)
        assertEquals(CounterAction.StartLoading, dispatchedActions[0])
        assertEquals(CounterAction.SetCount(25), dispatchedActions[1])
        assertEquals(CounterAction.StopLoading, dispatchedActions[2])
        
        assertEquals(1, emittedEvents.size)
        assertTrue(emittedEvents[0] is CounterEvent.CountLoaded)
        assertEquals(25, (emittedEvents[0] as CounterEvent.CountLoaded).count)
    }
    
    @Test
    fun `loadCount effect should handle failure and emit ShowError event`() = runTest {
        // Given
        val effect = CounterEffect.LoadCount
        effectHandler.shouldFailLoad = true
        
        // When
        effectHandler.handle(testState, effect, ::mockDispatch, ::mockEmit)
        
        // Then
        assertEquals(2, dispatchedActions.size)
        assertEquals(CounterAction.StartLoading, dispatchedActions[0])
        assertEquals(CounterAction.StopLoading, dispatchedActions[1])
        
        assertEquals(1, emittedEvents.size)
        assertTrue(emittedEvents[0] is CounterEvent.ShowError)
        assertEquals("Load failed", (emittedEvents[0] as CounterEvent.ShowError).message)
    }
    
    @Test
    fun `validateCount effect should pass validation for valid count`() = runTest {
        // Given
        val effect = CounterEffect.ValidateCount(50)
        
        // When
        effectHandler.handle(testState, effect, ::mockDispatch, ::mockEmit)
        
        // Then
        assertTrue(dispatchedActions.isEmpty())
        assertTrue(emittedEvents.isEmpty())
    }
    
    @Test
    fun `validateCount effect should fail validation for negative count`() = runTest {
        // Given
        val effect = CounterEffect.ValidateCount(-5)
        
        // When
        effectHandler.handle(testState, effect, ::mockDispatch, ::mockEmit)
        
        // Then
        assertEquals(1, dispatchedActions.size)
        assertTrue(dispatchedActions[0] is CounterAction.SetError)
        assertEquals("Count cannot be negative", (dispatchedActions[0] as CounterAction.SetError).error)
        
        assertEquals(1, emittedEvents.size)
        assertTrue(emittedEvents[0] is CounterEvent.ValidationFailed)
        assertEquals("Count cannot be negative", (emittedEvents[0] as CounterEvent.ValidationFailed).message)
    }
    
    @Test
    fun `validateCount effect should fail validation for count exceeding limit`() = runTest {
        // Given
        val effect = CounterEffect.ValidateCount(150)
        
        // When
        effectHandler.handle(testState, effect, ::mockDispatch, ::mockEmit)
        
        // Then
        assertEquals(1, dispatchedActions.size)
        assertTrue(dispatchedActions[0] is CounterAction.SetError)
        assertEquals("Count cannot exceed 100", (dispatchedActions[0] as CounterAction.SetError).error)
        
        assertEquals(1, emittedEvents.size)
        assertTrue(emittedEvents[0] is CounterEvent.ValidationFailed)
        assertEquals("Count cannot exceed 100", (emittedEvents[0] as CounterEvent.ValidationFailed).message)
    }
    
    @Test
    fun `validateCount effect should pass validation for boundary values`() = runTest {
        // Test lower boundary
        val effect1 = CounterEffect.ValidateCount(0)
        effectHandler.handle(testState, effect1, ::mockDispatch, ::mockEmit)
        
        // Test upper boundary
        val effect2 = CounterEffect.ValidateCount(100)
        effectHandler.handle(testState, effect2, ::mockDispatch, ::mockEmit)
        
        // Then
        assertTrue(dispatchedActions.isEmpty())
        assertTrue(emittedEvents.isEmpty())
    }
}
