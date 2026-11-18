package com.canhamp.ui.eq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.canhamp.CanhAmpApp
import com.canhamp.data.eq.EqPreset
import com.canhamp.data.eq.EqRepository
import com.canhamp.data.eq.SaveResult
import com.canhamp.data.settings.SettingsRepository
import com.canhamp.playback.dsp.DspConfig
import com.canhamp.playback.dsp.DspEngine
import com.canhamp.playback.dsp.DspStatus
import com.canhamp.ui.main.DeckFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EqUiState(
    val bands: List<Int> = List(DspConfig.BAND_COUNT) { 0 },
    val preamp: Int = 0,
    val bass: Int = 0,
    val treble: Int = 0,
    val balance: Float = 0f,
    val enabled: Boolean = true,
    /** Active preset name, empty for custom. */
    val presetName: String = "",
    /** "[ROCK]", or null for custom (the screen shows the localised [CUSTOM]). */
    val presetLabel: String? = null,
    /** "+3.2 dB" (B49). */
    val gainText: String = EqLogic.gainValueText(0.0),
    val curve: List<Pair<Double, Double>> = emptyList(),
    val dspStatus: DspStatus = DspStatus.ACTIVE,
    val sampleRateHz: Int? = null,
    val presets: List<EqPreset> = emptyList(),
)

@OptIn(FlowPreview::class)
class EqViewModel(
    private val dsp: DspEngine,
    private val settings: SettingsRepository,
    private val repo: EqRepository,
    sampleRateHz: Flow<Int?>,
    /** Outlives the view model so a pending write still lands when the screen closes. */
    private val persistScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : ViewModel() {

    private val presetName = MutableStateFlow<String?>(null)
    private val dirty = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    @Volatile
    private var pendingWrite = false

    val state: StateFlow<EqUiState> = combine(
        dsp.config,
        presetName.filterNotNull(),
        repo.presets(),
        dsp.status,
        sampleRateHz,
    ) { config, name, presets, status, rate ->
        EqUiState(
            bands = config.gainsDb,
            preamp = config.preampDb,
            bass = config.bassDb,
            treble = config.trebleDb,
            balance = config.balance,
            enabled = config.eqEnabled,
            presetName = name,
            presetLabel = EqLogic.presetLabel(name),
            gainText = EqLogic.gainValueText(EqLogic.gainDb(config.preampDb, EqLogic.peakBoostDb(config, rate))),
            curve = EqLogic.curve(config, rate),
            dspStatus = status,
            sampleRateHz = rate,
            presets = presets,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EqUiState())

    init {
        viewModelScope.launch { presetName.value = settings.settings.first().eqPresetName }
        // B52: persist 300 ms after the last change.
        viewModelScope.launch { dirty.debounce(PERSIST_DEBOUNCE_MS).collect { persist() } }
    }

    fun setBand(index: Int, db: Int) = edit { EqLogic.setBand(it, index, db) }
    fun setPreamp(db: Int) = edit { EqLogic.setPreamp(it, db) }
    fun setBass(db: Int) = edit { EqLogic.setBass(it, db) }
    fun setTreble(db: Int) = edit { EqLogic.setTreble(it, db) }
    fun toggleEnabled() = edit { EqLogic.setEnabled(it, !it.config.eqEnabled) }
    fun applyPreset(preset: EqPreset) = edit { EqLogic.applyPreset(it, preset) }
    fun resetFlat() = edit { EqLogic.resetFlat(it) }

    fun setBalance(raw: Float) {
        val b = DeckFormat.snapBalance(raw)
        edit { it.copy(config = it.config.copy(balance = b)) }
    }

    /** Saves the current gains and preamp under [name]; the saved preset becomes the active one. */
    fun savePreset(name: String, overwrite: Boolean, onResult: (SaveResult) -> Unit) {
        viewModelScope.launch {
            val config = dsp.current()
            val result = repo.savePreset(name, config.gainsDb, config.preampDb, overwrite)
            if (result is SaveResult.Saved) {
                presetName.value = result.name
                markDirty()
            }
            onResult(result)
        }
    }

    fun deletePreset(preset: EqPreset) {
        viewModelScope.launch {
            repo.deletePreset(preset.id)
            if (presetName.value.equals(preset.name, ignoreCase = true)) {
                presetName.value = EqLogic.CUSTOM
                markDirty()
            }
        }
    }

    /** Applies [change] to the engine at once (B43) and schedules the debounced write. */
    private fun edit(change: (EqModel) -> EqModel) {
        val name = presetName.value ?: return
        var next: EqModel? = null
        dsp.update { config ->
            change(EqModel(config, name)).also { next = it }.config
        }
        next?.let { presetName.value = it.presetName }
        markDirty()
    }

    private fun markDirty() {
        pendingWrite = true
        dirty.tryEmit(Unit)
    }

    private suspend fun persist() {
        pendingWrite = false
        val c = dsp.current()
        settings.setEq(c.eqEnabled, c.gainsDb.toIntArray(), c.preampDb, presetName.value.orEmpty())
        settings.setBassTreble(c.bassDb, c.trebleDb)
        settings.setBalance(c.balance)
    }

    override fun onCleared() {
        if (pendingWrite) persistScope.launch { persist() }
    }

    companion object {
        const val PERSIST_DEBOUNCE_MS = 300L
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as CanhAmpApp).container
                val rate = c.playerConnection.nowPlaying.map { it.sampleRateHz }.distinctUntilChanged()
                EqViewModel(c.dspEngine, c.settingsRepository, c.eqRepository, rate)
            }
        }
    }
}
