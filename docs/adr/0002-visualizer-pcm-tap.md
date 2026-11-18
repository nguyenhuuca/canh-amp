# ADR-0002: Spectrum visualizer from an in-pipeline PCM tap

**Status:** Accepted (2026-11-18) · **Deciders:** project owner · **Tags:** audio, permissions, ui

## Context

The main deck shows a 16-band spectrum analyser and a wave mode that move with the music (PRD R10). The PRD also says
the app must not ask for the microphone (R10 acceptance: no `RECORD_AUDIO` in the manifest) and has no network
(R30). ADR-0001 already puts our own `AudioProcessor` chain in ExoPlayer.

`android.media.audiofx.Visualizer` requires `RECORD_AUDIO` (a runtime "microphone" prompt). Its output is capped at
1024-sample captures, and it returns zeros on several OEM builds or when another effect is attached.

## Options

### A. Platform `Visualizer` on the player session
- **Cost:** low.
- **Risk:** it forces the microphone permission and a scary prompt. It is unreliable across OEMs.
- **Reversibility:** easy.

### B. `SpectrumTapProcessor` at the end of the DSP chain
- **How:** the last processor in the ADR-0001 chain:
  1. Pass-through copies the output PCM, downmixed to mono float, into a lock-free ring buffer of 4096 samples.
  2. A `SpectrumAnalyzer` coroutine on `Dispatchers.Default` wakes at most 30 times per second while the deck is visible. It takes the latest 1024 samples, applies a Hann window and a real FFT, then maps the bins to 16 log-spaced bands (50 Hz–16 kHz) in dB.
  3. It applies attack/decay smoothing and peak-hold, and emits a `SpectrumFrame(bands: FloatArray, wave: FloatArray)` through a `StateFlow`.
- **Cost:** about 250 lines, unit-testable with sine inputs.
- **Risk:** the bars run ahead of what is heard by the sink's buffer, typically 100–250 ms. Compensate by delaying frames by `AudioTrack` latency where the platform reports it (should).
- **Reversibility:** easy.

### C. Fake animation (random bars, as in the Stitch HTML)
- **Cost:** none.
- **Risk:** fails R10 ("bars move with the audio").

## Decision

**B.** No permission, identical on all devices, and it reuses the ADR-0001 chain. We accept a small latency offset and a
little CPU while the deck is visible. The analyser stops whenever no collector is active (`WhileSubscribed`), so there is
no cost in the background.

## Consequences

- Classes `SpectrumTapProcessor`, `RingBuffer`, `Fft` (radix-2, in-house, 1024 points) and `SpectrumAnalyzer` in `com.canhamp.playback.dsp`.
- The UI collects `DspEngine.spectrum` with `collectAsStateWithLifecycle`, so collection stops when the screen is not visible.
- The manifest has no `RECORD_AUDIO` or `MODIFY_AUDIO_SETTINGS`. A CI check asserts this (spec B71).
- Tests: a 1 kHz sine peaks in the band that contains 1 kHz, silence gives all bands at the floor, and the band edges are monotonic.

## Changelog

- 2026-11-18: created.
