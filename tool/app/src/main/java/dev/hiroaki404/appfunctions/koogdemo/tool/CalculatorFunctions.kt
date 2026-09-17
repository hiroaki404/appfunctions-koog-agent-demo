package dev.hiroaki404.appfunctions.koogdemo.tool

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

/**
 * A single line item in a purchase: [name] of the item, [unitPrice] per unit, and [quantity]
 * purchased.
 *
 * Nested inside a `List<LineItem>` argument, this exercises both the "nested object" and
 * "array" branches of the metadata -> schema conversion on the agent side.
 */
@AppFunctionSerializable
data class LineItem(val name: String, val unitPrice: Long, val quantity: Long)

/**
 * A minimal AppFunctions smoke-test target: adds two integers.
 *
 * This is intentionally the simplest possible function (no nested objects) so that the tool
 * app's AppFunctions + KSP wiring can be verified end to end before richer functions are added.
 *
 * Since androidx.appfunctions 1.0.0-alpha10, `@AppFunction` methods must live inside a service
 * annotated with `@AppFunctionServiceEntryPoint`. The compiler generates a concrete
 * `CalculatorAppFunctionService` (declared in AndroidManifest.xml) plus an XML describing the
 * exposed functions.
 */
@AppFunctionServiceEntryPoint(
    serviceName = "CalculatorAppFunctionService",
    appFunctionXmlFileName = "calculator_functions",
)
abstract class BaseCalculatorAppFunctionService : AppFunctionService() {
    /**
     * Adds [num1] and [num2] and returns the sum.
     */
    @AppFunction(isDescribedByKDoc = true)
    fun add(num1: Long, num2: Long): Long {
        return num1 + num2
    }

    /**
     * Sums `unitPrice * quantity` across [items], then subtracts [discountPercent]
     * (0-100) from the subtotal.
     */
    @AppFunction(isDescribedByKDoc = true)
    fun calculateTotal(items: List<LineItem>, discountPercent: Long): Long {
        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        return subtotal - (subtotal * discountPercent / 100)
    }
}
