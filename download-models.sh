#!/bin/bash
set -euo pipefail

ASSETS_DIR="app/src/main/assets"
LIBS_DIR="app/libs"

download_if_missing() {
	local output_path="$1"
	local source_url="$2"

	if [ -s "$output_path" ]; then
		echo "Already exists, skipping: $output_path"
		return
	fi

	mkdir -p "$(dirname "$output_path")"
	curl -L --progress-bar -o "$output_path" "$source_url"
}

echo "Downloading Parakeet TDT-CTC 110M models..."
mkdir -p "$ASSETS_DIR/parakeet-tdt-ctc-110m"
download_if_missing \
	"$ASSETS_DIR/parakeet-tdt-ctc-110m/model.onnx" \
	"https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000/resolve/main/model.onnx"
download_if_missing \
	"$ASSETS_DIR/parakeet-tdt-ctc-110m/tokens.txt" \
	"https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000/resolve/main/tokens.txt"

echo "Downloading Silero VAD model..."
download_if_missing \
	"$ASSETS_DIR/parakeet-tdt-ctc-110m/silero_vad.onnx" \
	"https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx"

echo "Downloading Piper TTS model (en_US-amy-low)..."
mkdir -p "$ASSETS_DIR/vits-piper-en_US-amy-low"
download_if_missing \
	"$ASSETS_DIR/vits-piper-en_US-amy-low/en_US-amy-low.onnx" \
	"https://huggingface.co/csukuangfj/vits-piper-en_US-amy-low/resolve/main/en_US-amy-low.onnx"
download_if_missing \
	"$ASSETS_DIR/vits-piper-en_US-amy-low/en_US-amy-low.onnx.json" \
	"https://huggingface.co/csukuangfj/vits-piper-en_US-amy-low/resolve/main/en_US-amy-low.onnx.json"
download_if_missing \
	"$ASSETS_DIR/vits-piper-en_US-amy-low/tokens.txt" \
	"https://huggingface.co/csukuangfj/vits-piper-en_US-amy-low/resolve/main/tokens.txt"
download_if_missing \
	"$ASSETS_DIR/vits-piper-en_US-amy-low/MODEL_CARD" \
	"https://huggingface.co/csukuangfj/vits-piper-en_US-amy-low/resolve/main/MODEL_CARD"

echo "Downloading Piper espeak-ng-data directory..."
python - "$ASSETS_DIR/vits-piper-en_US-amy-low" <<'PY'
import json
import os
import sys
from urllib.request import Request, urlopen

target_dir = sys.argv[1]
api_url = "https://huggingface.co/api/models/csukuangfj/vits-piper-en_US-amy-low"
base_url = "https://huggingface.co/csukuangfj/vits-piper-en_US-amy-low/resolve/main/"

request = Request(api_url, headers={"User-Agent": "graham-download-script"})
with urlopen(request, timeout=60) as response:
    data = json.loads(response.read().decode("utf-8"))

siblings = data.get("siblings", [])
espeak_paths = [
    item.get("rfilename", "")
    for item in siblings
    if item.get("rfilename", "").startswith("espeak-ng-data/")
]

for relative_path in espeak_paths:
    destination = os.path.join(target_dir, relative_path)
    if os.path.exists(destination) and os.path.getsize(destination) > 0:
        continue

    os.makedirs(os.path.dirname(destination), exist_ok=True)
    source_url = base_url + relative_path
    print(f"Downloading {relative_path}")

    file_request = Request(source_url, headers={"User-Agent": "graham-download-script"})
    with urlopen(file_request, timeout=120) as response, open(destination, "wb") as output:
        output.write(response.read())
PY

echo "Downloading sherpa-onnx AAR..."
mkdir -p "$LIBS_DIR"
download_if_missing \
	"$LIBS_DIR/sherpa-onnx-1.12.32.aar" \
	"https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.12.32/sherpa-onnx-1.12.32.aar"

echo "Done."
