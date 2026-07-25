# radio-player

An Android whitelabel radio player. `:core` depends on nothing but a stream URL; `:radioco` is an
optional module that plugs in [radio.co](https://radio.co) now-playing metadata.

Status: under construction, extracted from [otonfm](https://github.com/aafanasev/otonfm).

## Modules

- `:core` — playback engine (media3/ExoPlayer) and Compose player UI. Configured via
  `RadioPlayerConfig`; enriched via the optional `NowPlayingMetadataProvider` interface.
- `:radioco` — implements `NowPlayingMetadataProvider` against the radio.co public API
  (now-playing status, artwork, next track).
- `:sample` — minimal app demonstrating `:core` on its own, with no backend beyond the stream URL.

## Status

Scaffolding in progress. See the project's task tracker for the extraction plan.
