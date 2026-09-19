package dev.hiroaki404.appfunctions.koogdemo.tool

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import java.math.BigInteger
import java.util.Locale

/** A single purchase line item used to calculate an order total. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class LineItem(
    /** The item name, used only to identify this line item in a request. */
    val name: String,
    /** The non-negative unit price in whole Japanese yen. */
    val unitPrice: Long,
    /** The non-negative number of units to purchase. */
    val quantity: Long,
)

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
private val demoExchangeRatesToJpy = mapOf(
    "JPY" to 1.0,
    "USD" to 100.0,
    "EUR" to 200.0,
)

private val hundred = BigInteger.valueOf(100)

@AppFunctionServiceEntryPoint(
    serviceName = "CalculatorAppFunctionService",
    appFunctionXmlFileName = "calculator_functions",
)
abstract class BaseCalculatorAppFunctionService : AppFunctionService() {
    /**
     * Add two integers and return their sum.
     *
     * @param num1 The first integer.
     * @param num2 The second integer.
     * @return The sum of [num1] and [num2].
     * @throws AppFunctionInvalidArgumentException If the sum cannot be represented as a Long;
     *   use smaller absolute values and retry.
     */
    @AppFunction(isDescribedByKDoc = true)
    fun add(num1: Long, num2: Long): Long {
        return try {
            Math.addExact(num1, num2)
        } catch (_: ArithmeticException) {
            throw AppFunctionInvalidArgumentException(
                "The sum must fit in a signed 64-bit integer. Use smaller absolute values.",
            )
        }
    }

    /**
     * Calculate a total in whole Japanese yen for all line items.
     *
     * @param items The line items to total. Every unit price and quantity must be non-negative,
     *   and all prices are whole Japanese yen.
     * @param discountPercent The discount percentage from 0 through 100. Defaults to 0.
     * @return The discounted total in whole Japanese yen.
     * @throws AppFunctionInvalidArgumentException If a price or quantity is negative, the discount
     *   is outside 0 through 100, or the result cannot be represented as a Long. Correct the
     *   invalid value or split the calculation and retry.
     */
    @AppFunction(isDescribedByKDoc = true)
    fun calculateTotal(items: List<LineItem>, discountPercent: Long = 0): Long {
        if (discountPercent !in 0..100) {
            throw AppFunctionInvalidArgumentException(
                "discountPercent must be between 0 and 100 inclusive.",
            )
        }
        items.forEach { item ->
            if (item.unitPrice < 0) {
                throw AppFunctionInvalidArgumentException(
                    "unitPrice for '${item.name}' must be non-negative.",
                )
            }
            if (item.quantity < 0) {
                throw AppFunctionInvalidArgumentException(
                    "quantity for '${item.name}' must be non-negative.",
                )
            }
        }

        return try {
            val subtotal = items.fold(BigInteger.ZERO) { total, item ->
                total + BigInteger.valueOf(item.unitPrice) * BigInteger.valueOf(item.quantity)
            }
            val discount = subtotal * BigInteger.valueOf(discountPercent) / hundred
            (subtotal - discount).longValueExact()
        } catch (_: ArithmeticException) {
            throw AppFunctionInvalidArgumentException(
                "The total must fit in a signed 64-bit integer. Split the calculation and retry.",
            )
        }
    }

    /**
     * Convert a non-negative amount between supported currencies using fixed demo exchange rates.
     *
     * @param amount The non-negative finite amount to convert.
     * @param from The source currency code, case-insensitively: JPY, USD, or EUR.
     * @param to The target currency code, case-insensitively: JPY, USD, or EUR.
     * @return The converted amount using fixed rates of 1 JPY, 100 JPY per USD, and 200 JPY per
     *   EUR.
     * @throws AppFunctionInvalidArgumentException If the amount is negative or non-finite, a
     *   currency code is unsupported, or the result is non-finite. Correct the input and retry.
     */
    @AppFunction(isDescribedByKDoc = true)
    fun convertCurrency(amount: Double, from: String, to: String): Double {
        if (!amount.isFinite() || amount < 0) {
            throw AppFunctionInvalidArgumentException(
                "amount must be a non-negative finite number.",
            )
        }
        val fromCode = from.trim().uppercase(Locale.ROOT)
        val toCode = to.trim().uppercase(Locale.ROOT)
        val fromRate = demoExchangeRatesToJpy[fromCode]
            ?: throw AppFunctionInvalidArgumentException(
                "Unsupported currency code: $fromCode. Supported codes are JPY, USD, EUR.",
            )
        val toRate = demoExchangeRatesToJpy[toCode]
            ?: throw AppFunctionInvalidArgumentException(
                "Unsupported currency code: $toCode. Supported codes are JPY, USD, EUR.",
            )
        return (amount * fromRate / toRate).also { convertedAmount ->
            if (!convertedAmount.isFinite()) {
                throw AppFunctionInvalidArgumentException(
                    "The converted amount must be a finite number. Use a smaller amount.",
                )
            }
        }
    }
}
