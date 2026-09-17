package dev.hiroaki404.appfunctions.koogdemo.tool

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

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
}
