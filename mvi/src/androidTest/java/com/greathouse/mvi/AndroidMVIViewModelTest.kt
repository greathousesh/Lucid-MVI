package com.greathouse.mvi

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AndroidMVIViewModelTest {
    
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
    fun testAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.greathouse.mvi.test", appContext.packageName)
    }
    
    @Test
    fun testInitialStateInAndroidEnvironment() {
        // Given & When
        val initialState = viewModel.stateFlow.value
        
        // Then
        assertEquals(AndroidCounterState(), initialState)
        assertEquals(0, initialState.count)
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
        assertTrue(initialState.lastUpdated > 0)
    }
    
    @Test
    fun testBasicActionsInAndroidEnvironment() = runTest {
        // Given & When & Then
        viewModel.stateFlow.test {
            // Initial state
            val initialState = awaitItem()
            assertEquals(0, initialState.count)
            
            // Increment
            viewModel.increment()
            val incrementState = awaitItem()
            assertEquals(1, incrementState.count)
            assertTrue(incrementState.lastUpdated > initialState.lastUpdated)
            
            // Decrement
            viewModel.decrement()
            val decrementState = awaitItem()
            assertEquals(0, decrementState.count)
            assertTrue(decrementState.lastUpdated > incrementState.lastUpdated)
            
            // Set count
            viewModel.setCount(42)
            val setCountState = awaitItem()
            assertEquals(42, setCountState.count)
            
            // Reset
            viewModel.reset()
            val resetState = awaitItem()
            assertEquals(0, resetState.count)
        }
    }
    
    @Test
    fun testPreferencesOperationsInAndroidEnvironment() = runTest {
        // Given
        effectHandler.shouldFailPreferences = false
        effectHandler.preferencesCount = 25
        
        // Test save to preferences
        viewModel.eventFlow.test {
            viewModel.stateFlow.test {
                // Skip initial state
                skipItems(1)
                
                viewModel.saveToPreferences()
                
                // Should start loading
                val loadingState = awaitItem()
                assertTrue(loadingState.isLoading)
                
                // Should stop loading
                val completedState = awaitItem()
                assertFalse(completedState.isLoading)
                
                // Should receive save event
                val saveEvent = awaitItem()
                assertEquals(AndroidCounterEvent.CountSavedToPreferences, saveEvent)
            }
        }
        
        // Test load from preferences
        viewModel.eventFlow.test {
            viewModel.stateFlow.test {
                // Skip initial state
                skipItems(1)
                
                viewModel.loadFromPreferences()
                
                // Should start loading
                val loadingState = awaitItem()
                assertTrue(loadingState.isLoading)
                
                // Should set loaded count
                val countSetState = awaitItem()
                assertEquals(25, countSetState.count)
                assertTrue(countSetState.isLoading)
                
                // Should stop loading
                val completedState = awaitItem()
                assertEquals(25, completedState.count)
                assertFalse(completedState.isLoading)
                
                // Should receive load event
                val loadEvent = awaitItem()
                assertTrue(loadEvent is AndroidCounterEvent.CountLoadedFromPreferences)
                assertEquals(25, (loadEvent as AndroidCounterEvent.CountLoadedFromPreferences).count)
            }
        }
    }
    
    @Test
    fun testDatabaseOperationsInAndroidEnvironment() = runTest {
        // Given
        effectHandler.shouldFailDatabase = false
        
        // Test save to database
        viewModel.eventFlow.test {
            viewModel.saveToDatabase(50)
            
            val saveEvent = awaitItem()
            assertEquals(AndroidCounterEvent.CountSavedToDatabase, saveEvent)
        }
        
        // Verify database count was saved
        assertEquals(50, effectHandler.databaseCount)
        
        // Test load from database
        viewModel.eventFlow.test {
            viewModel.loadFromDatabase()
            
            val loadEvent = awaitItem()
            assertTrue(loadEvent is AndroidCounterEvent.CountLoadedFromDatabase)
            assertEquals(50, (loadEvent as AndroidCounterEvent.CountLoadedFromDatabase).count)
        }
    }
    
    @Test
    fun testNetworkSyncInAndroidEnvironment() = runTest {
        // Given
        effectHandler.shouldFailNetwork = false
        
        // When & Then
        viewModel.eventFlow.test {
            viewModel.stateFlow.test {
                // Skip initial state
                skipItems(1)
                
                viewModel.syncWithNetwork()
                
                // Should start loading
                val loadingState = awaitItem()
                assertTrue(loadingState.isLoading)
                
                // Should stop loading after sync
                val completedState = awaitItem()
                assertFalse(completedState.isLoading)
                
                // Should receive sync completed event
                val syncEvent = awaitItem()
                assertEquals(AndroidCounterEvent.NetworkSyncCompleted, syncEvent)
            }
        }
    }
    
    @Test
    fun testErrorHandlingInAndroidEnvironment() = runTest {
        // Given
        effectHandler.shouldFailPreferences = true
        
        // When & Then
        viewModel.eventFlow.test {
            viewModel.saveToPreferences()
            
            val errorEvent = awaitItem()
            assertTrue(errorEvent is AndroidCounterEvent.ShowError)
            assertEquals("Preferences save failed", (errorEvent as AndroidCounterEvent.ShowError).message)
        }
    }
    
    @Test
    fun testNetworkErrorHandlingInAndroidEnvironment() = runTest {
        // Given
        effectHandler.shouldFailNetwork = true
        
        // When & Then
        viewModel.eventFlow.test {
            viewModel.syncWithNetwork()
            
            val errorEvent = awaitItem()
            assertTrue(errorEvent is AndroidCounterEvent.NetworkSyncFailed)
            assertEquals("Network sync failed", (errorEvent as AndroidCounterEvent.NetworkSyncFailed).error)
        }
    }
    
    @Test
    fun testToastEffectInAndroidEnvironment() = runTest {
        // When & Then
        viewModel.eventFlow.test {
            viewModel.showToast("Test message")
            
            val toastEvent = awaitItem()
            assertTrue(toastEvent is AndroidCounterEvent.ToastShown)
            assertEquals("Test message", (toastEvent as AndroidCounterEvent.ToastShown).message)
        }
    }
    
    @Test
    fun testTimestampUpdateInAndroidEnvironment() = runTest {
        // Given
        val initialTimestamp = viewModel.stateFlow.value.lastUpdated
        
        // Wait a bit to ensure timestamp difference
        kotlinx.coroutines.delay(10)
        
        // When & Then
        viewModel.stateFlow.test {
            // Skip initial state
            skipItems(1)
            
            viewModel.updateTimestamp()
            
            val updatedState = awaitItem()
            assertTrue(updatedState.lastUpdated > initialTimestamp)
        }
    }
    
    @Test
    fun testConcurrentOperationsInAndroidEnvironment() = runTest {
        // When & Then
        viewModel.stateFlow.test {
            // Initial state
            assertEquals(0, awaitItem().count)
            
            // Send multiple concurrent operations
            repeat(5) {
                viewModel.increment()
            }
            
            // Should receive all updates
            repeat(5) { i ->
                assertEquals(i + 1, awaitItem().count)
            }
        }
    }
    
    @Test
    fun testComplexWorkflowInAndroidEnvironment() = runTest {
        // Given
        effectHandler.shouldFailPreferences = false
        effectHandler.shouldFailDatabase = false
        
        // Test state changes
        viewModel.stateFlow.test {
            // Initial state
            val initialState = awaitItem()
            assertEquals(0, initialState.count)
            
            // 1. Set count
            viewModel.setCount(10)
            val setState = awaitItem()
            assertEquals(10, setState.count)
            
            // 2. Save to preferences
            viewModel.saveToPreferences()
            
            // Should start loading
            val loadingState1 = awaitItem()
            assertTrue(loadingState1.isLoading)
            
            // Should stop loading
            val completedState1 = awaitItem()
            assertFalse(completedState1.isLoading)
            
            // 3. Save to database
            viewModel.saveToDatabase(10)
            
            // Should start loading
            val loadingState2 = awaitItem()
            assertTrue(loadingState2.isLoading)
            
            // Should stop loading
            val completedState2 = awaitItem()
            assertFalse(completedState2.isLoading)
        }
        
        // Test events separately
        viewModel.eventFlow.test {
            // Save to preferences
            viewModel.saveToPreferences()
            val saveEvent = awaitItem()
            assertEquals(AndroidCounterEvent.CountSavedToPreferences, saveEvent)
            
            // Save to database
            viewModel.saveToDatabase(10)
            val dbSaveEvent = awaitItem()
            assertEquals(AndroidCounterEvent.CountSavedToDatabase, dbSaveEvent)
            
            // Show toast
            viewModel.showToast("Workflow completed")
            val toastEvent = awaitItem()
            assertTrue(toastEvent is AndroidCounterEvent.ToastShown)
            assertEquals("Workflow completed", (toastEvent as AndroidCounterEvent.ToastShown).message)
        }
    }
}
