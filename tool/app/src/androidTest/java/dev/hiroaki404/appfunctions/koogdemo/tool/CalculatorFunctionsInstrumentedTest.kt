package dev.hiroaki404.appfunctions.koogdemo.tool

import androidx.appfunctions.AppFunctionInvalidArgumentException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculatorFunctionsInstrumentedTest {
    private val failedFunctions = mutableListOf<String>()
    private val service = object : BaseCalculatorAppFunctionService() {
        override val appFunctionLogger = object : AppFunctionLogger {
            override fun started(functionName: String, arguments: String) = Unit
            override fun succeeded(functionName: String, arguments: String, result: Any) = Unit
            override fun failed(functionName: String, arguments: String, error: Exception) {
                failedFunctions += functionName
            }
        }

        override fun onExecuteFunction(
            request: androidx.appfunctions.ExecuteAppFunctionRequest,
            cancellationSignal: android.os.CancellationSignal,
            callback: java.util.function.Consumer<androidx.appfunctions.ExecuteAppFunctionResponse>,
        ) = error("This instrumentation test invokes the calculator methods directly.")
    }

    @Test
    fun calculateTotalRejectsInvalidInputsAndOverflow() {
        assertInvalidArgument {
            service.calculateTotal(listOf(LineItem(name = "Invalid price", unitPrice = -1, quantity = 1)))
        }
        assertInvalidArgument {
            service.calculateTotal(listOf(LineItem(name = "Invalid quantity", unitPrice = 1, quantity = -1)))
        }
        assertInvalidArgument {
            service.calculateTotal(emptyList(), discountPercent = 101)
        }
        assertInvalidArgument {
            service.calculateTotal(listOf(LineItem(name = "Overflow", unitPrice = Long.MAX_VALUE, quantity = 2)))
        }
    }

    @Test
    fun convertCurrencyRejectsInvalidInputs() {
        failedFunctions.clear()
        assertInvalidArgument { service.convertCurrency(-1.0, "JPY", "USD") }
        assertInvalidArgument { service.convertCurrency(Double.NaN, "JPY", "USD") }
        assertInvalidArgument { service.convertCurrency(1.0, "GBP", "JPY") }
        assertEquals(listOf("convertCurrency", "convertCurrency", "convertCurrency"), failedFunctions)
    }

    private fun assertInvalidArgument(block: () -> Unit) {
        assertThrows(AppFunctionInvalidArgumentException::class.java, block)
    }
}
