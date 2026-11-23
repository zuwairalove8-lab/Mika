# Sideload signing key

`mika-sideload.jks` is the one key every build type (debug, perf, release) is signed with.
It is committed on purpose: GitHub Actions runners start empty, so the auto-generated
`~/.android/debug.keystore` used to be a *different* key on every run. Two APKs signed by
different keys cannot install over each other — Android aborts the update and MIUI only
says "App not installed".

Keep this file: replacing it changes the app's certificate, and every already-installed
copy then has to be uninstalled once before the new build can be installed.

- alias: `mika`
- store/key password: `mika-sideload`
- certificate SHA-256: `66BEBB8E69F9F734C839EDAA36B93775E5E712745C9DF35745B948297418872`

The fingerprint is pinned in `build-apk.yml` and `release-apk.yml`; a build signed by any
other key fails instead of being published. To rotate the key, regenerate it, update the
fingerprint in both workflows and this file, and bump `versionCode`.
