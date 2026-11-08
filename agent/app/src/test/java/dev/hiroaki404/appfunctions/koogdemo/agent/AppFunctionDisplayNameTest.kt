package dev.hiroaki404.appfunctions.koogdemo.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppFunctionDisplayNameTest {
    @Test
    fun resourceNameAppendsTheFunctionName() {
        assertEquals("app_function_display_name_getTravelInfo", functionDisplayNameResourceName("getTravelInfo"))
    }

    @Test
    fun namesThatCannotBeResourceNamesHaveNoResource() {
        assertNull(functionDisplayNameResourceName(""))
        assertNull(functionDisplayNameResourceName("a.b"))
    }
}
