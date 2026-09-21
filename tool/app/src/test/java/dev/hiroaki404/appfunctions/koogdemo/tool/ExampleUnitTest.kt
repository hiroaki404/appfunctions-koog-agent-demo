package dev.hiroaki404.appfunctions.koogdemo.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorFunctionsTest {
    private val service = object : BaseCalculatorAppFunctionService() {
        override val appFunctionLogger: AppFunctionLogger = NoOpAppFunctionLogger

        override fun onExecuteFunction(
            request: androidx.appfunctions.ExecuteAppFunctionRequest,
            cancellationSignal: android.os.CancellationSignal,
            callback: java.util.function.Consumer<androidx.appfunctions.ExecuteAppFunctionResponse>,
        ) = error("This unit test invokes the calculator methods directly.")
    }

    @Test
    fun addReturnsSum() {
        assertEquals(4, service.add(2, 2))
    }

    @Test
    fun calculateTotalUsesZeroPercentDiscountByDefault() {
        val total = service.calculateTotal(
            listOf(
                LineItem(name = "Notebook", unitPrice = 1_000, quantity = 2),
                LineItem(name = "Pen", unitPrice = 500, quantity = 3),
            ),
        )

        assertEquals(3_500, total)
    }

    @Test
    fun calculateTotalAppliesDiscountAtBoundary() {
        val total = service.calculateTotal(
            items = listOf(LineItem(name = "Notebook", unitPrice = 1_000, quantity = 2)),
            discountPercent = 100,
        )

        assertEquals(0, total)
    }

    @Test
    fun convertCurrencyLogsFunctionArgumentsAndResult() {
        val logger = RecordingAppFunctionLogger()
        val service = testService(logger)

        assertEquals(1_000.0, service.convertCurrency(10.0, "USD", "JPY"), 0.0)
        assertEquals(listOf("convertCurrency"), logger.startedFunctions)
        assertTrue(logger.startedArguments.single().contains("amount=10.0"))
        assertEquals(listOf("convertCurrency"), logger.succeededFunctions)
        assertEquals(listOf("1000.0"), logger.results)
    }

    private fun testService(logger: AppFunctionLogger) = object : BaseCalculatorAppFunctionService() {
        override val appFunctionLogger: AppFunctionLogger = logger

        override fun onExecuteFunction(
            request: androidx.appfunctions.ExecuteAppFunctionRequest,
            cancellationSignal: android.os.CancellationSignal,
            callback: java.util.function.Consumer<androidx.appfunctions.ExecuteAppFunctionResponse>,
        ) = error("This unit test invokes the calculator methods directly.")
    }

    private class RecordingAppFunctionLogger : AppFunctionLogger {
        val startedFunctions = mutableListOf<String>()
        val startedArguments = mutableListOf<String>()
        val succeededFunctions = mutableListOf<String>()
        val results = mutableListOf<String>()

        override fun started(functionName: String, arguments: String) {
            startedFunctions += functionName
            startedArguments += arguments
        }

        override fun succeeded(functionName: String, arguments: String, result: Any) {
            succeededFunctions += functionName
            results += result.toString()
        }

        override fun failed(functionName: String, arguments: String, error: Exception) = Unit
    }

    private object NoOpAppFunctionLogger : AppFunctionLogger {
        override fun started(functionName: String, arguments: String) = Unit
        override fun succeeded(functionName: String, arguments: String, result: Any) = Unit
        override fun failed(functionName: String, arguments: String, error: Exception) = Unit
    }

}
