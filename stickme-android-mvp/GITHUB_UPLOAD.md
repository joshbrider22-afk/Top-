# GitHub Upload Path

Authenticated GitHub user seen by the connector: `joshbrider22-afk`.

## Option A — GitHub web upload
1. Create a new GitHub repository named `stickme-android-mvp`.
2. Upload the contents of this folder, not the parent ZIP folder.
3. Commit to `main` with message: `Initial StickMe Android keyboard MVP`.
4. Open the repository Actions tab and confirm `Android CI` starts.

## Option B — command line
```bash
git init
git add .
git commit -m "Initial StickMe Android keyboard MVP"
git branch -M main
git remote add origin https://github.com/joshbrider22-afk/stickme-android-mvp.git
git push -u origin main
```

## Build command
```bash
./gradlew :app:assembleDebug --stacktrace
```

If no Gradle wrapper exists yet, open the project in Android Studio first and let it generate/sync the wrapper, or run the build using the installed Gradle distribution.

## Status
- Android keyboard MVP source prepared.
- Store metadata and release assets included.
- CI workflow included.
- Physical device testing still required before public release.
