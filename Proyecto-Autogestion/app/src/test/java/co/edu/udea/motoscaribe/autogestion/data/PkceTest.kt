package co.edu.udea.motoscaribe.autogestion.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PkceTest {

    @Test fun challengeMatchesRfc7636Vector() {
        // Apendice B del RFC 7636.
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challengeOf("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test fun verifierIsRandomAndValidLength() {
        val a = Pkce.newVerifier()
        val b = Pkce.newVerifier()
        assertNotEquals(a, b)
        assertTrue(a.length in 43..128)
        assertTrue(a.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }
}
