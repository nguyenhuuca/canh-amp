package com.canhamp.data.eq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInPresetsTest {

    private fun gains(name: String) = BuiltInPresets.all.first { it.name == name }.gains

    @Test
    fun orderAndNames() {
        assertEquals(
            listOf("Rock", "Pop", "Techno", "Flat", "Full Bass", "Club", "Reggae", "Vocal"),
            BuiltInPresets.all.map { it.name },
        )
    }

    @Test
    fun gainsMatchSpecB46() {
        assertEquals(listOf(5, 7, 3, -1, -2, 1, 4, 7, 8, 9), gains("Rock"))
        assertEquals(listOf(-1, 2, 4, 5, 4, 2, -1, -2, -2, -2), gains("Pop"))
        assertEquals(listOf(7, 6, 2, -3, -4, 0, 5, 8, 8, 7), gains("Techno"))
        assertEquals(List(10) { 0 }, gains("Flat"))
        assertEquals(listOf(11, 10, 8, 4, 0, -2, -4, -6, -7, -8), gains("Full Bass"))
        assertEquals(listOf(0, 2, 4, 3, 3, 3, 2, 0, 1, 2), gains("Club"))
        assertEquals(listOf(0, 1, -1, -4, -1, 3, 4, 2, 0, 0), gains("Reggae"))
        assertEquals(listOf(-3, -4, -2, 4, 7, 6, 3, 1, -1, -3), gains("Vocal"))
    }

    @Test
    fun preampIsZeroAndNotUser() {
        assertTrue(BuiltInPresets.all.all { it.preamp == 0 && !it.user })
        assertTrue(BuiltInPresets.all.all { it.gains.size == 10 })
    }

    @Test
    fun drawerTags() {
        val tags = BuiltInPresets.all.associate { it.name to it.tag }
        assertEquals("CLASSIC", tags["Rock"])
        assertEquals("CLASSIC", tags["Pop"])
        assertEquals("BASS+", tags["Techno"])
        assertEquals("0dB", tags["Flat"])
        assertEquals("SUB-BOOST", tags["Full Bass"])
        assertEquals("DANCE", tags["Club"])
        assertEquals("WARM", tags["Reggae"])
        assertEquals("MID-RANGE", tags["Vocal"])
    }

    @Test
    fun idsAreUniqueAndNegative() {
        val ids = BuiltInPresets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it < 0 })
    }

    @Test
    fun builtInNameLookupIgnoresCase() {
        assertTrue(BuiltInPresets.isBuiltInName("vOcAl"))
        assertFalse(BuiltInPresets.isBuiltInName("Mine"))
    }
}
