# Model pack tooling

Python scripts that produce the downloadable model packs the app fetches from Hugging Face Hub. Nothing here ships inside the APK.

## Setup

```bash
cd ml
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
```

## Scripts

| Script | Output |
|---|---|
| `export_lite_pack.py` | `exports/lite/` — INT8 LiteRT models for the Lite engine: garment segmentation, pose/parsing, warp, harmonization |
| `export_diffusion_pack.py` | `exports/pro/` — INT8/FP16 ONNX try-on diffusion (CatVTON-class) for the Pro engine |
| `manifest_gen.py` | `exports/manifest.json` — pack manifest (ids, versions, sha256, sizes, device gates) consumed by the app |

## Publishing a pack release

1. Run the export script(s); verify outputs with the checks each script prints.
2. `python manifest_gen.py exports/` — regenerates `manifest.json`, bumping versions for changed packs.
3. Upload the changed files + `manifest.json` to the Hugging Face packs repo (`huggingface-cli upload`).

The app polls `manifest.json`; installed packs with a lower version show "Update available".

## Licensing

Before publishing any exported model, record its upstream license in `MODEL_LICENSES.md` and confirm it permits redistribution + commercial use. Research-only checkpoints must not be published to the production packs repo.
