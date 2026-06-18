# AGENTS.md

Guidance for coding agents working in the TarsosDSP repository. For end-user docs see
`README.md`.

## Overview

TarsosDSP is a real-time audio processing library for Java. The DSP algorithms are
**pure Java with no external dependencies** (no `javax.sound`, no native libs in the
core): pitch detection, onset/beat tracking, IIR filters, audio effects, FFT, MFCC,
Haar wavelets, resampling/time-stretching, and synthesis.

- **Group ID:** `be.tarsos.dsp`
- **Base package:** `be.tarsos.dsp`
- **Version:** 2.5
- **License:** GPL v3

## Module layout

Multi-module **Gradle 7.2** build. `settings.gradle` includes three modules:

| Module     | Path         | Java | Depends on        | Purpose                                                        |
|------------|--------------|------|-------------------|----------------------------------------------------------------|
| `core`     | `core/`      | 11   | (none)            | Platform-independent DSP algorithms. Keep dependency-free.     |
| `jvm`      | `jvm/`       | 11   | `core`            | `javax.sound.sampled` audio I/O + Swing visualization UI.      |
| `examples` | `examples/`  | 17   | `core`, `jvm`, Reflections | Runnable CLI + GUI demos. Builds a Shadow fat JAR.    |

## Source tree

Core DSP — `core/src/main/java/be/tarsos/dsp/`:

- Root pipeline: `AudioDispatcher`, `AudioProcessor` (the interface every processor
  implements), `AudioEvent` (the buffer passed through the chain), `AudioGenerator`.
- `pitch/` — Yin, FastYin, McLeodPitchMethod, AMDF, DynamicWavelet, Goertzel/DTMF.
- `onsets/` — percussion, complex-domain, and spectral-flux onset detectors.
- `beatroot/` — BeatRoot tempo induction and beat-tracking agents.
- `io/` — audio stream/format abstraction; `PipeDecoder` (FFmpeg), `TarsosDSPAudioFormat`.
- `filters/` — IIR filters (BandPass, HighPass, LowPass).
- `effects/` — DelayEffect, FlangerEffect.
- `resample/` — Resampler + WSOLA rate transposers (pitch-preserving time stretch).
- `synthesis/` — sine/noise generators, LFO, pitch resynthesis.
- `wavelet/` — Haar wavelet transform, coder/decoder, file I/O (incl. `lift/`).
- `writer/` — WAV file output (`WriterProcessor`, `WaveHeader`).
- `util/` — pitch conversion, complex math, peak picking, `FFMPEGDownloader`.
- `util/fft/` — `FFT` interface, `FloatFFT` (JTransforms), window functions.

JVM module — `jvm/src/main/java/be/tarsos/dsp/`:

- `io/jvm/` — `AudioDispatcherFactory`, `JVMAudioInputStream`, `AudioPlayer`, `WaveformWriter`.
- `ui/` (and `ui/layers/`) — Swing-based visualization layers.

Examples — `examples/src/main/java/be/tarsos/dsp/example/`:

- Main class / runner: `be.tarsos.dsp.example.TarsosDSPExampleRunner`.
- `example/cli/` — command-line demos (incl. `feature_extractor/`).
- `example/gui/` — Swing demos (pitch detector, oscilloscope, DTMF, effects, …).
- `example/unverified/` — experimental demos.
- `example/util/` — shared example helpers.

## Build & run

```bash
./gradlew build          # build & test all modules
./gradlew shadowJar       # build the runnable examples fat JAR
```

```bash
java -jar examples/build/libs/examples-all.jar            # launch GUI runner
java -jar examples/build/libs/examples-all.jar list       # list CLI examples
java -jar examples/build/libs/examples-all.jar <example> <args>   # run one example
```

## Testing

- Framework: **JUnit 5 (Jupiter)**, via `useJUnitPlatform()`.
- Tests live in `core/src/test/java/be/tarsos/dsp/test/`.
- Run with `./gradlew test` (or `./gradlew :core:test`).
- Reports are written to `core/build/reports/`.
- Legacy tests in `core/src/test/resources/tests_todo/` are **not** compiled or run.

## Build artifacts

Generated under `{module}/build/` (gitignored):

- JARs in `{module}/build/libs/` — `core-2.5.jar`, `jvm-2.5.jar`, `examples-all.jar`
  (plus `-sources` / `-javadoc` JARs for `core` and `jvm`).
- Javadoc in `{module}/build/docs/javadoc/`.

## Conventions & gotchas

- **Keep `core` pure Java.** No JVM audio APIs (`javax.sound.sampled`) or other
  dependencies in `core` — those belong in `jvm`. New algorithms go in `core`; new
  hardware I/O or Swing UI goes in `jvm`.
- A processing chain is built by adding `AudioProcessor`s to an `AudioDispatcher`;
  each receives an `AudioEvent` per buffer.
- **FFmpeg** is auto-downloaded by `FFMPEGDownloader` and used via `PipeDecoder` to
  decode non-WAV audio. CI installs FFmpeg before building.
- CI (`.github/workflows/gradle.yml`) runs `gradle build` on Ubuntu with Java 17 on
  push to `master` and on pull requests.
- Project documentation is `README.md` (Markdown).
