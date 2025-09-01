package com.greathouse.mvi

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class CounterReducerTest {
    
    private lateinit var reducer: CounterReducer
    private lateinit var initialState: CounterState
    
    @Before
    fun setUp() {
        reducer = CounterReducer()
        initialState = CounterState(count = 5, isLoading = false, error = null)
    }
    
    @Test
    fun `increment action should increase count by 1`() {
        // Given
        val action = CounterAction.Increment
        
        // When
        val newState = reducer.reduce(initialState, action)
        
        // Then
        assertEquals(6, newState.count)
        assertEquals(initialState.isLoading, newState.isLoading)
        assertEquals(initialState.error, newState.error)
    }
    
    @Test
    fun `decrement action should decrease count by 1`() {
        // Given
        val action = CounterAction.Decrement
        
        // When
        val newState = reducer.reduce(initialState, action)
        
        // Then
        assertEquals(4, newState.count)
        assertEquals(initialState.isLoading, newState.isLoading)
        assertEquals(initialState.error, newState.error)
    }
    
    @Test
    fun `reset action should set count to 0`() {
        // Given
        val action = CounterAction.Reset
        
        // When
        val newState = reducer.reduce(initialState, action)
        
        // Then
        assertEquals(0, newState.count)
        assertEquals(initialState.isLoading, newState.isLoading)
        assertEquals(initialState.error, newState.error)
    }
    
    @Test
    fun `setCount action should set count to specified value`() {
        // Given
        val targetCount = 42
        val action = CounterAction.SetCount(targetCount)
        
        // When
        val newState = reducer.reduce(initialState, action)
        
        // Then
        assertEquals(targetCount, newState.count)
        assertEquals(initialState.isLoading, newState.isLoading)
        assertEquals(initialState.error, newState.error)
    }
    
    @Test
    fun `startLoading action should set isLoading to true`() {
        // Given
        val action = CounterAction.StartLoading
        
        // When
        val newState = reducer.reduce(initialState, action)
        
        // Then
        assertTrue(newState.isLoading)
        assertEquals(initialState.count, newState.count)
        assertEquals(initialState.error, newState.error)
    }
    
    @Test
    fun `stopLoading action should set isLoading to false`() {
        // Given
        val loadingState = initialState.copy(isLoading = true)
        val action = CounterAction.StopLoading
        
        // When
        val newState = reducer.reduce(loadingState, action)
        
        // Then
        assertFalse(newState.isLoading)
        assertEquals(loadingState.count, newState.count)
        assertEquals(loadingState.error, newState.error)
    }
    
    @Test
    fun `setError action should set error message`() {
        // Given
        val errorMessage = "Something went wrong"
        val action = CounterAction.SetError(errorMessage)
        
        // When
        val newState = reducer.reduce(initialState, action)
        
        // Then
        assertEquals(errorMessage, newState.error)
        assertEquals(initialState.count, newState.count)
        assertEquals(initialState.isLoading, newState.isLoading)
    }
    
    @Test
    fun `clearError action should clear error message`() {
        // Given
        val errorState = initialState.copy(error = "Some error")
        val action = CounterAction.ClearError
        
        // When
        val newState = reducer.reduce(errorState, action)
        
        // Then
        assertNull(newState.error)
        assertEquals(errorState.count, newState.count)
        assertEquals(errorState.isLoading, newState.isLoading)
    }
    
    @Test
    fun `reducer should be pure function - same input produces same output`() {
        // Given
        val action = CounterAction.Increment
        
        // When
        val result1 = reducer.reduce(initialState, action)
        val result2 = reducer.reduce(initialState, action)
        
        // Then
        assertEquals(result1, result2)
    }
    
    @Test
    fun `reducer should not modify original state`() {
        // Given
        val originalState = CounterState(count = 10, isLoading = true, error = "error")
        val action = CounterAction.Increment
        
        // When
        val newState = reducer.reduce(originalState, action)
        
        // Then
        // Original state should remain unchanged
        assertEquals(10, originalState.count)
        assertTrue(originalState.isLoading)
        assertEquals("error", originalState.error)
        
        // New state should have the changes
        assertEquals(11, newState.count)
        assertTrue(newState.isLoading)
        assertEquals("error", newState.error)
    }
}
