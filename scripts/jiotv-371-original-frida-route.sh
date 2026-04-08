#!/usr/bin/env bash
set -euo pipefail

PKG=com.jio.jioplay.tv
HOOKS="$(cd "$(dirname "$0")" && pwd)/jiotv-371-original-frida.js"

if ! command -v adb >/dev/null 2>&1; then
  echo 'adb not found'
  exit 1
fi

if ! command -v frida >/dev/null 2>&1; then
  echo 'frida not found'
  exit 1
fi

echo '== device =='
adb devices -l

echo '== installed package =='
adb shell dumpsys package "$PKG" | grep -E 'versionCode|versionName' || true

echo '== process =='
adb shell pidof "$PKG" || true

echo '== frida transport probe =='
adb forward tcp:27042 tcp:27042 >/dev/null
if frida-ps -H 127.0.0.1:27042 >/tmp/jiotv-frida-ps.txt 2>/tmp/jiotv-frida-ps.err; then
  echo 'frida-server is reachable on tcp:27042'
else
  echo 'frida-server is not reachable on this device.'
  echo 'Current blocker: this Play Store AVD is not rootable via adb root, so classic Frida attach needs a rooted AVD/device or another runtime-hook mechanism.'
  echo 'stderr:'
  cat /tmp/jiotv-frida-ps.err
  exit 2
fi

echo '== launch app if needed =='
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/tmp/jiotv-monkey.log 2>&1 || true
sleep 2

echo '== attach hooks =='
exec frida -H 127.0.0.1:27042 -n "$PKG" -l "$HOOKS"
