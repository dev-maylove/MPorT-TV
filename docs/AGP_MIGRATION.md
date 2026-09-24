# AGP 9 migration fixes

## Error (CI log)

```
The option 'android.defaults.buildfeatures.buildconfig' is deprecated.
It was removed in version 9.0 of the Android Gradle plugin.
```

**Fix:** Remove from `gradle.properties`. Enable BuildConfig only in module DSL:

```kotlin
android {
    buildFeatures {
        buildConfig = true
    }
}
```

## Also fixed earlier

- KSP → `2.3.11` (not `2.1.20-2.0.32`)
- AGP 9.4.1 + Gradle 9.6.1 + Kotlin 2.4.20
