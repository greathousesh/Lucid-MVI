package com.greathouse.mvi.middleware

import com.greathouse.mvi.middleware.ValidationMiddleware
import com.greathouse.mvi.middleware.ActionTransformMiddleware
import com.greathouse.mvi.middleware.ActionFilterMiddleware
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class ValidationMiddlewareTest {
    
    private data class TestState(val value: Int = 0, val isValid: Boolean = true)
    private sealed class TestAction {
        object ValidAction : TestAction()
        object InvalidAction : TestAction()
        data class SetValue(val value: Int) : TestAction()
    }
    
    @Test
    fun `should pass valid actions through`() = runTest {
        // Given
        val validator: (TestState, TestAction) -> String? = { _, action ->
            when (action) {
                TestAction.InvalidAction -> "Invalid action"
                else -> null
            }
        }
        val middleware = ValidationMiddleware(validator)
        val currentState = TestState(5)
        val action = TestAction.ValidAction
        val newState = TestState(6)
        val next: suspend (TestAction) -> TestState = { newState }
        
        // When
        val result = middleware.process(currentState, action, next)
        
        // Then
        assertEquals(newState, result)
    }
    
    @Test
    fun `should block invalid actions and return current state`() = runTest {
        // Given
        val validator: (TestState, TestAction) -> String? = { _, action ->
            when (action) {
                TestAction.InvalidAction -> "Invalid action"
                else -> null
            }
        }
        val middleware = ValidationMiddleware(validator)
        val currentState = TestState(5)
        val action = TestAction.InvalidAction
        val newState = TestState(6)
        val next: suspend (TestAction) -> TestState = { newState }
        
        // When
        val result = middleware.process(currentState, action, next)
        
        // Then
        assertEquals(currentState, result) // Should return unchanged state
    }
    
    @Test
    fun `should call onValidationError when validation fails`() = runTest {
        // Given
        var errorCalled = false
        var errorState: TestState? = null
        var errorAction: TestAction? = null
        var errorMessage: String? = null
        
        val validator: (TestState, TestAction) -> String? = { _, action ->
            when (action) {
                TestAction.InvalidAction -> "Invalid action"
                else -> null
            }
        }
        
        val onValidationError: (TestState, TestAction, String) -> Unit = { state, action, message ->
            errorCalled = true
            errorState = state
            errorAction = action
            errorMessage = message
        }
        
        val middleware = ValidationMiddleware(validator, onValidationError)
        val currentState = TestState(5)
        val action = TestAction.InvalidAction
        val next: suspend (TestAction) -> TestState = { TestState(6) }
        
        // When
        val result = middleware.process(currentState, action, next)
        
        // Then
        assertEquals(currentState, result)
        assertTrue(errorCalled)
        assertEquals(currentState, errorState)
        assertEquals(action, errorAction)
        assertEquals("Invalid action", errorMessage)
    }
    
    @Test
    fun `should validate based on state and action`() = runTest {
        // Given
        val validator: (TestState, TestAction) -> String? = { state, action ->
            when {
                !state.isValid -> "State is invalid"
                action is TestAction.SetValue && action.value < 0 -> "Negative values not allowed"
                else -> null
            }
        }
        val middleware = ValidationMiddleware(validator)
        
        // Test with invalid state
        val invalidState = TestState(5, isValid = false)
        val validAction = TestAction.ValidAction
        val next: suspend (TestAction) -> TestState = { TestState(6) }
        
        // When
        val result1 = middleware.process(invalidState, validAction, next)
        
        // Then
        assertEquals(invalidState, result1)
        
        // Test with negative value
        val validState = TestState(5, isValid = true)
        val negativeAction = TestAction.SetValue(-1)
        
        // When
        val result2 = middleware.process(validState, negativeAction, next)
        
        // Then
        assertEquals(validState, result2)
        
        // Test with valid combination
        val positiveAction = TestAction.SetValue(10)
        
        // When
        val result3 = middleware.process(validState, positiveAction, next)
        
        // Then
        assertEquals(TestState(6), result3) // Should process normally
    }
}

class ActionTransformMiddlewareTest {
    
    private data class TestState(val value: Int = 0)
    private sealed class TestAction {
        object OldAction : TestAction()
        object NewAction : TestAction()
        data class SetValue(val value: Int) : TestAction()
    }
    
