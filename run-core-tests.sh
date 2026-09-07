#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/.core-test-out"
rm -rf "$OUT" && mkdir -p "$OUT"
kotlinc "$ROOT/app/src/main/java/com/masaryamamah/otpbridge/OtpCore.kt" "$ROOT/core-test/CoreTest.kt" -include-runtime -d "$OUT/core-tests.jar"
java -jar "$OUT/core-tests.jar"
