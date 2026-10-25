"""Validate model inheritance, visible shells, texture aliases and animations.
Usage: python3 tools/verify_resources.py /path/to/minecraft-client.jar
"""
import json
import sys
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1] / 'src/main/resources'
vanilla = zipfile.ZipFile(sys.argv[1])
vanilla_names = set(vanilla.namelist())
def read(path):
    f = root/path
    return json.loads(f.read_text()) if f.exists() else json.loads(vanilla.read(path))
def normalize(ref): return ref if ':' in ref else 'minecraft:'+ref
def resolve(ref, seen=()):
    ref = normalize(ref)
    assert ref not in seen, ('model cycle', ref)
    ns, path = ref.split(':',1)
    if path.startswith('builtin/'): return {}, []
    model = read(f'assets/{ns}/models/{path}.json')
    textures, elements = resolve(model['parent'],seen+(ref,)) if 'parent' in model else ({},[])
    return {**textures,**model.get('textures',{})},model.get('elements',elements)
def texture(ref,textures):
    seen=set()
    while ref.startswith('#'):
        assert ref not in seen, ('texture cycle',ref)
        seen.add(ref); ref=textures[ref[1:]]
    ns,path=normalize(ref).split(':',1)
    assert (root/f'assets/{ns}/textures/{path}.png').exists() or f'assets/{ns}/textures/{path}.png' in vanilla_names, ref
roots=set(); blockroots=set()
def scan(node,isblock=False):
    if isinstance(node,dict):
        if isinstance(node.get('model'),str):
            roots.add(node['model'])
            if isblock:blockroots.add(node['model'])
        for value in node.values():scan(value,isblock)
    elif isinstance(node,list):
        for value in node:scan(value,isblock)
for f in (root/'assets/exnihiloomnia/blockstates').glob('*.json'):scan(json.loads(f.read_text()),True)
for f in (root/'assets/exnihiloomnia/items').glob('*.json'):scan(json.loads(f.read_text()))
for ref in roots:
    textures,elements=resolve(ref)
    if ref in blockroots:texture(textures['particle'],textures)
    for value in textures.values():texture(value,textures)
    for element in elements:
        for face in element.get('faces',{}).values():texture(face['texture'],textures)
    if ref in ['exnihiloomnia:block/crucible','exnihiloomnia:block/crucible_raw']:
        assert len(elements)==10, (ref,'invisible crucible shell')
# Custom fluid rendering uses these same two original animated sprites.
for name in ['witchwater_still','witchwater_flowing']:
    path=root/'assets/exnihiloomnia/textures/block'/(name+'.png')
    assert path.is_file()
    assert 'animation' in json.loads(Path(str(path)+'.mcmeta').read_text())
print(f'PASS: {len(roots)} model graphs, texture files, crucible shells and Witchwater animations')
