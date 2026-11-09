# Sourced by the ONNX Runtime scripts next to it: the version they build, where they keep sources, builds and probe
# results, and the inputs they share. One build makes both libraries, the Android AAR and the desktop app's Linux
# x86_64 jar, from the same sources and settings, and one release publishes them together. Their version comes from
# gradle/libs.versions.toml: the ONNX Runtime version, with a -<n> suffix for later builds of it (a published release
# is never replaced, so a rebuild for changed models needs new coordinates). ORT_COMMIT is the version's tag commit and
# is checked against the clone, so a moved tag cannot change what gets built. upgrade.sh updates both.

ORT_COMMIT=402979ac1bef89dcd55add41eaa7277c407599eb

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
root=$(git -C "$here" rev-parse --show-toplevel)
sdk=${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' "$root/local.properties")}
ndk_version=$(sed -n 's/^android-ndk = "\(.*\)"/\1/p' "$root/gradle/katari.versions.toml")
library_version=$(sed -n 's/^onnxruntime = "\([^"]*\)".*/\1/p' "$root/gradle/libs.versions.toml")
ort_version=${library_version%%-*}
work=${ONNXRUNTIME_BUILD_DIR:-$HOME/.cache/katari-onnxruntime}
python=$work/venv/bin/python
release_tag=onnxruntime-$library_version
# Settings.gradle.kts reads the staged libraries from here with -PlocalOnnxRuntime, in the layout of the release
# download URL.
staged_aar=$work/release/$release_tag/onnxruntime-android-$library_version.aar
staged_jar=$work/release/$release_tag/onnxruntime-linux-x86-64-$library_version.jar
models=$work/models
stock_probe=$work/probe/stock-$ort_version

# Clones ONNX Runtime at ORT_COMMIT, applies patches/ to it, and sets up the Python its build and tools need. A patch
# that no longer applies after an upgrade stops the build: drop it if upstream fixed what it fixes.
prepare_source() {
    if [ ! -d "$work/onnxruntime" ]; then
        git clone --depth 1 --branch "v$ort_version" https://github.com/microsoft/onnxruntime.git "$work/onnxruntime"
    elif [ "$(git -C "$work/onnxruntime" rev-parse HEAD)" != "$ORT_COMMIT" ]; then
        git -C "$work/onnxruntime" fetch --depth 1 origin tag "v$ort_version"
        git -C "$work/onnxruntime" checkout --detach "v$ort_version"
    fi
    if [ "$(git -C "$work/onnxruntime" rev-parse HEAD)" != "$ORT_COMMIT" ]; then
        echo "$work/onnxruntime is not ONNX Runtime $ort_version ($ORT_COMMIT)" >&2
        exit 1
    fi
    git -C "$work/onnxruntime" checkout -- .
    local patch
    for patch in "$here"/patches/*.patch; do
        [ -e "$patch" ] || continue
        if ! git -C "$work/onnxruntime" apply "$patch"; then
            echo "$patch does not apply to ONNX Runtime $ort_version" >&2
            exit 1
        fi
    done
    if [ ! -x "$python" ]; then
        python3 -m venv "$work/venv"
    fi
    "$work/venv/bin/pip" install --quiet --disable-pip-version-check flatbuffers==25.12.19 onnx==1.22.0
}

# Downloads Microsoft's full package of this version from Maven Central into $work/stock, checked against its
# published SHA-1, and prints its path.
stock_package() {
    local artifact=$1 extension=$2
    local file=$work/stock/$artifact-$ort_version.$extension
    local url=https://repo1.maven.org/maven2/com/microsoft/onnxruntime/$artifact/$ort_version/${file##*/}
    if [ ! -f "$file" ]; then
        mkdir -p "$work/stock"
        curl -sSfL -o "$file.part" "$url"
        if [ "$(sha1sum < "$file.part" | cut -d' ' -f1)" != "$(curl -sSfL "$url.sha1")" ]; then
            echo "$url does not match its published SHA-1" >&2
            exit 1
        fi
        mv "$file.part" "$file"
    fi
    echo "$file"
}

# Runs every model of the provider with the libraries of an AAR on each Android ABI it builds and of a jar on this
# machine's x86_64 CPU, writing <output>/<platform>.log and the optimized graphs to <output>/<platform>/.
probe_libraries() {
    local aar=$1 jar=$2 output=$3
    "$here/probe/probe-abis.sh" "$aar" "$models" "$output"
    "$here/probe/probe-linux.sh" "$jar" "$work/onnxruntime/include/onnxruntime/core/session" "$models" "$output"
}

# Downloads the provider's models and runs them with Microsoft's full packages on every platform, unless these exact
# models already were.
probe_stock() {
    python3 -I "$here/probe/fetch-models.py" "$models"
    local inputs
    inputs=$(cd "$models" && sha256sum -- *.onnx)
    if [ "$(cat "$stock_probe/models.sha256" 2>/dev/null)" != "$inputs" ]; then
        probe_libraries "$(stock_package onnxruntime-android aar)" "$(stock_package onnxruntime jar)" "$stock_probe"
        echo "$inputs" > "$stock_probe/models.sha256"
    fi
}
