package dev.hiroaki404.appfunctions.koogdemo.tool

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorFunctionsTest {
    private val service = object : BaseCalculatorAppFunctionService() {
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

}
