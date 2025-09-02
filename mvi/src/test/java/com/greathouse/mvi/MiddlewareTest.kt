package com.greathouse.mvi

import com.greathouse.mvi.middleware.LoggingMiddleware
import com.greathouse.mvi.middleware.ValidationMiddleware
import com.greathouse.mvi.middleware.TimeTravelMiddleware
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

// Helper class for testing
class PassThroughMiddleware<State, Action> : Middleware<State, Action> {
    override suspend fun process(
        currentState: State,
        action: Action,
        next: suspend (Action) -> State
    ): State {
        return next(action)
    }
}

class MiddlewareChainTest {
    
    private data class TestState(val value: Int = 0)
    private sealed class TestAction {
        object ValidAction : TestAction()
        object InvalidAction : TestAction()
        data class SetValue(val value: Int) : TestAction()
    }
    
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
    fun `should execute middleware in order`() = runTest {
        // Given
        val executionOrder = mutableListOf<String>()
        
        val middleware1 = object : Middleware<TestState, TestAction> {
            override suspend fun process(
                currentState: TestState,
                action: TestAction,
                next: suspend (TestAction) -> TestState
            ): TestState {
                executionOrder.add("middleware1")
                return next(action)
            }
        }
        
        val middleware2 = object : Middleware<TestState, TestAction> {
            override suspend fun process(
                currentState: TestState,
                action: TestAction,
                next: suspend (TestAction) -> TestState
            ): TestState {
                executionOrder.add("middleware2")
                return next(action)
            }
        }
        
        val chain = MiddlewareChain(listOf(middleware1, middleware2))
        val currentState = TestState(5)
        val action = TestAction.ValidAction
        val newState = TestState(6)
        val next: suspend (TestAction) -> TestState = { 
            executionOrder.add("reducer")
            newState 
        }
        
        // When
        val result = chain.process(currentState, action, next)
        
        // Then
        assertEquals(newState, result)
        assertEquals(listOf("middleware1", "middleware2", "reducer"), executionOrder)
    }
    
    @Test
    fun `should handle empty middleware list`() = runTest {
        // Given
        val chain = MiddlewareChain<TestState, TestAction>(emptyList())
        val currentState = TestState(5)
        val action = TestAction.ValidAction
        val newState = TestState(6)
        val next: suspend (TestAction) -> TestState = { newState }
        
        // When
        val result = chain.process(currentState, action, next)
        
        // Then
        assertEquals(newState, result)
    }
    
    @Test
    fun `should handle single middleware`() = runTest {
        // Given
        val middleware = object : Middleware<TestState, TestAction> {
            override suspend fun process(
                currentState: TestState,
                action: TestAction,
                next: suspend (TestAction) -> TestState
            ): TestState {
                return next(action)
            }
        }
        
        val chain = MiddlewareChain(listOf(middleware))
        val currentState = TestState(5)
        val action = TestAction.ValidAction
        val newState = TestState(6)
        val next: suspend (TestAction) -> TestState = { newState }
        
        // When
        val result = chain.process(currentState, action, next)
        
        // Then
        assertEquals(newState, result)
    }
    
    @Test
    fun `should provide access to middleware list`() {
        // Given
        val middleware1 = PassThroughMiddleware<TestState, TestAction>()
        val middleware2 = PassThroughMiddleware<TestState, TestAction>()
        val middlewares = listOf(middleware1, middleware2)
        
        // When
        val chain = MiddlewareChain(middlewares)
        
        // Then
        assertEquals(middlewares, chain.getMiddlewares())
    }
    
    @Test
    fun `should handle middleware that blocks action`() = runTest {
        // Given
        val blockingMiddleware = object : Middleware<TestState, TestAction> {
            override suspend fun process(
                currentState: TestState,
                action: TestAction,
                next: suspend (TestAction) -> TestState
            ): TestState {
                return if (action == TestAction.InvalidAction) {
                    currentState // Block the action
                } else {
                    next(action)
                }
            }
        }
        
        val chain = MiddlewareChain(listOf(blockingMiddleware))
        val currentState = TestState(5)
        val action = TestAction.InvalidAction
        val next: suspend (TestAction) -> TestState = { TestState(6) }
        
        // When
        val result = chain.process(currentState, action, next)
        
        // Then
        assertEquals(currentState, result) // Should return unchanged state
    }
}

