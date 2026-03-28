package io.github.mobdev

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.github.mobdev.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeViewModel()
        setupButtons()
    }

    private fun observeViewModel() {
        viewModel.display.observe(this) { text ->
            binding.tvDisplay.text = text
            binding.tvDisplay.textSize = when {
                text.length > 12 -> 32f
                text.length > 8 -> 44f
                else -> 56f
            }
        }
        viewModel.expression.observe(this) { text ->
            binding.tvExpression.text = text
        }
    }

    private fun setupButtons() = with(binding) {
        btn0.setOnClickListener { viewModel.onDigit("0") }
        btn1.setOnClickListener { viewModel.onDigit("1") }
        btn2.setOnClickListener { viewModel.onDigit("2") }
        btn3.setOnClickListener { viewModel.onDigit("3") }
        btn4.setOnClickListener { viewModel.onDigit("4") }
        btn5.setOnClickListener { viewModel.onDigit("5") }
        btn6.setOnClickListener { viewModel.onDigit("6") }
        btn7.setOnClickListener { viewModel.onDigit("7") }
        btn8.setOnClickListener { viewModel.onDigit("8") }
        btn9.setOnClickListener { viewModel.onDigit("9") }
        btnDot.setOnClickListener { viewModel.onDot() }
        btnPlus.setOnClickListener { viewModel.onOperator("+") }
        btnMinus.setOnClickListener { viewModel.onOperator("−") }
        btnMul.setOnClickListener { viewModel.onOperator("×") }
        btnDiv.setOnClickListener { viewModel.onOperator("÷") }
        btnEquals.setOnClickListener { viewModel.onEquals() }
        btnClear.setOnClickListener { viewModel.onClear() }
        btnSign.setOnClickListener { viewModel.onSign() }
        btnPercent.setOnClickListener { viewModel.onPercent() }
        btnBackspace.setOnClickListener { viewModel.onBackspace() }
    }
}