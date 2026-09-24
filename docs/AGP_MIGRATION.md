# AGP / Gradle migration notes

## Error from CI log (fixed)

```
Plugin [id: 'com.google.devtools.ksp', version: '2.1.20-2.0.32'] was not found
```

**Cause:** Old KSP id tied to Kotlin 2.1.20 is no longer published the same way.  
**Fix:** Use KSP **2.3.11** (KSP2 — version independent of Kotlin string).

## Current stack

| Item | Version |
|------|---------|
| AGP | 9.4.1 |
| Gradle | 9.6.1 |
| Kotlin | 2.4.20 |
| KSP | 2.3.11 |
| compileSdk | 36 |

## Build time optimizations (`gradle.properties`)

- `org.gradle.parallel=true`
- `org.gradle.caching=true`
- `org.gradle.configuration-cache=true`
- `org.gradle.configureondemand=true`
- Kotlin incremental + in-process compiler
- JVM heap 4G

## After pull

```bash
./gradlew --stop
./gradlew assembleDebug --configuration-cache
```

Push **all** of `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, and wrapper to GitHub so Actions no longer resolves the old KSP version.
