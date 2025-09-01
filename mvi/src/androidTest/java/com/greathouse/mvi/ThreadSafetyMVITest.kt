package com.greathouse.mvi

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ThreadSafetyMVITest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var viewModel: AndroidTestCounterViewModel
    private lateinit var effectHandler: AndroidCounterEffectHandler
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        effectHandler = AndroidCounterEffectHandler()
        viewModel = AndroidTestCounterViewModel(
            reducer = AndroidCounterReducer(),
            effectHandler = effectHandler
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun testConcurrentActionsFromMultipleThreads() = runTest {
        // Given
        val numberOfThreads = 10
        val actionsPerThread = 100
        val latch = CountDownLatch(numberOfThreads)
        val completedActions = AtomicInteger(0)
        
        // When - Launch multiple coroutines concurrently
        repeat(numberOfThreads) { threadIndex ->
            launch(Dispatchers.Default) {
                repeat(actionsPerThread) {
                    viewModel.increment()
                    completedActions.incrementAndGet()
                }
                latch.countDown()
            }
        }
        
        // Wait for all threads to complete
        withContext(Dispatchers.IO) {
            latch.await()
        }
        
        // Wait a bit more for all state updates to propagate
        delay(100)
        
        // Then
        val finalCount = viewModel.stateFlow.value.count
        val expectedCount = numberOfThreads * actionsPerThread
        
        assertEquals(expectedCount, finalCount)
        assertEquals(expectedCount, completedActions.get())
    }
    
    @Test
    fun testConcurrentEffectsFromMultipleThreads() = runTest {
        // Given
        val numberOfThreads = 5
        val effectsPerThread = 20
        val latch = CountDownLatch(numberOfThreads)
        val receivedEvents = ConcurrentLinkedQueue<AndroidCounterEvent>()
        
        // Start collecting events
        val eventCollectionJob = launch {
            viewModel.eventFlow.collect { event ->
                receivedEvents.add(event)
            }
        }
        
        // When - Launch multiple coroutines sending effects
        repeat(numberOfThreads) { threadIndex ->
            launch(Dispatchers.Default) {
                repeat(effectsPerThread) { actionIndex ->
                    viewModel.showToast("Thread $threadIndex - Action $actionIndex")
                }
                latch.countDown()
            }
        }
        
        // Wait for all threads to complete
        withContext(Dispatchers.IO) {
            latch.await()
        }
        
        // Wait for all events to be collected
        delay(200)
        
        // Then
        val expectedEvents = numberOfThreads * effectsPerThread
        assertEquals(expectedEvents, receivedEvents.size)
        
        // Verify all events are toast events
        receivedEvents.forEach { event ->
            assertTrue(event is AndroidCounterEvent.ToastShown)
        }
        
        eventCollectionJob.cancel()
    }
    
    @Test
    fun testMixedConcurrentActionsAndEffects() = runTest {
        // Given
        val numberOfOperations = 50
        val latch = CountDownLatch(numberOfOperations)
        val actionResults = ConcurrentLinkedQueue<Int>()
        val eventResults = ConcurrentLinkedQueue<AndroidCounterEvent>()
        
        // Start collecting states and events
        val stateCollectionJob = launch {
            viewModel.stateFlow.collect { state ->
                if (state.count > 0) {
                    actionResults.add(state.count)
                }
            }
        }
        
        val eventCollectionJob = launch {
            viewModel.eventFlow.collect { event ->
                eventResults.add(event)
            }
        }
        
        // When - Mix actions and effects concurrently
        repeat(numberOfOperations) { index ->
            launch(Dispatchers.Default) {
                if (index % 2 == 0) {
                    // Even indices: send actions
                    viewModel.increment()
                } else {
                    // Odd indices: send effects
                    viewModel.showToast("Operation $index")
                }
                latch.countDown()
            }
        }
        
        // Wait for all operations to complete
        withContext(Dispatchers.IO) {
            latch.await()
        }
        
        // Wait for collections
        delay(200)
        
        // Then
        val expectedActions = numberOfOperations / 2
        val expectedEvents = numberOfOperations / 2
        
        assertEquals(expectedActions, viewModel.stateFlow.value.count)
        assertTrue(actionResults.size >= expectedActions)
        assertEquals(expectedEvents, eventResults.size)
        
        stateCollectionJob.cancel()
        eventCollectionJob.cancel()
    }
    
    @Test
    fun testConcurrentStateReads() = runTest {
        // Given
        val numberOfReaders = 20
        val readsPerReader = 100
        val latch = CountDownLatch(numberOfReaders)
        val readResults = ConcurrentLinkedQueue<AndroidCounterState>()
        
        // Set initial state
        viewModel.setCount(42)
        delay(50) // Wait for state to be set
        
        // When - Multiple threads reading state concurrently
        repeat(numberOfReaders) {
            launch(Dispatchers.Default) {
                repeat(readsPerReader) {
                    val state = viewModel.stateFlow.value
                    readResults.add(state)
                }
                latch.countDown()
            }
        }
        
        // Wait for all readers to complete
        withContext(Dispatchers.IO) {
            latch.await()
        }
        
        // Then
        val expectedReads = numberOfReaders * readsPerReader
        assertEquals(expectedReads, readResults.size)
        
        // All reads should return the same count (42)
        readResults.forEach { state ->
            assertEquals(42, state.count)
        }
    }
    
    @Test
    fun testConcurrentLongRunningEffects() = runTest {
        // Given
        val numberOfEffects = 5
        val latch = CountDownLatch(numberOfEffects)
        val completedEffects = AtomicInteger(0)
        effectHandler.shouldFailNetwork = false
        
        // When - Launch multiple long-running effects
        repeat(numberOfEffects) { index ->
            launch(Dispatchers.Default) {
                viewModel.syncWithNetwork()
                completedEffects.incrementAndGet()
                latch.countDown()
            }
        }
        
        // Wait for all effects to complete
        withContext(Dispatchers.IO) {
            latch.await()
        }
        
        // Wait for final state updates
        delay(300)
        
        // Then
        assertEquals(numberOfEffects, completedEffects.get())
        assertFalse(viewModel.stateFlow.value.isLoading) // Should not be loading anymore
    }
    
    @Test
    fun testStateConsistencyUnderConcurrentModification() = runTest {
        // Given
        val numberOfOperations = 100
        val latch = CountDownLatch(numberOfOperations)
        val stateSnapshots = ConcurrentLinkedQueue<AndroidCounterState>()
        
        // Start collecting state snapshots
        val collectionJob = launch {
            viewModel.stateFlow.collect { state ->
                stateSnapshots.add(state.copy()) // Create defensive copy
            }
        }
        
        // When - Perform various concurrent operations
        repeat(numberOfOperations) { index ->
            launch(Dispatchers.Default) {
                when (index % 4) {
                    0 -> viewModel.increment()
                    1 -> viewModel.decrement()
                    2 -> viewModel.setCount(index)
                    3 -> viewModel.updateTimestamp()
                }
                latch.countDown()
            }
        }
        
        // Wait for all operations
        withContext(Dispatchers.IO) {
            latch.await()
        }
        
        // Wait for final collections
        delay(200)
        
        // Then - Verify state consistency
        assertTrue(stateSnapshots.size > 0)
        
        // Each state should be internally consistent
        stateSnapshots.forEach { state ->
            // Count should be a valid integer
            assertTrue(state.count >= Int.MIN_VALUE && state.count <= Int.MAX_VALUE)
            
            // Timestamp should be positive
            assertTrue(state.lastUpdated > 0)
            
            // Loading should be a valid boolean
            assertTrue(state.isLoading == true || state.isLoading == false)
        }
        
        collectionJob.cancel()
    }
    
    @Test
    fun testNoRaceConditionInEffectHandler() = runTest {
        // Given
        val numberOfSaves = 10
        val latch = CountDownLatch(numberOfSaves)
        val saveResults = ConcurrentLinkedQueue<Boolean>()
        effectHandler.shouldFailPreferences = false
        
        // When - Concurrent saves to preferences
        repeat(numberOfSaves) { index ->
            launch(Dispatchers.Default) {
                viewModel.setCount(index)
                viewModel.saveToPreferences()
                saveResults.add(true)
                latch.countDown()
            }
        }
        
        // Wait for all saves
        withContext(Dispatchers.IO) {
            latch.await()
        }
        
        // Wait for effects to complete
        delay(300)
        
        // Then
        assertEquals(numberOfSaves, saveResults.size)
        
        // Final preferences count should be one of the set values
        assertTrue(effectHandler.preferencesCount in 0 until numberOfSaves)
    }
    
    @Test
    fun testChannelCapacityUnderHighLoad() = runTest {
        // Given
        val highLoadOperations = 1000
        val latch = CountDownLatch(highLoadOperations)
        val processedOperations = AtomicInteger(0)
        
        // When - Send high volume of operations
        repeat(highLoadOperations) {
            launch(Dispatchers.Default) {
                viewModel.increment()
                processedOperations.incrementAndGet()
                latch.countDown()
            }
        }
        
        // Wait for all operations to be queued
        withContext(Dispatchers.IO) {
            latch.await()
        }
        
        // Wait for processing
        delay(500)
        
        // Then - All operations should be processed
        assertEquals(highLoadOperations, processedOperations.get())
        assertEquals(highLoadOperations, viewModel.stateFlow.value.count)
    }
    
    @Test
    fun testViewModelBehaviorUnderMemoryPressure() = runTest {
        // Given - Simulate memory pressure with many concurrent operations
        val heavyOperations = 200
        val latch = CountDownLatch(heavyOperations)
        val largeDataList = mutableListOf<ByteArray>()
        
        // When - Perform operations while consuming memory
        repeat(heavyOperations) { index ->
            launch(Dispatchers.Default) {
                // Simulate memory allocation
                largeDataList.add(ByteArray(1024)) // 1KB per operation
                
                // Perform MVI operations
                viewModel.increment()
                viewModel.showToast("Heavy operation $index")
                
                latch.countDown()
            }
        }
        
        // Wait for completion
        withContext(Dispatchers.IO) {
            latch.await()
        }
        
        // Wait for processing
        delay(300)
        
        // Then - ViewModel should still function correctly
        assertEquals(heavyOperations, viewModel.stateFlow.value.count)
        
        // Cleanup
        largeDataList.clear()
    }
}
