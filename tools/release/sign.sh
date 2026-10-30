#!/usr/bin/env bash
# Signs a release built by CI (the `ehligible-v1.0-unsigned` artifact of the Android workflow) with the upload
# key, for when the PC can't run R8 itself. The key and its passwords never leave this computer: they're read
# from ~/.ehligible/keystore.properties into environment variables and never printed.
#
#   gh run download <run-id> --repo ehligible/ehligible -n ehligible-v1.0-unsigned -D <dir>
#   tools/release/sign.sh <dir> <output dir> <version>
#
# Writes ehligible-<version>.aab (upload this to Google Play), ehligible-<version>.apk (to install directly)
# and SHA256SUMS.txt. Needs ANDROID_HOME (build-tools 36.0.0) and JAVA_HOME, or the defaults below.
set -euo pipefail
if [ $# -ne 3 ]; then
  echo "usage: $0 <unsigned artifact dir> <output dir> <version>" >&2
  exit 2
fi
in="$1"; out="$2"; version="$3"
props="$HOME/.ehligible/keystore.properties"
ks="$HOME/.ehligible/upload-keystore.jks"
build_tools="${ANDROID_HOME:-$HOME/dev/android-sdk}/build-tools/36.0.0"
jdk_bin="${JAVA_HOME:-$HOME/dev/jdk17}/bin"

# Windows tools carry an extension; macOS and Linux ones don't.
tool() { for candidate in "$1/$2" "$1/$2.exe" "$1/$2.bat"; do [ -f "$candidate" ] && { echo "$candidate"; return; }; done
  echo "missing: $1/$2" >&2; exit 1; }
zipalign="$(tool "$build_tools" zipalign)"
apksigner="$(tool "$build_tools" apksigner)"
jarsigner="$(tool "$jdk_bin" jarsigner)"

value() { grep -E "^$1=" "$props" | head -1 | cut -d= -f2- | tr -d '\r'; }
export KS_PASS KEY_PASS
KS_PASS="$(value storePassword)"
KEY_PASS="$(value keyPassword)"
alias="$(value keyAlias)"

apk_in="$(find "$in" -name '*.apk' | head -1)"
aab_in="$(find "$in" -name '*.aab' | head -1)"
mkdir -p "$out"
apk_out="$out/ehligible-$version.apk"
aab_out="$out/ehligible-$version.aab"

# APK: align first, then sign (APK signature schemes v2+ cover the aligned file).
"$zipalign" -f -p 4 "$apk_in" "$out/aligned.apk"
"$apksigner" sign --ks "$ks" --ks-key-alias "$alias" --ks-pass env:KS_PASS --key-pass env:KEY_PASS \
  --out "$apk_out" "$out/aligned.apk"
rm -f "$out/aligned.apk" "$apk_out.idsig"
"$apksigner" verify --print-certs "$apk_out" | grep -E "certificate SHA-256"

# Bundle: Google Play expects JAR signing with the upload key.
"$jarsigner" -keystore "$ks" -storepass:env KS_PASS -keypass:env KEY_PASS \
  -sigalg SHA256withRSA -digestalg SHA-256 -signedjar "$aab_out" "$aab_in" "$alias" > /dev/null
"$jarsigner" -verify "$aab_out" | grep -q "jar verified" && echo "bundle signed and verified"

(cd "$out" && sha256sum "ehligible-$version.aab" "ehligible-$version.apk" > SHA256SUMS.txt && cat SHA256SUMS.txt)
