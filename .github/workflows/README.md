# GitHub Actions — MPorT TV

| Workflow | Trigger | Output |
|----------|---------|--------|
| **ci.yml** | push/PR ke `main`/`master`/`develop` + manual | Debug APK artifact |
| **pr-check.yml** | Pull request | Compile + unit test |
| **release.yml** | Tag `v*` atau manual | Release APK + GitHub Release |

## Signing (opsional)

Untuk APK release bertanda tangan, tambahkan repository secrets:

- `SIGNING_KEY_STORE_BASE64` — isi file `.jks` / `.keystore` di-encode base64  
  `base64 -w0 keystore.jks`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`
- `SIGNING_STORE_PASSWORD`

Tanpa secrets, `assembleRelease` tetap dijalankan (unsigned / default debug key tergantung konfigurasi Gradle).

## Cara release

```bash
git tag v1.0.0
git push origin v1.0.0
```

Workflow **Release** akan build APK dan membuat GitHub Release dari tag tersebut.

## Local

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```
