package dev.hiroaki404.appfunctions.koogdemo.tool

import androidx.appfunctions.AppFunctionInvalidArgumentException
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculatorFunctionsInstrumentedTest {
    private val service = object : BaseCalculatorAppFunctionService() {
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
        assertInvalidArgument { service.convertCurrency(-1.0, "JPY", "USD") }
        assertInvalidArgument { service.convertCurrency(Double.NaN, "JPY", "USD") }
        assertInvalidArgument { service.convertCurrency(1.0, "GBP", "JPY") }
    }

    private fun assertInvalidArgument(block: () -> Unit) {
        assertThrows(AppFunctionInvalidArgumentException::class.java, block)
    }
}
