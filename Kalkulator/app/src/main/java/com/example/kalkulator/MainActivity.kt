package com.example.kalkulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kalkulator.ui.theme.KalkulatorTheme
import com.example.kalkulator.ui.theme.Orange40
import com.example.kalkulator.ui.theme.Red40
import kotlinx.coroutines.launch
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KalkulatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Kalkulator()
                }
            }
        }
    }
}

@Composable
fun Kalkulator() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("0") }
    var isScientificMode by remember { mutableStateOf(false) }
    var isInverseMode by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    fun handleScientificFunction(functionName: String) {
        if (input.isEmpty() || isOperator(input.last().toString()) || input.last() == '(') {
            input += "$functionName("
        } else {
            val lastOperatorIndex = input.indexOfLast { isOperator(it.toString()) || it == '(' }
            val numberPart = if (lastOperatorIndex != -1) input.substring(lastOperatorIndex + 1) else input
            val prefix = if (lastOperatorIndex != -1) input.substring(0, lastOperatorIndex + 1) else ""
            input = "$prefix$functionName($numberPart)"
        }
    }

    val onButtonClick: (String) -> Unit = { buttonValue ->
        if (output == "Error" && buttonValue !in listOf("AC", "C", "Sc", "inv")) {
            input = ""
            output = "0"
        }

        if (buttonValue !in listOf("sin", "cos", "tan", "inv", "asin", "acos", "atan")) {
            isInverseMode = false
        }

        when (buttonValue) {
            "AC", "C" -> {
                input = ""
                output = "0"
                isInverseMode = false
            }
            "⌫", "<-" -> {
                if (input.isNotEmpty()) {
                    input = input.dropLast(1)
                }
            }
            "Sc" -> {
                isScientificMode = !isScientificMode
                isInverseMode = false
            }
            "inv" -> {
                isInverseMode = !isInverseMode
            }
            "=" -> {
                if (input.isNotEmpty()) {
                    try {
                        val factorial = object : Function("factorial", 1) {
                            override fun apply(vararg args: Double): Double {
                                val num = args[0].toInt()
                                if (num < 0) {
                                    throw IllegalArgumentException("Faktorial tidak terdefinisi untuk angka negatif")
                                }
                                var result = 1.0
                                for (i in 1..num) {
                                    result *= i.toDouble()
                                }
                                return result
                            }
                        }

                        var expressionWithFactorialFunc = input
                        while (expressionWithFactorialFunc.contains("!")) {
                            val factIndex = expressionWithFactorialFunc.lastIndexOf('!')
                            if (factIndex == 0) break

                            var startIndex = -1
                            var expressionToFactor = ""

                            if (expressionWithFactorialFunc[factIndex - 1] == ')') {
                                var bracketCount = 0
                                for (i in factIndex - 1 downTo 0) {
                                    when (expressionWithFactorialFunc[i]) {
                                        ')' -> bracketCount++
                                        '(' -> bracketCount--
                                    }
                                    if (bracketCount == 0) {
                                        startIndex = i
                                        break
                                    }
                                }
                                if (startIndex != -1) {
                                    expressionToFactor = expressionWithFactorialFunc.substring(startIndex, factIndex)
                                }
                            } else if (expressionWithFactorialFunc[factIndex - 1].isDigit() || expressionWithFactorialFunc[factIndex - 1] == '.') {
                                for (i in factIndex - 1 downTo 0) {
                                    val char = expressionWithFactorialFunc[i]
                                    if (!char.isDigit() && char != '.') {
                                        startIndex = i + 1
                                        break
                                    }
                                    if (i == 0) {
                                        startIndex = 0
                                    }
                                }
                                if (startIndex != -1) {
                                    expressionToFactor = expressionWithFactorialFunc.substring(startIndex, factIndex)
                                }
                            }

                            if (startIndex != -1) {
                                val original = "$expressionToFactor!"
                                val replacement = "factorial($expressionToFactor)"
                                expressionWithFactorialFunc = expressionWithFactorialFunc.replaceRange(startIndex, factIndex + 1, replacement)
                            } else {
                                expressionWithFactorialFunc = expressionWithFactorialFunc.removeRange(factIndex, factIndex + 1)
                            }
                        }

                        val expressionString = expressionWithFactorialFunc
                            .replace("x", "*")
                            .replace("÷", "/")
                            .replace("−", "-")
                            .replace("√", "sqrt")
                            .replace("π", Math.PI.toString())
                            .replace("e", Math.E.toString())

                        val openBrackets = expressionString.count { it == '(' }
                        val closeBrackets = expressionString.count { it == ')' }
                        val finalExpression = expressionString + ")".repeat(max(0, openBrackets - closeBrackets))

                        val expression = ExpressionBuilder(finalExpression)
                            .function(factorial)
                            .build()

                        val result = expression.evaluate()

                        output = formatResult(result)
                        input = output
                    } catch (e: Exception) {
                        output = "Error"
                    }
                }
            }
            "%" -> {
                if (input.isNotEmpty() && (input.last().isDigit() || input.last() == ')')) {
                    val lastOperatorIndex = input.indexOfLast { isOperator(it.toString()) }
                    val numberPart = if (lastOperatorIndex != -1) input.substring(lastOperatorIndex + 1) else input

                    try {
                        val numberValue = ExpressionBuilder(numberPart).build().evaluate()
                        val percentageValue = numberValue / 100.0

                        val prefix = if (lastOperatorIndex != -1) input.substring(0, lastOperatorIndex + 1) else ""
                        input = prefix + formatResult(percentageValue)
                    } catch (e: Exception) {
                    }
                }
            }
            "sin", "cos", "tan", "log", "ln", "asin", "acos", "atan" -> {
                val funcName = when (buttonValue) {
                    "asin" -> "asin"
                    "acos" -> "acos"
                    "atan" -> "atan"
                    else -> buttonValue
                }
                handleScientificFunction(funcName)
            }
            "√", "sqrt" -> {
                handleScientificFunction("sqrt")
            }
            "x!" -> {
                if (input.isNotEmpty() && (input.last().isDigit() || input.last() == ')')) {
                    input += "!"
                }
            }
            "1/x" -> {
                handleScientificFunction("1/")
            }
            "x^y" -> {
                if (input.isNotEmpty() && (input.last().isDigit() || input.last() == ')')) {
                    input += "^"
                }
            }
            "e", "π", "(", ")" -> {
                if (buttonValue in listOf("e", "π", "(") && (input.isNotEmpty() && (input.last().isDigit() || input.last() == ')'))) {
                } else {
                    input += buttonValue
                }
            }
            else -> {
                val currentNumber = input.split(Regex("[+\\-x÷^%()]")).last()
                if (isOperator(buttonValue) && (input.isEmpty() || isOperator(input.last().toString()))) {
                } else if (buttonValue == "." && currentNumber.contains(".")) {
                }
                else {
                    input += buttonValue
                }
            }
        }

        coroutineScope.launch {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    val buttonRows: List<List<String>> = if (isScientificMode) {
        val acButton = if (input.isEmpty()) "AC" else "C"
        listOf(
            listOf("log", "ln", "sin", "cos", "tan"),
            listOf("sqrt", "%", "asin", "acos", "atan"),
            listOf("x!", acButton, "<-", "x^y", "/"),
            listOf("1/x", "7", "8", "9", "x"),
            listOf("(", "4", "5", "6", "-"),
            listOf(")", "1", "2", "3", "+"),
            listOf("Sc", "e", "0", ".", "=")
        )
    } else {
        val acButton = if (input.isEmpty()) "AC" else "C"
        listOf(
            listOf(acButton, "<-", "%", "/"),
            listOf("7", "8", "9", "x"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("Sc", "0", ".", "=")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = input,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    fontSize = 30.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = output,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 50.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            buttonRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { buttonLabel ->
                        val buttonModifier = if (row.size > 4) Modifier.weight(1f) else Modifier
                            .weight(1f)
                            .aspectRatio(1f)

                        KalkButton(
                            modifier = buttonModifier,
                            num = buttonLabel,
                            click = { onButtonClick(buttonLabel) },
                            contentColor = when (buttonLabel) {
                                "AC", "C" -> Red40
                                "=" -> MaterialTheme.colorScheme.onPrimary
                                "Sc" -> if (isScientificMode) MaterialTheme.colorScheme.surface else Orange40
                                in listOf("<-", "%", "/", "x", "-", "+", "x^y") -> Orange40
                                in listOf("log", "ln", "sin", "cos", "tan", "sqrt", "asin", "acos", "atan", "x!", "1/x", "(", ")", "e", "π") -> Orange40
                                else -> MaterialTheme.colorScheme.onPrimary
                            },
                            containerColor = when {
                                isScientificMode && buttonLabel == "Sc" -> Orange40
                                buttonLabel == "=" -> Orange40
                                else -> MaterialTheme.colorScheme.primary
                            },
                            fontSize = if(isScientificMode) 18.sp else 22.sp,
                            height = if(isScientificMode) 60.dp else 75.dp,
                            shape = if (isScientificMode) RoundedCornerShape(16.dp) else RoundedCornerShape(24.dp)
                        )
                    }
                }
            }
        }
    }
}

