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
    fun stripsHonorifics() {
        assertEquals("トム", resolveContact("トムくん")?.name)
        assertEquals("トム", resolveContact("トム君")?.name)
        assertEquals("花子", resolveContact("花子さん")?.name)
        assertEquals("花子", resolveContact("花子ちゃん")?.name)
        assertEquals("健太", resolveContact("健太様")?.name)
        assertEquals("健太", resolveContact("健太さま")?.name)
    }

    @Test
    fun trimsWhitespaceIncludingFullWidthSpace() {
        assertEquals("トム", resolveContact("  トム  ")?.name)
        assertEquals("トム", resolveContact("　トムくん　")?.name)
    }

    @Test
    fun resolvesEnglishAliasCaseInsensitively() {
        assertEquals("トム", resolveContact("tom")?.name)
        assertEquals("トム", resolveContact("TOM")?.name)
        assertEquals("花子", resolveContact("hanako")?.name)
        assertEquals("健太", resolveContact("Kenta")?.name)
    }

    @Test
    fun resolvesEnglishAliasWithHonorific() {
        assertEquals("トム", resolveContact("Tomさん")?.name)
        assertEquals("花子", resolveContact("Hanakoちゃん")?.name)
    }

    @Test
    fun returnsNullForUnknownOrBlankNames() {
        assertNull(resolveContact("太郎"))
        assertNull(resolveContact(""))
        assertNull(resolveContact("   "))
    }

    @Test
    fun honorificAloneDoesNotResolve() {
        assertNull(resolveContact("くん"))
        assertNull(resolveContact("さん"))
    }
}
