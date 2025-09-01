package com.example.mvi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mvi.databinding.ActivityTimeTravelDebugBinding
import com.greathouse.mvi.middleware.TimeTravelMiddleware
import kotlinx.coroutines.launch

class TimeTravelDebugActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTimeTravelDebugBinding
    private val viewModel: TodoViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeTravelDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeState()
        
        // Load initial data
        viewModel.loadTodos()
    }
    
    private fun setupUI() {
        // Setup history RecyclerView
        historyAdapter = HistoryAdapter { index ->
            viewModel.jumpToState(index)
        }
        
        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(this@TimeTravelDebugActivity)
            adapter = historyAdapter
        }
        
        // Setup buttons
        binding.btnStepBack.setOnClickListener {
            android.util.Log.d("TimeTravelDebug", "Step back clicked")
            val result = viewModel.stepBack()
            android.util.Log.d("TimeTravelDebug", "Step back result: $result")
        }
        
        binding.btnStepForward.setOnClickListener {
            android.util.Log.d("TimeTravelDebug", "Step forward clicked")
            val result = viewModel.stepForward()
            android.util.Log.d("TimeTravelDebug", "Step forward result: $result")
        }
        
        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
            updateHistoryDisplay()
        }
        
        // Add some test actions
        binding.btnAddTodo.setOnClickListener {
            val title = "Test Todo ${System.currentTimeMillis() % 1000}"
            viewModel.addTodo(title, "Generated for testing")
        }
        
        binding.btnToggleFirst.setOnClickListener {
            val firstTodo = viewModel.stateFlow.value.todos.firstOrNull()
            firstTodo?.let { viewModel.toggleTodo(it.id) }
        }
        
        binding.btnDeleteFirst.setOnClickListener {
            val firstTodo = viewModel.stateFlow.value.todos.firstOrNull()
            firstTodo?.let { viewModel.deleteTodo(it.id) }
        }
    }
    
    private fun observeState() {
        lifecycleScope.launch {
            viewModel.stateFlow.collect { state ->
                updateStateDisplay(state)
                updateHistoryDisplay()
            }
        }
        
        // Observe time travel current index changes
        lifecycleScope.launch {
            viewModel.getTimeTravelMiddleware()?.currentIndex?.collect {
                updateHistoryDisplay()
            }
        }
        
        lifecycleScope.launch {
            viewModel.eventFlow.collect { event ->
                when (event) {
                    is TodoEvent.ShowError -> {
                        Toast.makeText(this@TimeTravelDebugActivity, event.message, Toast.LENGTH_SHORT).show()
                    }
                    is TodoEvent.ShowSuccess -> {
                        Toast.makeText(this@TimeTravelDebugActivity, event.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun updateStateDisplay(state: TodoState) {
        val stats = viewModel.getStats(state)
        binding.textCurrentState.text = """
            Current State:
            Total Todos: ${stats.total}
            Active: ${stats.active}
            Completed: ${stats.completed}
            Loading: ${state.isLoading}
            Filter: ${state.filter}
        """.trimIndent()
    }
    
    private fun updateHistoryDisplay() {
        val timeTravelMiddleware = viewModel.getTimeTravelMiddleware()
        val history = viewModel.getHistory()
        val currentIndex = timeTravelMiddleware?.currentIndex?.value ?: -1
        
        android.util.Log.d("TimeTravelDebug", "updateHistoryDisplay: middleware=$timeTravelMiddleware, history.size=${history.size}, currentIndex=$currentIndex")
        
        historyAdapter.updateHistory(history, currentIndex)
        
        binding.textHistoryInfo.text = """
            History: ${history.size} states
            Current Index: $currentIndex
            Middleware: ${if (timeTravelMiddleware != null) "Found" else "Not Found"}
        """.trimIndent()
        
        // Update button states
        binding.btnStepBack.isEnabled = currentIndex > 0
        binding.btnStepForward.isEnabled = currentIndex < history.size - 1
    }
}

class HistoryAdapter(
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    
    private var history: List<TimeTravelMiddleware.StateSnapshot<TodoState, TodoAction>> = emptyList()
    private var currentIndex: Int = -1
    
    fun updateHistory(
        newHistory: List<TimeTravelMiddleware.StateSnapshot<TodoState, TodoAction>>,
        newCurrentIndex: Int
    ) {
        history = newHistory
        currentIndex = newCurrentIndex
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): HistoryViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return HistoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val snapshot = history[position]
        val isCurrent = position == currentIndex
        
        holder.bind(snapshot, position, isCurrent) {
            onItemClick(position)
        }
    }
    
    override fun getItemCount(): Int = history.size
    
    class HistoryViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val text1: android.widget.TextView = itemView.findViewById(android.R.id.text1)
        private val text2: android.widget.TextView = itemView.findViewById(android.R.id.text2)
        
        fun bind(
            snapshot: TimeTravelMiddleware.StateSnapshot<TodoState, TodoAction>,
            position: Int,
            isCurrent: Boolean,
            onClick: () -> Unit
        ) {
            text1.text = "${if (isCurrent) "► " else ""}#$position: ${snapshot.action?.let { it::class.simpleName } ?: "Initial"}"
            text2.text = "Todos: ${snapshot.state.todos.size}, Time: ${java.text.SimpleDateFormat("HH:mm:ss").format(snapshot.timestamp)}"
            
            itemView.setBackgroundColor(
                if (isCurrent) 0xFFE3F2FD.toInt() else 0xFFFFFFFF.toInt()
            )
            
            itemView.setOnClickListener { onClick() }
        }
    }
}
