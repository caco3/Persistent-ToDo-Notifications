#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# build.sh — Build the TodoNotifications APK without Android Studio
#
# What this script does (once, then reuses the cache):
#   1. Downloads Android command-line tools → ~/android-sdk
#   2. Accepts SDK licenses
#   3. Installs platforms;android-34, build-tools;34.0.0, platform-tools
#   4. Downloads Gradle 8.4 binary → ~/.gradle-dist/gradle-8.4
#   5. Generates ./gradlew wrapper for future manual use
#   6. Writes local.properties pointing to the SDK
#   7. Builds a debug APK
#
# Requirements: java (JDK 17+), curl, unzip
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── Colours ───────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; BOLD='\033[1m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }
step()  { echo -e "\n${BOLD}▶ $*${NC}"; }

# ── Paths ─────────────────────────────────────────────────────────────────────
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK_DIR="$HOME/android-sdk"
GRADLE_VERSION="8.4"
GRADLE_HOME="$HOME/.gradle-dist/gradle-${GRADLE_VERSION}"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"
# Android cmdline-tools build number — update if the download URL changes
CMDLINE_TOOLS_BUILD="11076708"

# ── Prereq checks ─────────────────────────────────────────────────────────────
step "Checking prerequisites"
command -v java  &>/dev/null || error "java not found. Install JDK 17+: sudo apt-get install openjdk-17-jdk"
command -v curl  &>/dev/null || error "curl not found: sudo apt-get install curl"
command -v unzip &>/dev/null || error "unzip not found: sudo apt-get install unzip"

# Detect JAVA_HOME if not set
if [ -z "${JAVA_HOME:-}" ]; then
    JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(which java)")")")"
    export JAVA_HOME
fi
info "Java:      $(java -version 2>&1 | head -1)"
info "JAVA_HOME: $JAVA_HOME"

# ── Android command-line tools ────────────────────────────────────────────────
SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
if [ ! -f "$SDKMANAGER" ]; then
    step "Downloading Android command-line tools"
    TOOLS_ZIP="/tmp/cmdline-tools-linux.zip"
    curl -fsSL \
        "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip" \
        -o "$TOOLS_ZIP"
    mkdir -p "$SDK_DIR/cmdline-tools"
    unzip -q "$TOOLS_ZIP" -d "$SDK_DIR/_tools_tmp"
    mv "$SDK_DIR/_tools_tmp/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm -rf "$SDK_DIR/_tools_tmp" "$TOOLS_ZIP"
    info "Saved to $SDK_DIR/cmdline-tools/latest"
else
    info "Android cmdline-tools already present, skipping download"
fi

export ANDROID_SDK_ROOT="$SDK_DIR"

# ── Accept SDK licences (hash-based, no interactive prompt) ───────────────────
step "Accepting Android SDK licences"
mkdir -p "$SDK_DIR/licenses"
# android-sdk-license
printf '\n24333f8a63b6825ea9c5514f83c2829b004d1fee' \
    > "$SDK_DIR/licenses/android-sdk-license"
# android-sdk-preview-license
printf '\n84831b9409646a918e30573bab4c9c91346d8abd' \
    > "$SDK_DIR/licenses/android-sdk-preview-license"
info "Licences written"

# ── Install SDK packages ───────────────────────────────────────────────────────
step "Installing SDK packages"
PACKAGES_MARKER="$SDK_DIR/.packages-installed-34"
if [ ! -f "$PACKAGES_MARKER" ]; then
    "$SDKMANAGER" \
        "platforms;android-34" \
        "build-tools;34.0.0" \
        "platform-tools"
    touch "$PACKAGES_MARKER"
    info "SDK packages installed"
else
    info "SDK packages already installed, skipping"
fi

# ── local.properties ──────────────────────────────────────────────────────────
step "Writing local.properties"
echo "sdk.dir=$SDK_DIR" > "$PROJECT_DIR/local.properties"
info "sdk.dir=$SDK_DIR"

# ── Gradle binary ─────────────────────────────────────────────────────────────
if [ ! -f "$GRADLE_BIN" ]; then
    step "Downloading Gradle ${GRADLE_VERSION}"
    GRADLE_ZIP="/tmp/gradle-${GRADLE_VERSION}-bin.zip"
    curl -fsSL \
        "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
        -o "$GRADLE_ZIP"
    mkdir -p "$HOME/.gradle-dist"
    unzip -q "$GRADLE_ZIP" -d "$HOME/.gradle-dist/"
    rm "$GRADLE_ZIP"
    info "Saved to $GRADLE_HOME"
else
    info "Gradle ${GRADLE_VERSION} already present, skipping download"
fi

# ── Generate ./gradlew wrapper (for future manual use) ────────────────────────
if [ ! -f "$PROJECT_DIR/gradlew" ]; then
    step "Generating Gradle wrapper"
    "$GRADLE_BIN" -p "$PROJECT_DIR" wrapper --gradle-version "$GRADLE_VERSION" --quiet
    chmod +x "$PROJECT_DIR/gradlew"
    info "gradlew created — you can use './gradlew assembleDebug' for future builds"
fi

# ── Build APK ─────────────────────────────────────────────────────────────────
step "Building debug APK"
"$GRADLE_BIN" -p "$PROJECT_DIR" assembleDebug

# ── Done ──────────────────────────────────────────────────────────────────────
APK_PATH=$(find "$PROJECT_DIR/app/build/outputs/apk/debug" -name "*.apk" 2>/dev/null | head -1 || true)
echo ""
echo -e "${GREEN}${BOLD}✅  Build successful!${NC}"
if [ -n "$APK_PATH" ]; then
    echo -e "    APK → ${BOLD}${APK_PATH}${NC}"
    echo ""
    echo "To install on a connected device / emulator:"
    echo "    $SDK_DIR/platform-tools/adb install \"$APK_PATH\""
fi