fun isOperator(char: String): Boolean {
    return char in listOf("+", "−", "-", "x", "*", "÷", "/", "^")
}

fun formatResult(result: Double): String {
    if (result.isNaN() || result.isInfinite()) {
        return "Error"
    }

    val formattedString = if (abs(result) > 1e11 || (abs(result) > 0 && abs(result) < 1e-8)) {
        String.format("%.6e", result).replace("e+", "E")
    } else {
        if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format("%.8f", result).replace(",", ".").trimEnd('0').trimEnd('.')
        }
    }

    return if (formattedString.length > 12 && !formattedString.contains("E")) {
        formattedString.substring(0, 12)
    } else {
        formattedString
    }
}


@Composable
fun KalkButton(
    modifier: Modifier = Modifier,
    num: String,
    click: (String) -> Unit,
    contentColor: Color,
    containerColor: Color,
    fontSize: TextUnit = 22.sp,
    height: Dp = 75.dp,
    shape: Shape = RoundedCornerShape(24.dp)
) {
    Button(
        onClick = { click(num) },
        modifier = modifier
            .height(height)
            .padding(vertical = 4.dp),
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        )
    ) {
        Text(text = num, fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            softWrap = false,
            overflow = TextOverflow.Visible
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun KalkulatorPreview() {
    KalkulatorTheme {
        Kalkulator()
    }
}