    @Test
    fun `should transform actions`() = runTest {
        // Given
        val transformer: (TestState, TestAction) -> TestAction = { _, action ->
            when (action) {
                TestAction.OldAction -> TestAction.NewAction
                else -> action
            }
        }
        val middleware = ActionTransformMiddleware(transformer)
        val currentState = TestState(5)
        val action = TestAction.OldAction
        var processedAction: TestAction? = null
        val next: suspend (TestAction) -> TestState = { receivedAction ->
            processedAction = receivedAction
            TestState(6)
        }
        
        // When
        val result = middleware.process(currentState, action, next)
        
        // Then
        assertEquals(TestState(6), result)
        assertEquals(TestAction.NewAction, processedAction)
    }
    
    @Test
    fun `should pass through non-transformable actions`() = runTest {
        // Given
        val transformer: (TestState, TestAction) -> TestAction = { _, action ->
            when (action) {
                TestAction.OldAction -> TestAction.NewAction
                else -> action
            }
        }
        val middleware = ActionTransformMiddleware(transformer)
        val currentState = TestState(5)
        val action = TestAction.SetValue(10)
        var processedAction: TestAction? = null
        val next: suspend (TestAction) -> TestState = { receivedAction ->
            processedAction = receivedAction
            TestState(6)
        }
        
        // When
        val result = middleware.process(currentState, action, next)
        
        // Then
        assertEquals(TestState(6), result)
        assertEquals(action, processedAction)
    }
}

class ActionFilterMiddlewareTest {
    
    private data class TestState(val value: Int = 0, val allowNegative: Boolean = false)
    private sealed class TestAction {
        object AllowedAction : TestAction()
        object BlockedAction : TestAction()
        data class SetValue(val value: Int) : TestAction()
    }
    
    @Test
    fun `should allow actions that match predicate`() = runTest {
        // Given
        val predicate: (TestState, TestAction) -> Boolean = { _, action ->
            action != TestAction.BlockedAction
        }
        val middleware = ActionFilterMiddleware(predicate)
        val currentState = TestState(5)
        val action = TestAction.AllowedAction
        val newState = TestState(6)
        val next: suspend (TestAction) -> TestState = { newState }
        
        // When
        val result = middleware.process(currentState, action, next)
        
        // Then
        assertEquals(newState, result)
    }
    
    @Test
    fun `should block actions that don't match predicate`() = runTest {
        // Given
        val predicate: (TestState, TestAction) -> Boolean = { _, action ->
            action != TestAction.BlockedAction
        }
        val middleware = ActionFilterMiddleware(predicate)
        val currentState = TestState(5)
        val action = TestAction.BlockedAction
        val next: suspend (TestAction) -> TestState = { TestState(6) }
        
        // When
        val result = middleware.process(currentState, action, next)
        
        // Then
        assertEquals(currentState, result) // Should return unchanged state
    }
    
    @Test
    fun `should filter based on state and action`() = runTest {
        // Given
        val predicate: (TestState, TestAction) -> Boolean = { state, action ->
            when (action) {
                is TestAction.SetValue -> state.allowNegative || action.value >= 0
                else -> true
            }
        }
        val middleware = ActionFilterMiddleware(predicate)
        
        // Test with state that doesn't allow negative values
        val restrictiveState = TestState(5, allowNegative = false)
        val negativeAction = TestAction.SetValue(-1)
        val next: suspend (TestAction) -> TestState = { TestState(6) }
        
        // When
        val result1 = middleware.process(restrictiveState, negativeAction, next)
        
        // Then
        assertEquals(restrictiveState, result1) // Should be blocked
        
        // Test with state that allows negative values
        val permissiveState = TestState(5, allowNegative = true)
        
        // When
        val result2 = middleware.process(permissiveState, negativeAction, next)
        
        // Then
        assertEquals(TestState(6), result2) // Should be allowed
        
        // Test with positive value (should always be allowed)
        val positiveAction = TestAction.SetValue(10)
        
        // When
        val result3 = middleware.process(restrictiveState, positiveAction, next)
        
        // Then
        assertEquals(TestState(6), result3) // Should be allowed
    }
}
