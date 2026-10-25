"""Check inherited crucible geometry and the raw/fired texture overrides."""
import json
from pathlib import Path

assets = Path(__file__).resolve().parents[1] / "src/main/resources/assets/exnihiloomnia"

def resolve(name):
    model = json.loads((assets / "models/block" / (name + ".json")).read_text())
    parent = model.get("parent", "")
    textures, elements = resolve(parent.split("/")[-1]) if parent.startswith("exnihiloomnia:block/") else ({}, [])
    return {**textures, **model.get("textures", {})}, model.get("elements", elements)

for name in ("crucible", "crucible_raw"):
    textures, elements = resolve(name)
    assert len(elements) == 10, (name, "missing shell geometry")
    expected = "_raw" if name.endswith("_raw") else ""
    assert textures["side"] == "exnihiloomnia:block/crucible_side" + expected
    assert textures["bottom"] == "exnihiloomnia:block/crucible_bottom" + expected
    for element in elements:
        assert element.get("faces"), (name, "invisible element")
        for face in element["faces"].values():
            texture = textures[face["texture"].removeprefix("#")]
            assert (assets / "textures" / (texture.split(":")[1] + ".png")).is_file()
    item = json.loads((assets / "items" / (name + ".json")).read_text())
    assert item["model"]["model"] == "exnihiloomnia:block/" + name
    print(name + ": visible shell and correct textures in block and item model")