class MiddlewareBuilderTest {
    
    private data class TestState(val value: Int = 0)
    private sealed class TestAction {
        object ValidAction : TestAction()
        object InvalidAction : TestAction()
    }
    
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
    fun `should return null for empty builder`() {
        // Given
        val builder = MiddlewareBuilder<TestState, TestAction>()
        
        // When
        val result = builder.build()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `should return single middleware when only one added`() {
        // Given
        val builder = MiddlewareBuilder<TestState, TestAction>()
        
        // When
        val result = builder.logging("Test").build()
        
        // Then
        assertNotNull(result)
        assertTrue(result is LoggingMiddleware)
    }
    
    @Test
    fun `should return middleware chain when multiple added`() {
        // Given
        val builder = MiddlewareBuilder<TestState, TestAction>()
        
        // When
        val result = builder
            .logging("Test")
            .timeTravel()
            .build()
        
        // Then
        assertNotNull(result)
        assertTrue(result is MiddlewareChain)
    }
    
    @Test
    fun `should build logging middleware with custom parameters`() {
        // Given
        val builder = MiddlewareBuilder<TestState, TestAction>()
        
        // When
        val result = builder.logging("CustomTag", Log.INFO, false).build()
        
        // Then
        assertNotNull(result)
        assertTrue(result is LoggingMiddleware)
    }
    
    @Test
    fun `should build time travel middleware with custom history size`() {
        // Given
        val builder = MiddlewareBuilder<TestState, TestAction>()
        
        // When
        val result = builder.timeTravel(maxHistorySize = 20).build()
        
        // Then
        assertNotNull(result)
        assertTrue(result is TimeTravelMiddleware)
    }
    
    @Test
    fun `should build validation middleware`() {
        // Given
        val builder = MiddlewareBuilder<TestState, TestAction>()
        val validator: (TestState, TestAction) -> String? = { _, _ -> null }
        
        // When
        val result = builder.validation(validator).build()
        
        // Then
        assertNotNull(result)
        assertTrue(result is ValidationMiddleware)
    }
    
    @Test
    fun `should build transform middleware`() {
        // Given
        val builder = MiddlewareBuilder<TestState, TestAction>()
        val transformer: (TestState, TestAction) -> TestAction = { _, action -> action }
        
        // When
        val result = builder.transform(transformer).build()
        
        // Then
        assertNotNull(result)
        // ActionTransformMiddleware is not public, so we can't check the exact type
        // but we can verify it's not null and not a chain
        assertFalse(result is MiddlewareChain<*, *>)
    }
    
    @Test
    fun `should build filter middleware`() {
        // Given
        val builder = MiddlewareBuilder<TestState, TestAction>()
        val predicate: (TestState, TestAction) -> Boolean = { _, _ -> true }
        
        // When
        val result = builder.filter(predicate).build()
        
        // Then
        assertNotNull(result)
        // ActionFilterMiddleware is not public, so we can't check the exact type
        // but we can verify it's not null and not a chain
        assertFalse(result is MiddlewareChain<*, *>)
    }
    
    @Test
    fun `should build custom middleware`() {
        // Given
        val builder = MiddlewareBuilder<TestState, TestAction>()
        val customMiddleware = PassThroughMiddleware<TestState, TestAction>()
        
        // When
        val result = builder.custom(customMiddleware).build()
        
        // Then
        assertEquals(customMiddleware, result)
    }
    
    @Test
    fun `should build complex middleware chain`() = runTest {
        // Given
        val validator: (TestState, TestAction) -> String? = { _, action ->
            if (action == TestAction.InvalidAction) "Invalid" else null
        }
        
        // When
        val middleware = buildMiddleware<TestState, TestAction> {
            logging("Test")
            validation(validator)
            timeTravel()
        }
        
        // Then
        assertNotNull(middleware)
        assertTrue(middleware is MiddlewareChain)
        
        val chain = middleware as MiddlewareChain
        assertEquals(3, chain.getMiddlewares().size)
    }
    
    @Test
    fun `buildMiddleware function should work with empty builder`() {
        // When
        val result = buildMiddleware<TestState, TestAction> {
            // Empty builder
        }
        
        // Then
        assertNull(result)
    }
}
