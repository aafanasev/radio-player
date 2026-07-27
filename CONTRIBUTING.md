# Contributing

## Project layout

- `:core` — playback engine + Compose UI building blocks. Should never gain a dependency on a
  specific station backend, analytics vendor, or app-specific feature (chat, auth, etc.). New
  extension points belong on `RadioPlayerHost` / `RadioPlayerConfig` / `NowPlayingMetadataProvider`
  / `PlayerAnalytics`, not as new hard dependencies.
- `:radioco` — reference implementation of `NowPlayingMetadataProvider` for radio.co stations.
- `:sample` — smallest possible app proving `:core` works standalone. Keep it dependency-free
  beyond `:core`.

## Adding a backend for another provider (Shoutcast, Icecast, a custom API, ...)

Add a new module implementing `net.afanasev.radioplayer.core.metadata.NowPlayingMetadataProvider`
against your backend, following `:radioco`'s structure:

- `RETRY_MAX_COUNT` / `RETRY_DELAY_MS`-style constants for any polling/retry behavior
- report failures through the injected `PlayerAnalytics` rather than logging directly, so host
  apps can route them to their own analytics backend
- unit tests using `ktor-client-mock` (see `radioco/src/test`)

## Local development

```
./gradlew build test
./gradlew :core:publishToMavenLocal :radioco:publishToMavenLocal
```

`publishToMavenLocal` lets a consuming app (e.g. otonfm) depend on `mavenLocal()` builds before a
version is tagged and available on JitPack.

## Style

- No new abstractions or config knobs without a concrete second consumer in mind — `:core` is
  meant to stay small.
- Preserve backward-compatible defaults on `RadioPlayerConfig`/`PlayerAnalytics`/
  `NowPlayingMetadataProvider` where possible; these are the public API surface.
