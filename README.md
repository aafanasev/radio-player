# radio-player

A whitelabel Android radio player. `:core` depends on nothing but a stream URL — no backend, no
account, no chat. `:radioco` is an optional module that plugs in [radio.co](https://radio.co)
now-playing metadata (artwork, next track) for stations hosted there.

Extracted from [otonfm](https://github.com/aafanasev/otonfm), which is now a consumer of this
library rather than owning the player code directly.

## Modules

- **`:core`** — the playback engine (media3/ExoPlayer + `MediaLibraryService`, so playback
  survives backgrounding and shows lock-screen/notification controls) and a set of Compose
  building blocks (`PortraitContent`, `LandscapeContent`, `Artwork`, `Title`, `NextTrack`,
  `PlayButton`, `Background`) plus `PlayerViewModel`. Configured via `RadioPlayerConfig`;
  enriched via the optional `NowPlayingMetadataProvider` interface, which defaults to
  ICY-stream-metadata-only (no backend required).
- **`:radioco`** — implements `NowPlayingMetadataProvider` against the radio.co public status
  API, given a station id.
- **`:sample`** — a minimal app demonstrating `:core` on its own, playing a public test stream
  with zero backend.

## Getting started

Add JitPack and the dependency:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven("https://jitpack.io")
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.aafanasev.radio-player:core:<version>")
    // optional, if your station is on radio.co
    implementation("com.github.aafanasev.radio-player:radioco:<version>")
}
```

Implement `RadioPlayerHost` on your `Application` — `:core`'s `PlaybackService` and
`PlayerViewModel` read this off `application` at runtime, since Android constructs services and
view models without a way to inject constructor arguments directly:

```kotlin
class MyApp : Application(), RadioPlayerHost {
    override val radioPlayerConfig = RadioPlayerConfig(
        streamUrl = "https://stream.example.com/listen",
        stationName = "My Station",
        defaultArtworkUri = "https://example.com/logo.jpg",
        sessionActivityClass = MainActivity::class.java,
    )

    // Omit for ICY-metadata-only playback, or supply your own backend:
    override val nowPlayingMetadataProvider = RadioCoMetadataProvider(stationId = "sXXXXXXXX")

    // Omit for no analytics, or forward to your own backend:
    override val playerAnalytics = object : PlayerAnalytics { /* ... */ }
}
```

`PlaybackService` and its permissions are declared in `:core`'s manifest and merge into your app
automatically — no manifest changes needed on your side. Then compose the player UI:

```kotlin
val viewModel: PlayerViewModel = viewModel()
val title by viewModel.title.collectAsState()
// ...
PortraitContent(
    title = title,
    nextTrackTitle = nextTrackTitle,
    buttonState = buttonState,
    artworkUri = artworkUri,
    onPlayClick = viewModel::playPause,
    logo = { /* your branding */ },
)
```

Run `:sample` to see it end to end.

## Contributing

Other backends (Shoutcast, Icecast, a custom API) are welcome as their own module implementing
`NowPlayingMetadataProvider`, following `:radioco` as a reference. See
[CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache 2.0 — see [LICENSE](LICENSE).
