# Cinderhell toolchain

The MVP build is pinned to the following baseline:

| Component | Version |
| --- | --- |
| Application ID | `io.github.anthonystainer.cinderhell` (`.debug` / `.preview` build suffixes) |
| Compile SDK | 37.0 |
| Target SDK | 35 |
| SDK Build Tools | 36.0.0 |
| Minimum SDK | 26 |
| Java bytecode target | 17 |
| Verified Gradle JVM | Eclipse Temurin 17.0.19+10 |
| Gradle | 9.8.2 |
| Android Gradle Plugin | 9.5.1 |
| Kotlin / Compose compiler plugin | 2.4.21 |
| KSP | 2.3.12 |
| Compose BOM | 2026.09.00 |
| Room | 2.8.5 |
| Navigation Compose | 2.10.2 |
| Kotlinx serialization | 1.12.0 |
| Android NDK | 27.0.12077973 |
| CMake | 3.31.6 |
| Packaged ABI | `arm64-v8a` only |

The checked-in Gradle wrapper is authoritative. The Gradle JVM archive used for
local verification is pinned by `scripts/fetch-jdk.sh` with SHA-256
`d8afc263758141a66e0e3aafc321e783f7016696f4eaea067d340a269037d331`.
`local.properties` is developer-local; CI sets `ANDROID_HOME` explicitly.

The target SDK remains 35 so adopting the newer compile SDK does not opt the app
into Android 17 runtime behavior changes.

## Windows with Scoop

Use PowerShell and Git for Windows' Bash for the checked-in shell scripts.
Install the tools, then open a new PowerShell window so Scoop's environment
changes are available:

```powershell
scoop bucket add java
scoop install git java/temurin17-jdk python nodejs-lts android-clt
npm install -g @fission-ai/openspec
```

Install the pinned Android packages (accept the SDK license when prompted):

```powershell
sdkmanager --install 'platform-tools' 'platforms;android-37.0' 'build-tools;36.0.0' 'ndk;27.0.12077973' 'cmake;3.31.6'
```

Clone with line-ending conversion disabled, including for submodules. Woof's
text assets are packaged verbatim into a checksummed PK3; CRLF conversion
changes that checksum even though the files look identical in an editor.

```powershell
git -c core.autocrlf=false -c core.eol=lf clone --recurse-submodules https://github.com/Aelvryx/cinderhell.git
cd cinderhell
git config core.autocrlf false
git config core.eol lf
git submodule foreach --recursive git config core.autocrlf false
git submodule foreach --recursive git config core.eol lf
$gitBash = "$env:USERPROFILE/scoop/apps/git/current/bin/bash.exe"
& $gitBash scripts/fetch-native-dependencies.sh
& $gitBash scripts/apply-native-patches.sh
& $gitBash scripts/verify-bootstrap.sh
python3 -m unittest discover -s scripts/tests
.\gradlew.bat testDebugUnitTest assembleDebug
openspec validate --all --strict
```

Scoop sets `JAVA_HOME` and `ANDROID_HOME`; alternatively use a developer-local
`local.properties` with `sdk.dir` pointing to the SDK using forward slashes and
an escaped drive colon (for example, `sdk.dir=C\:/Android/sdk`).
The debug APK is `app/build/outputs/apk/debug/app-debug.apk` and installs as a
separate `.debug` app. Enable USB debugging and authorize the workstation on
the device, verify `adb devices -l`, then run
`.\gradlew.bat connectedDebugAndroidTest` for device tests.

The Windows setup on 2026-11-09 used Temurin 17.0.21+9, Python 3.15.2,
Node 26.13.0, and OpenSpec 1.16.1 with the pinned Gradle/SDK/NDK/CMake versions
above. New shells pick up tools installed by Scoop; an already-running IDE
may need restarting. `openspec update` refreshes generated agent workflows
without rewriting the project's specifications.

Before changing the Gradle version on Windows, run `.\gradlew.bat --stop`.
An older daemon can keep migrated lint JARs locked after an upgrade; if that
happens, stop the daemon using its old Gradle distribution before retrying lint.

## Native audio regression test

This standalone NDK test uses OpenAL loopback to inspect actual music/SFX samples
in memory. It produces no speaker output and does not change Android audio mode.
With one authorized arm64 Android device connected, run from the repository root
in PowerShell (set `ANDROID_SERIAL` first if multiple devices are connected):

```powershell
$androidSdk = $env:ANDROID_HOME
$cmakeTool = "$androidSdk/cmake/3.31.6/bin/cmake.exe"
$adbTool = "$androidSdk/platform-tools/adb.exe"
& $cmakeTool -S native/tests -B native/tests/build -G Ninja `
  "-DCMAKE_MAKE_PROGRAM=$androidSdk/cmake/3.31.6/bin/ninja.exe" `
  "-DCMAKE_TOOLCHAIN_FILE=$androidSdk/ndk/27.0.12077973/build/cmake/android.toolchain.cmake" `
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 `
  -DANDROID_STL=c++_static -DCMAKE_BUILD_TYPE=Debug
if ($LASTEXITCODE -ne 0) { throw 'Configure failed' }
& $cmakeTool --build native/tests/build --parallel 4
if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
& $adbTool push native/tests/build/cinderhell_audio_test /data/local/tmp/cinderhell_audio_test
if ($LASTEXITCODE -ne 0) { throw 'Push failed' }
& $adbTool shell chmod 755 /data/local/tmp/cinderhell_audio_test
if ($LASTEXITCODE -ne 0) { throw 'chmod failed' }
& $adbTool shell /data/local/tmp/cinderhell_audio_test
if ($LASTEXITCODE -ne 0) { throw 'Loopback test failed' }
```
