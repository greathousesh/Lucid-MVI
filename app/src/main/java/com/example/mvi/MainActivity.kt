package com.example.mvi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.mvi.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: CounterViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
        observeState()
        observeEvents()
    }
    
    private fun setupClickListeners() {
        binding.incrementButton.setOnClickListener {
            viewModel.increment()
        }
        
        binding.decrementButton.setOnClickListener {
            viewModel.decrement()
        }
        
        binding.resetButton.setOnClickListener {
            viewModel.reset()
        }
        
        binding.saveButton.setOnClickListener {
            viewModel.saveCount()
        }
    }
    
    private fun observeState() {
        lifecycleScope.launch {
            viewModel.stateFlow.collect { state ->
                updateUI(state)
            }
        }
    }
    
    private fun observeEvents() {
        lifecycleScope.launch {
            viewModel.eventFlow.collect { event ->
                handleEvent(event)
            }
        }
    }
    
    private fun updateUI(state: CounterState) {
        binding.countText.text = state.count.toString()
        binding.progressBar.isVisible = state.isLoading
        
        // 禁用按钮当正在加载时
        val isEnabled = !state.isLoading
        binding.incrementButton.isEnabled = isEnabled
        binding.decrementButton.isEnabled = isEnabled
        binding.resetButton.isEnabled = isEnabled
        binding.saveButton.isEnabled = isEnabled
    }
    
    private fun handleEvent(event: CounterEvent) {
        when (event) {
            is CounterEvent.CountSaved -> {
                Toast.makeText(this, getString(R.string.count_saved), Toast.LENGTH_SHORT).show()
            }
            is CounterEvent.ShowError -> {
                Toast.makeText(
                    this, 
                    getString(R.string.error_occurred, event.message), 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
