package io.github.mobdev

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CalculatorViewModel : ViewModel() {

    private val _display = MutableLiveData("0")
    val display: LiveData<String> = _display

    private val _expression = MutableLiveData("")
    val expression: LiveData<String> = _expression

    private var operand1: Double? = null
    private var operator: String? = null
    private var currentInput = ""
    private var justCalculated = false

    fun onDigit(digit: String) {
        if (justCalculated) clear()
        justCalculated = false
        if (currentInput == "0") currentInput = digit
        else currentInput += digit
        _display.value = currentInput
    }

    fun onDot() {
        if (justCalculated) clear()
        justCalculated = false
        if (!currentInput.contains(".")) {
            currentInput = if (currentInput.isEmpty()) "0." else "$currentInput."
        }
        _display.value = currentInput
    }

    fun onOperator(op: String) {
        justCalculated = false
        val input = currentInput.toDoubleOrNull()
        if (operand1 != null && operator != null && input != null) {
            val result = calculate(operand1!!, input, operator!!)
            operand1 = result
            _display.value = format(result)
            _expression.value = "${format(result)} $op"
        } else {
            operand1 = input ?: operand1 ?: 0.0
            _expression.value = "${format(operand1!!)} $op"
        }
        operator = op
        currentInput = ""
    }

    fun onEquals() {
        val input = currentInput.toDoubleOrNull() ?: return
        val op = operator ?: return
        val first = operand1 ?: return
        val result = calculate(first, input, op)
        _expression.value = "${format(first)} $op ${format(input)} ="
        _display.value = format(result)
        operand1 = result
        operator = null
        currentInput = format(result)
        justCalculated = true
    }

    fun onClear() = clear()

    fun onSign() {
        val v = currentInput.toDoubleOrNull() ?: return
        currentInput = format(-v)
        _display.value = currentInput
    }

    fun onPercent() {
        val v = currentInput.toDoubleOrNull() ?: return
        currentInput = format(v / 100.0)
        _display.value = currentInput
    }

    fun onBackspace() {
        if (justCalculated) { clear(); return }
        currentInput = currentInput.dropLast(1)
        _display.value = if (currentInput.isEmpty()) "0" else currentInput
    }

    private fun calculate(a: Double, b: Double, op: String) = when (op) {
        "+" -> a + b
        "−" -> a - b
        "×" -> a * b
        "÷" -> if (b != 0.0) a / b else Double.NaN
        else -> b
    }

    private fun format(v: Double): String {
        if (v.isNaN()) return "Ошибка"
        if (v.isInfinite()) return "∞"
        return if (v == Math.floor(v)) v.toLong().toString()
        else "%.10f".format(v).trimEnd('0').trimEnd('.')
    }

    private fun clear() {
        operand1 = null
        operator = null
        currentInput = ""
        justCalculated = false
        _display.value = "0"
        _expression.value = ""
    }
}