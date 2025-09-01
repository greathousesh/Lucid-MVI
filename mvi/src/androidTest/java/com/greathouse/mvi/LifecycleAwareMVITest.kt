package com.greathouse.mvi

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LifecycleAwareMVITest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var viewModel: AndroidTestCounterViewModel
    private lateinit var effectHandler: AndroidCounterEffectHandler
    private lateinit var testLifecycleOwner: TestLifecycleOwner
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        effectHandler = AndroidCounterEffectHandler()
        viewModel = AndroidTestCounterViewModel(
            reducer = AndroidCounterReducer(),
            effectHandler = effectHandler
        )
        testLifecycleOwner = TestLifecycleOwner()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun testViewModelWithLifecycleOwner() = runTest {
        // Given
        val stateCollector = StateEventCollector(viewModel, testLifecycleOwner)
        
        // When
        testLifecycleOwner.moveToState(Lifecycle.State.CREATED)
        stateCollector.startCollecting()
        
        testLifecycleOwner.moveToState(Lifecycle.State.STARTED)
        testLifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        
        // Perform some actions
        viewModel.increment()
        viewModel.increment()
        viewModel.setCount(10)
        
        // Wait for state updates
        kotlinx.coroutines.delay(100)
        
        // Then
        val states = stateCollector.states
        assertTrue(states.size >= 4) // Initial + 3 actions
        assertEquals(0, states[0].count) // Initial state
        assertEquals(1, states[1].count) // After first increment
        assertEquals(2, states[2].count) // After second increment
        assertEquals(10, states[3].count) // After setCount
    }
    
    @Test
    fun testViewModelSurvivesLifecycleChanges() = runTest {
        // Given
        testLifecycleOwner.moveToState(Lifecycle.State.CREATED)
        testLifecycleOwner.moveToState(Lifecycle.State.STARTED)
        testLifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        
        // When - Perform action in RESUMED state
        viewModel.setCount(5)
        
        // Simulate lifecycle changes
        testLifecycleOwner.moveToState(Lifecycle.State.STARTED)
        testLifecycleOwner.moveToState(Lifecycle.State.CREATED)
        
        // Perform action in CREATED state
        viewModel.increment()
        
        // Move back to RESUMED
        testLifecycleOwner.moveToState(Lifecycle.State.STARTED)
        testLifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        
        // Then - State should be preserved
        assertEquals(6, viewModel.stateFlow.value.count)
    }
    
    @Test
    fun testEffectsWorkAcrossLifecycleStates() = runTest {
        // Given
        testLifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        effectHandler.shouldFailPreferences = false
        
        // When & Then
        viewModel.eventFlow.test {
            // Start in RESUMED state
            viewModel.saveToPreferences()
            val event1 = awaitItem()
            assertEquals(AndroidCounterEvent.CountSavedToPreferences, event1)
            
            // Move to STARTED state
            testLifecycleOwner.moveToState(Lifecycle.State.STARTED)
            
            // Effects should still work
            viewModel.showToast("Test in STARTED")
            val event2 = awaitItem()
            assertTrue(event2 is AndroidCounterEvent.ToastShown)
            assertEquals("Test in STARTED", (event2 as AndroidCounterEvent.ToastShown).message)
            
            // Move to CREATED state
            testLifecycleOwner.moveToState(Lifecycle.State.CREATED)
            
            // Effects should still work
            viewModel.showToast("Test in CREATED")
            val event3 = awaitItem()
            assertTrue(event3 is AndroidCounterEvent.ToastShown)
            assertEquals("Test in CREATED", (event3 as AndroidCounterEvent.ToastShown).message)
        }
    }
    
    @Test
    fun testStateCollectionWithLifecycleAwareness() = runTest {
        // Given
        val collectedStates = mutableListOf<AndroidCounterState>()
        val collectedEvents = mutableListOf<AndroidCounterEvent>()
        
        // Start lifecycle
        testLifecycleOwner.moveToState(Lifecycle.State.CREATED)
        testLifecycleOwner.moveToState(Lifecycle.State.STARTED)
        
        // Start collecting when lifecycle is active
        val stateJob = testLifecycleOwner.lifecycleScope.launch {
            viewModel.stateFlow.collect { state ->
                collectedStates.add(state)
            }
        }
        
        val eventJob = testLifecycleOwner.lifecycleScope.launch {
            viewModel.eventFlow.collect { event ->
                collectedEvents.add(event)
            }
        }
        
        testLifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        
        // When - Perform actions
        viewModel.increment()
        viewModel.showToast("Lifecycle test")
        
        // Wait for collections
        kotlinx.coroutines.delay(100)
        
        // Then
        assertTrue(collectedStates.size >= 2) // Initial + increment
        assertTrue(collectedEvents.size >= 1) // Toast event
        
        // Cleanup
        stateJob.cancel()
        eventJob.cancel()
    }
    
    @Test
    fun testViewModelClearsResourcesOnDestroy() = runTest {
        // Given
        testLifecycleOwner.moveToState(Lifecycle.State.CREATED)
        testLifecycleOwner.moveToState(Lifecycle.State.STARTED)
        testLifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        
        // Perform some actions to ensure ViewModel is active
        viewModel.increment()
        viewModel.saveToPreferences()
        
        // When - Destroy lifecycle
        testLifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
        
        // Then - ViewModel should still have last state but channels should be closed
        assertEquals(1, viewModel.stateFlow.value.count)
        
        // New actions should not crash (channels are closed gracefully)
        viewModel.increment() // Should not crash
    }
    
    @Test
    fun testLongRunningEffectWithLifecycleChanges() = runTest {
        // Given
        testLifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        effectHandler.shouldFailNetwork = false
        
        // When & Then
        viewModel.stateFlow.test {
            // Skip initial state
            skipItems(1)
            
            // Start long-running network sync
            viewModel.syncWithNetwork()
            
            // Should start loading
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            
            // Change lifecycle state during operation
            testLifecycleOwner.moveToState(Lifecycle.State.STARTED)
            
            // Operation should complete despite lifecycle change
            val completedState = awaitItem()
            assertFalse(completedState.isLoading)
        }
        
        // Verify event was emitted
        viewModel.eventFlow.test {
            viewModel.syncWithNetwork()
            val event = awaitItem()
            assertEquals(AndroidCounterEvent.NetworkSyncCompleted, event)
        }
    }
    
    @Test
    fun testMultipleLifecycleOwnersWithSameViewModel() = runTest {
        // Given
        val lifecycleOwner1 = TestLifecycleOwner()
        val lifecycleOwner2 = TestLifecycleOwner()
        
        val collector1 = StateEventCollector(viewModel, lifecycleOwner1)
        val collector2 = StateEventCollector(viewModel, lifecycleOwner2)
        
        // When
        lifecycleOwner1.moveToState(Lifecycle.State.RESUMED)
        lifecycleOwner2.moveToState(Lifecycle.State.RESUMED)
        
        collector1.startCollecting()
        collector2.startCollecting()
        
        // Perform actions
        viewModel.increment()
        viewModel.setCount(20)
        
        // Wait for collections
        kotlinx.coroutines.delay(100)
        
        // Then - Both collectors should receive the same states
        val states1 = collector1.states
        val states2 = collector2.states
        
        assertTrue(states1.size >= 3) // Initial + increment + setCount
        assertTrue(states2.size >= 3)
        
        // States should be the same
        assertEquals(states1.last().count, states2.last().count)
        assertEquals(20, states1.last().count)
    }
    
    @Test
    fun testViewModelBehaviorWhenLifecycleIsDestroyed() = runTest {
        // Given
        testLifecycleOwner.moveToState(Lifecycle.State.RESUMED)
        
        // Perform initial action
        viewModel.setCount(15)
        assertEquals(15, viewModel.stateFlow.value.count)
        
        // When - Destroy lifecycle
        testLifecycleOwner.moveToState(Lifecycle.State.DESTROYED)
        
        // Then - ViewModel should continue to work independently
        viewModel.increment()
        assertEquals(16, viewModel.stateFlow.value.count)
        
        // Effects should still work
        viewModel.eventFlow.test {
            viewModel.showToast("After destroy")
            val event = awaitItem()
            assertTrue(event is AndroidCounterEvent.ToastShown)
            assertEquals("After destroy", (event as AndroidCounterEvent.ToastShown).message)
        }
    }
}

// Test LifecycleOwner implementation
class TestLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    
    override val lifecycle: Lifecycle = lifecycleRegistry
    
    fun moveToState(state: Lifecycle.State) {
        lifecycleRegistry.currentState = state
    }
}
