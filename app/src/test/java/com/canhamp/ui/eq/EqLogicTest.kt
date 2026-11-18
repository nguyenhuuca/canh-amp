package com.canhamp.ui.eq

import com.canhamp.data.eq.BuiltInPresets
import com.canhamp.playback.dsp.DspConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqLogicTest {

    private val rock = BuiltInPresets.all.first { it.name == "Rock" }
    private val onRock = EqLogic.applyPreset(EqModel(DspConfig.FLAT, ""), rock)

    @Test
    fun applyPresetSetsGainsPreampAndName() {
        assertEquals(rock.gains, onRock.config.gainsDb)
        assertEquals("Rock", onRock.presetName)
    }

    @Test
    fun applyPresetKeepsBassTrebleBalance() {
        val m = EqModel(DspConfig(bassDb = 4, trebleDb = 2, balance = 0.5f), "")
        val r = EqLogic.applyPreset(m, rock).config
        assertEquals(4, r.bassDb)
        assertEquals(2, r.trebleDb)
        assertEquals(0.5f, r.balance, 0f)
    }

    @Test
    fun bandEditClearsPresetName() {
        val m = EqLogic.setBand(onRock, 3, 6)
        assertEquals(EqLogic.CUSTOM, m.presetName)
        assertEquals(6, m.config.gainsDb[3])
        assertEquals(rock.gains[0], m.config.gainsDb[0])
    }

    @Test
    fun preampEditClearsPresetName() {
        val m = EqLogic.setPreamp(onRock, -2)
        assertEquals(EqLogic.CUSTOM, m.presetName)
        assertEquals(-2, m.config.preampDb)
    }

    @Test
    fun bassTrebleEnableKeepPresetName() {
        assertEquals("Rock", EqLogic.setBass(onRock, 5).presetName)
        assertEquals("Rock", EqLogic.setTreble(onRock, 5).presetName)
        assertEquals("Rock", EqLogic.setEnabled(onRock, false).presetName)
    }

    @Test
    fun editsAreClamped() {
        assertEquals(12, EqLogic.setBand(onRock, 0, 99).config.gainsDb[0])
        assertEquals(-12, EqLogic.setBand(onRock, 0, -99).config.gainsDb[0])
        assertEquals(0, EqLogic.setBass(onRock, -4).config.bassDb)
        assertEquals(12, EqLogic.setTreble(onRock, 40).config.trebleDb)
        assertEquals(onRock, EqLogic.setBand(onRock, 10, 1))
    }

    @Test
    fun resetFlatAppliesFlatPreset() {
        val m = EqLogic.resetFlat(EqLogic.setBand(onRock, 1, 9).copy(config = onRock.config.copy(preampDb = 5)))
        assertEquals(List(10) { 0 }, m.config.gainsDb)
        assertEquals(0, m.config.preampDb)
        assertEquals("Flat", m.presetName)
    }

    @Test
    fun gainIsPreampMinusPositivePeak() {
        assertEquals(-3.2, EqLogic.gainDb(0, 3.2), 1e-9)
        assertEquals(1.8, EqLogic.gainDb(5, 3.2), 1e-9)
        // No boost: headroom never adds gain.
        assertEquals(2.0, EqLogic.gainDb(2, 0.0), 1e-9)
        assertEquals(-1.0, EqLogic.gainDb(-1, -4.0), 1e-9)
    }

    @Test
    fun gainTextFormat() {
        assertEquals("+3.2 dB", EqLogic.gainValueText(3.24))
        assertEquals("-3.2 dB", EqLogic.gainValueText(-3.2))
        assertEquals("0.0 dB", EqLogic.gainValueText(0.0))
        assertEquals("0.0 dB", EqLogic.gainValueText(-0.04))
        assertEquals("-1.0 dB", EqLogic.gainValueText(-0.96))
    }

    @Test
    fun gainOfRockIsNegativePeakOfBoost() {
        val peak = EqLogic.peakBoostDb(onRock.config, 48_000)
        assertTrue(peak > 0.0)
        assertEquals(-peak, EqLogic.gainDb(0, peak), 1e-9)
    }

    @Test
    fun presetLabel() {
        assertEquals("[ROCK]", EqLogic.presetLabel("Rock"))
        assertEquals("[FULL BASS]", EqLogic.presetLabel("Full Bass"))
        assertNull(EqLogic.presetLabel(""))
        assertNull(EqLogic.presetLabel("  "))
    }

    @Test
    fun bypassedCurveIsFlat() {
        val bypass = EqLogic.setEnabled(onRock, false).config
        val curve = EqLogic.curve(bypass, 44_100)
        assertEquals(EqLogic.CURVE_POINTS, curve.size)
        assertTrue(curve.all { it.second == 0.0 })
        assertEquals(0.0, EqLogic.peakBoostDb(bypass, 44_100), 0.0)
    }

    @Test
    fun enabledCurveFollowsConfig() {
        val curve = EqLogic.curve(onRock.config, null)
        assertEquals(EqLogic.CURVE_POINTS, curve.size)
        assertTrue(curve.any { it.second > 3.0 })
    }

    @Test
    fun labels() {
        assertEquals("60", EqLogic.bandLabel(60))
        assertEquals("1K", EqLogic.bandLabel(1000))
        assertEquals("16K", EqLogic.bandLabel(16000))
        assertEquals("+5", EqLogic.dbLabel(5))
        assertEquals("-1", EqLogic.dbLabel(-1))
        assertEquals("0", EqLogic.dbLabel(0))
    }
}
