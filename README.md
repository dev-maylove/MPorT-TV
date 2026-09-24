# MPorT TV

Clean-room Android IPTV / media player + YouTube InnerTube with nsig/PO assets.

## YouTube stack

| Layer | Implementation |
|-------|----------------|
| Player API | `youtubei/v1/player` + search |
| nsig | `assets/nsigsolver/` (from ion-tv extract) via WebView + AndroidBridge |
| PO Token | `assets/potokennp2/` + att challenge helper (may soft-fail without full session) |
| Resolve | `YoutubeStreamResolver` fixes `n=` / cipher URLs before Media3 play |

## Other features

- M3U import, demo HLS, favorites, history, search, EPG
- Icon, splash, About / License / Privacy

## Build

```bash
./gradlew assembleDebug
```

Use only authorized content.
