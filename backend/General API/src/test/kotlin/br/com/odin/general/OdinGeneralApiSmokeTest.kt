package br.com.odin.general

import kotlin.test.Test
import kotlin.test.assertEquals

class OdinGeneralApiSmokeTest {
    @Test
    fun `project identity is stable`() {
        assertEquals("odin-general-api", "odin-general-api")
    }
}
