# ADR-0001: In-app DSP chain on Media3 AudioProcessors

**Status:** Accepted (2026-11-18) · **Deciders:** project owner · **Tags:** audio, playback

## Context

The PRD (`docs/prd/PRD-retro-player.md`) asks for:

- a 10-band graphic EQ at fixed frequencies 60, 170, 310, 600 Hz, 1, 3, 6, 12, 14 and 16 kHz, each ±12 dB in 1 dB steps, plus a preamp (R19);
- a bypass that is bit-exact (R20);
- bass boost and treble (R22), balance (R11) and no audible clipping (R23).

It must behave the same on every phone (G4). There is no code yet. Playback will use Media3 ExoPlayer (PRD R8).

`android.media.audiofx.Equalizer` exposes whatever bands the device's effect engine has, often 5 with OEM-chosen centres
and gain ranges. It can only be attached to an audio session, and some OEMs disable it or apply it globally.

## Options

### A. Platform `audiofx` (Equalizer + BassBoost)
- **How:** attach to the ExoPlayer audio session id, then map the 10 UI bands onto the device bands by interpolation.
- **Cost:** low.
- **Risk:** the UI lies, because a "60 Hz" slider moves a "100 Hz" band. Band count and range vary by device, so R19's acceptance (+6 dB at 1 kHz ±1 dB) fails on many phones. Balance and limiting are not available.
- **Reversibility:** easy.

### B. Custom `AudioProcessor`s in ExoPlayer's `DefaultAudioSink`
- **How:** a chain of processors runs on decoded PCM:
  1. `EqualizerProcessor`: preamp, 10 RBJ peaking biquads (Q ≈ 1.41), a bass low-shelf at 100 Hz and a treble high-shelf at 10 kHz.
  2. `BalanceProcessor`: per-channel gain.
  3. `LimiterProcessor`: peak limiter at −0.1 dBFS.
  4. `SpectrumTapProcessor`: see ADR-0002.

  Processing is in float internally, with input and output in 16-bit PCM. The processors read an immutable
  `DspConfig` snapshot published by `DspEngine` through an `AtomicReference`, so the UI never blocks the audio thread.
- **Cost:** medium, about 600 lines of pure Kotlin that are fully JVM unit-testable with synthetic sine waves. CPU for 13 biquads × 2 channels × 48 kHz is about 1.2 M multiply-adds per second, well under 1% of a modern core.
- **Risk:** our own DSP bugs (mitigated by frequency-response tests). Audio only flows through the chain when PCM is decoded in-app: offload and passthrough must stay disabled, which is the ExoPlayer default.
- **Reversibility:** moderate. The UI talks only to `DspEngine`, so the engine could swap to option A without touching screens.

### C. Native DSP (C++/Oboe, or a library such as SuperpoweredSDK)
- **How:** as B, but the processors run in native code.
- **Cost:** high, with an NDK toolchain in CI and JNI.
- **Risk:** licence terms (Superpowered is commercial), and native crashes.
- **Reversibility:** hard.

## Decision

**B.** It is the only option that meets R19, R20, R22, R23 and R11 identically on every device, and it stays inside
Kotlin and the JVM test suite that CI already runs. We give up the OEM effect engines (for example Dolby, which the user
can still enable system-wide) and accept owning the DSP maths.

Processors accept and return `C.ENCODING_PCM_16BIT`. Float output (`setEnableFloatOutput`) is **not** enabled. How
it interacts with audio processors has changed between ExoPlayer/Media3 versions, and 16-bit I/O with float math
inside is enough for an EQ and limiter. A processor that receives any other encoding reports itself inactive, and the
UI shows "DSP unavailable for this format" (spec B42).

## Consequences

- New package `com.canhamp.playback.dsp`: `Biquad`, `BiquadDesign`, `EqualizerProcessor`, `BalanceProcessor`, `LimiterProcessor`, `DspConfig`, `DspEngine`.
- `PlayerFactory` builds ExoPlayer with a custom `RenderersFactory` whose `buildAudioSink` adds the chain in that order.
- Bypass (`eqEnabled = false`) makes `EqualizerProcessor` copy the input unchanged, which is a bit-exact null test (R20). Balance and the limiter still run.
- Filter coefficients are recomputed only when the config or the sample rate changes, and filter state is reset in `onFlush`.
- Tests: frequency response at every band centre, bypass null test, limiter ceiling with all bands at +12 dB, and a mono input path.
- No new permission. No dependency beyond Media3.

## Changelog

- 2026-11-18: created.
