# Sourced by the ONNX Runtime scripts next to it: the version they build, where they keep sources, builds and probe
# results, and the inputs they share. The AAR's version comes from gradle/libs.versions.toml: the ONNX Runtime version,
# with a -<n> suffix for later builds of it (a published AAR is never replaced, so a rebuild for changed models needs
# new coordinates). ORT_COMMIT is the version's tag commit and is checked against the clone, so a moved tag cannot
# change what gets built. upgrade.sh updates both.

ORT_COMMIT=f2c39fe2f838cf35ce7da92824f5a5e3ee6e88a7

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
root=$(git -C "$here" rev-parse --show-toplevel)
sdk=${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' "$root/local.properties")}
ndk_version=$(sed -n 's/^android-ndk = "\(.*\)"/\1/p' "$root/gradle/mihon.versions.toml")
aar_version=$(sed -n 's/^onnxruntime = "\([^"]*\)".*/\1/p' "$root/gradle/libs.versions.toml")
ort_version=${aar_version%%-*}
work=${ONNXRUNTIME_BUILD_DIR:-$HOME/.cache/katari-onnxruntime}
python=$work/venv/bin/python
release_tag=onnxruntime-$aar_version
# Settings.gradle.kts reads the staged AAR from here with -PlocalOnnxRuntime, in the layout of the release download URL.
staged_aar=$work/release/$release_tag/onnxruntime-android-$aar_version.aar
models=$work/models
stock_probe=$work/probe/stock-$ort_version

# Clones ONNX Runtime at ORT_COMMIT and sets up the Python its build and tools need.
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
    if [ ! -x "$python" ]; then
        python3 -m venv "$work/venv"
    fi
    "$work/venv/bin/pip" install --quiet flatbuffers==25.12.19 onnx==1.22.0
}

# Prints the path of Microsoft's full onnxruntime-android AAR of this version, downloaded from Maven Central.
stock_aar() {
    local aar=$work/stock/onnxruntime-android-$ort_version.aar
    local url=https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/$ort_version/${aar##*/}
    if [ ! -f "$aar" ]; then
        mkdir -p "$work/stock"
        curl -sSfL -o "$aar.part" "$url"
        if [ "$(sha1sum < "$aar.part" | cut -d' ' -f1)" != "$(curl -sSfL "$url.sha1")" ]; then
            echo "$url does not match its published SHA-1" >&2
            exit 1
        fi
        mv "$aar.part" "$aar"
    fi
    echo "$aar"
}

# Downloads the provider's models and runs them with the stock AAR on every ABI, unless these exact models already were.
probe_stock() {
    python3 -I "$here/probe/fetch-models.py" "$models"
    local inputs
    inputs=$(cd "$models" && sha256sum -- *.onnx)
    if [ "$(cat "$stock_probe/models.sha256" 2>/dev/null)" != "$inputs" ]; then
        "$here/probe/probe-abis.sh" "$(stock_aar)" "$models" "$stock_probe"
        echo "$inputs" > "$stock_probe/models.sha256"
    fi
}
