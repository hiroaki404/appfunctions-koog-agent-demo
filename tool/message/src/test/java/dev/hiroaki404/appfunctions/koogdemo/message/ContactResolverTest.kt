package dev.hiroaki404.appfunctions.koogdemo.message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactResolverTest {
    @Test
    fun resolvesExactName() {
        assertEquals("トム", resolveContact("トム")?.name)
        assertEquals("花子", resolveContact("花子")?.name)
        assertEquals("健太", resolveContact("健太")?.name)
    }

    @Test
    fun trimsWhitespaceIncludingFullWidthSpace() {
        assertEquals("トム", resolveContact("  トム  ")?.name)
        assertEquals("トム", resolveContact("　トム　")?.name)
    }

    @Test
    fun resolvesEnglishAliasCaseInsensitively() {
        assertEquals("トム", resolveContact("tom")?.name)
        assertEquals("トム", resolveContact("TOM")?.name)
        assertEquals("花子", resolveContact("hanako")?.name)
        assertEquals("健太", resolveContact("Kenta")?.name)
    }

    /** Honorifics and nicknames are left to the calling LLM, which the sendMessage description asks to map them. */
    @Test
    fun doesNotResolveHonorificsOrNicknames() {
        assertNull(resolveContact("トムくん"))
        assertNull(resolveContact("花子さん"))
        assertNull(resolveContact("Tommy"))
    }

    @Test
    fun returnsNullForUnknownOrBlankNames() {
        assertNull(resolveContact("太郎"))
        assertNull(resolveContact(""))
        assertNull(resolveContact("   "))
    }
}
