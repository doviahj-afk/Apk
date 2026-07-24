# SYS.DOWNLOADER — Android APK (built via GitHub Actions)

A native Android app wrapping `yt-dlp` (via the `youtubedl-android` library,
which bundles yt-dlp + a Python runtime through Chaquopy — no Termux needed
on the phone that runs the final APK). Same terminal aesthetic as your other
SYS.* projects: black background, green monospace text, live progress log.

## Build the APK on GitHub (no Android SDK needed on Kali)

### 1. Create the repo
On github.com, click **New repository**. Name it something like
`sys-downloader`. Public or private both work. Don't add a README/gitignore
(this project already has them) — just create it empty.

### 2. Push this project from Kali

```bash
unzip SysDownloaderApp.zip
cd SysDownloaderApp

git init
git add .
git commit -m "Initial commit: SYS.DOWNLOADER Android app"
git branch -M main
git remote add origin https://github.com/<your-username>/sys-downloader.git
git push -u origin main
```

If you haven't set up git credentials on this machine yet:

```bash
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

For the push itself you'll need a GitHub personal access token (GitHub
retired plain password auth) — generate one at
github.com → Settings → Developer settings → Personal access tokens, then
use it as the password when git prompts for one. Or set up an SSH key and
use the `git@github.com:...` remote form instead.

### 3. Watch it build

Go to your repo on github.com → the **Actions** tab. You'll see the
"Build APK" workflow running automatically (triggered by the push). It
takes a few minutes the first time.

### 4. Download the APK

When the workflow run finishes (green check), click into it → scroll to
**Artifacts** → download `sysdownloader-debug-apk`. It's a zip containing
`app-debug.apk`. Transfer that to your phone (email, cloud drive, USB,
whatever's easiest) and install it — you'll need to allow "install from
unknown sources" for whichever app you use to open it.

### 5. Re-building after changes

Any future `git push` to `main` re-triggers the workflow automatically. You
can also trigger it manually from the Actions tab (`workflow_dispatch` is
enabled) without needing a new commit.

## Project layout

```
SysDownloaderApp/
├── app/
│   ├── build.gradle              # app module deps (youtubedl-android, ffmpeg)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/sysdownloader/app/MainActivity.kt
│       └── res/layout/activity_main.xml, res/values/*
├── build.gradle                  # root
├── settings.gradle
├── gradle.properties
├── .gitignore
└── .github/workflows/build.yml   # builds the APK on every push
```

## Web browsing (new)

There's now a `[ BROWSE WEB ]` button on the main screen that opens a small
built-in browser (`BrowserActivity` — plain `WebView` with an address bar,
back/forward buttons, and a `[ USE THIS PAGE ]` button). Navigate to
wherever the video/page lives, tap **USE THIS PAGE**, and the current URL
is copied straight into the downloader's URL field — no switching apps or
copy-pasting.

It's intentionally simple: it doesn't scrape pages for embedded video
links, bypass logins/paywalls/DRM, or auto-download anything in the
background. It just saves you a step for finding the URL. Whether yt-dlp
can actually pull a given page down still depends on yt-dlp's site support,
and whether you're *allowed* to depends on that site's terms of service and
copyright law — that part's on you. Only download things you own or have
the rights/permission to save.

## Notes

- The workflow installs Gradle directly on the runner (`gradle/actions/setup-gradle`)
  rather than relying on a checked-in Gradle wrapper, and GitHub's
  `ubuntu-latest` runners come with the Android SDK preinstalled — so there's
  nothing extra to configure.
- First launch on the phone will take a few seconds longer while
  `youtubedl-android` unpacks its bundled Python + yt-dlp runtime.
- Downloads save to the app's private external storage
  (`Android/data/com.sysdownloader.app/files/downloads`) — no storage
  permission dialogs needed. Use a file manager to move files elsewhere.
- To update yt-dlp itself later (sites break yt-dlp often), bump the
  `youtubedl-android` library version in `app/build.gradle`, or call
  `YoutubeDL.getInstance().updateYoutubeDL(context)` at runtime.
- App icon is a placeholder system icon — swap `android:icon` in the
  manifest for your own if you want a custom one.
- The APK from this workflow is a **debug build**, unsigned for release —
  fine for installing on your own device. If you ever want to publish to
  the Play Store, that needs a signed release build with your own keystore.
