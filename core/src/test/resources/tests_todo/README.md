# Deferred / parked tests

These files are **not** part of the test source set (they live under
`resources/`, so they are not compiled or run). They are kept here as a backlog.

Two of the original files were migrated to JUnit 5 and moved into the active
source set (`core/src/test/java/be/tarsos/dsp/test/`):

- `EnvelopeFollowerTest` — headless-safe smoke test, **enabled**.
- `ComplexOnsetTests` — migrated but tagged `@Tag("manual")` (overlaps
  `BeatRootTest`; parameter-sensitive assertions). Excluded from CI.

## Why the rest are still parked

| File | Reason |
|------|--------|
| `AudioPlayerTest`, `PitchShifterTest`, `RateTransposerTest` | Require a real audio **output** line (`AudioPlayer` / `SourceDataLine`). |
| `PercussionOnsetTest`, `WaveformWriterTest` | Require microphone **capture** (`TargetDataLine` / `Mixer`). |
| `CrossCorrelation`, `ImpulseDetection` | Not tests — they are `main()`-method demos; belong in `examples` if anywhere. |
| `TestUtilities` | Duplicate of the active `TestUtilities`; do **not** copy it into the source set. |

## How to migrate one

1. Move the file to `core/src/test/java/be/tarsos/dsp/test/`.
2. Convert JUnit 4 → 5: `org.junit.Test` → `org.junit.jupiter.api.Test`,
   `@Ignore` → `@Disabled`, and flip assertion argument order (the message is
   the **last** argument in JUnit 5).
3. Reuse the existing active `TestUtilities` — never the parked duplicate.
4. If the test needs audio hardware, annotate it `@Tag("manual")` so it is
   excluded from CI. Run manual tests with `./gradlew test -PrunManual`.
