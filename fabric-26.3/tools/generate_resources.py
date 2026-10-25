"""Regenerate the first port's models, recipes and loot using existing textures.
Run from any directory. No artwork is generated or modified.
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/exnihiloomnia'
DATA = RES / 'data/exnihiloomnia'
LEGACY = ROOT.parent / 'src/main/resources/assets/exnihiloomnia/textures'
LANG = {'itemGroup.exnihiloomnia': 'Ex Nihilo Omnia'}
BLOCKS = []

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n')

def tex(old, new):
    dest = ASSETS / 'textures' / (new + '.png')
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(LEGACY / (old + '.png'), dest)
    metadata = LEGACY / (old + '.png.mcmeta')
    if metadata.exists():
        shutil.copyfile(metadata, Path(str(dest)+'.mcmeta'))

def item_model(name, texture=None, block=False):
    write(ASSETS / 'items' / (name + '.json'), {'model': {'type': 'minecraft:model', 'model': 'exnihiloomnia:' + ('block/' if block else 'item/') + name}})
    if not block:
        write(ASSETS / 'models/item' / (name + '.json'), {'parent': 'minecraft:item/handheld' if name.startswith('hammer') else 'minecraft:item/generated', 'textures': {'layer0': texture}})
    LANG[('block.' if block else 'item.') + 'exnihiloomnia.' + name] = name.replace('_', ' ').title()

def loot_entry(item, **kwargs):
    return {'type': 'minecraft:item', 'name': item, **kwargs}

def pool(entry, conditions=None):
    p = {'rolls': 1, 'entries': [entry]}
    if conditions: p['condition'] = conditions[0] if len(conditions)==1 else {'type':'minecraft:all_of','terms':conditions}
    return p

def block(name, texture, compressed=False):
    BLOCKS.append(name)
    item_model(name, block=True)
    write(ASSETS / 'blockstates' / (name + '.json'), {'variants': {'': {'model': 'exnihiloomnia:block/' + name}}})
    model = {'parent': 'minecraft:block/cube_all', 'textures': {'all': texture}}
    if compressed:
        # A narrow inset on every face makes compressed blocks distinguishable
        # while reusing the original material's texture and vanilla stone.
        faces = {f: {'texture': '#all'} for f in ('north','south','east','west','up','down')}
        model = {'textures': {'all': texture, 'particle': texture, 'rim': 'minecraft:block/polished_deepslate'},
                 'elements': [{'from': [0,0,0], 'to': [16,16,16], 'faces': {f: {'texture':'#rim'} for f in faces}}]}
        for f in faces:
            lo, hi = {'north':([1,1,-.01],[15,15,0]), 'south':([1,1,16],[15,15,16.01]),
                      'west':([-.01,1,1],[0,15,15]), 'east':([16,1,1],[16.01,15,15]),
                      'up':([1,16,1],[15,16.01,15]), 'down':([1,-.01,1],[15,0,15])}[f]
            model['elements'].append({'from':lo,'to':hi,'faces':{f:{'texture':'#all','uv':[1,1,15,15]}}})
    write(ASSETS / 'models/block' / (name + '.json'), model)
    write(DATA / 'loot_table/blocks' / (name + '.json'), {'type':'minecraft:block','pools':[pool(loot_entry('exnihiloomnia:'+name),[{'type':'minecraft:survives_explosion'}])]})

recipes = []
def recipe(name, pattern, key, result, count=1):
    result = result if ':' in result else 'exnihiloomnia:' + result
    write(DATA / 'recipe' / (name+'.json'), {'type':'minecraft:crafting_shaped','category':'misc','pattern':pattern,'key':key,'result':{'id':result,'count':count}})
    recipes.append('exnihiloomnia:'+name)

def shapeless(name, ingredients, result, count):
    write(DATA / 'recipe' / (name+'.json'), {'type':'minecraft:crafting_shapeless','category':'misc','ingredients':ingredients,'result':{'id':result,'count':count}})
    recipes.append('exnihiloomnia:'+name)

materials = ['dirt','gravel','sand','dust','soul_sand','gravel_nether','gravel_ender']
for name in ['dust','gravel_nether','gravel_ender']:
    tex('blocks/'+name, 'block/'+name)
    block(name, 'exnihiloomnia:block/'+name)
for name in materials + ['cobblestone']:
    base = ('exnihiloomnia:' if name in ('dust','gravel_nether','gravel_ender') else 'minecraft:') + name
    block('compressed_'+name, base.replace(':',':block/'), compressed=True)
    recipe('compress_'+name,['XXX','XXX','XXX'],{'X':base},'compressed_'+name)
    shapeless('decompress_'+name,['exnihiloomnia:compressed_'+name],base,9)

# Sieve: fixed frame, visible installed mesh and a material surface while loaded.
for name, wood in [('sieve_wood','oak_planks'), ('heavy_sieve','dark_oak_planks')]:
    BLOCKS.append(name)
    item_model(name, block=True)
    elements = []
    for lo, hi in [([0,0,0],[2,11,2]),([14,0,0],[16,11,2]),([0,0,14],[2,11,16]),([14,0,14],[16,11,16]),([0,11,0],[16,14,2]),([0,11,14],[16,14,16]),([0,11,2],[2,14,14]),([14,11,2],[16,14,14])]:
        elements.append({'from':lo,'to':hi,'faces':{f:{'texture':'#wood'} for f in ['up','down','north','south','east','west']}})
    write(ASSETS / 'models/block' / (name+'.json'), {'textures':{'wood':'minecraft:block/'+wood,'particle':'minecraft:block/'+wood},'elements':elements})
    parts = [{'apply':{'model':'exnihiloomnia:block/'+name}}]
    for mesh in range(1,5):
        parts.append({'when':{'mesh':str(mesh)},'apply':{'model':'exnihiloomnia:block/sieve_mesh_'+str(mesh)}})
    for i,mat in enumerate(materials,1):
        for progress in range(8):
            parts.append({'when':{'content':str(i),'progress':str(progress)},'apply':{'model':f'exnihiloomnia:block/sieve_content_{mat}_{progress}'}})
    write(ASSETS / 'blockstates' / (name+'.json'), {'multipart':parts})
    pools = [pool(loot_entry('exnihiloomnia:'+name))]
    # Native lattice models reuse vanilla material textures; actual gaps do not
# depend on a transparent render layer. The inventory uses the same lattice.
mesh_textures = ['white_wool', 'gray_concrete', 'iron_block', 'diamond_block']
for i,mesh in enumerate(['string','flint','iron','diamond'],1):
    name = 'mesh_'+mesh
    item_model(name)
    write(ASSETS/'models/item'/(name+'.json'), {
        'parent': f'exnihiloomnia:block/sieve_mesh_{i}',
        'display': {
            'gui': {'rotation':[90,0,0], 'translation':[0,0,0], 'scale':[1,1,1]},
            'ground': {'translation':[0,2,0], 'scale':[.5,.5,.5]},
            'fixed': {'rotation':[90,0,0], 'scale':[1,1,1]},
            'firstperson_righthand': {'rotation':[0,-90,25], 'scale':[.8,.8,.8]},
            'thirdperson_righthand': {'rotation':[75,45,0], 'translation':[0,2.5,0], 'scale':[.5,.5,.5]}
        }
    })
    elements = []
    faces = {f:{'texture':'#mesh'} for f in ['up','down','north','south','east','west']}
    for offset in [2,4.25,6.5,8.75,11,13.25]:
        elements.append({'from':[offset,11,2], 'to':[offset+.75,11.15,14], 'faces':faces})
        for gap in range(5):
            start = 2.75 + gap * 2.25
            elements.append({'from':[start,11,offset], 'to':[start+1.5,11.15,offset+.75], 'faces':faces})
    write(ASSETS/'models/block'/(f'sieve_mesh_{i}.json'), {
        'ambientocclusion':False,
        'textures': {'mesh':'minecraft:block/'+mesh_textures[i-1], 'particle':'minecraft:block/'+mesh_textures[i-1]},
        'elements':elements
    })
    (ASSETS/'textures/item'/(name+'.png')).unlink(missing_ok=True)
    (ASSETS/'textures/block'/(f'sieve_mesh_{i}.png')).unlink(missing_ok=True)
for mat in materials:
    texture = ('exnihiloomnia:' if mat in ('dust','gravel_nether','gravel_ender') else 'minecraft:') + 'block/' + mat
    (ASSETS/'models/block'/('sieve_content_'+mat+'.json')).unlink(missing_ok=True)
    for progress in range(8):
        height = 11.2 + 2.6 * (8 - progress) / 8
        write(ASSETS/'models/block'/(f'sieve_content_{mat}_{progress}.json'), {'textures':{'content':texture,'particle':texture},'elements':[{'from':[2,11.2,2],'to':[14,height,14],'faces':{face:{'texture':'#content'} for face in ['up','down','north','south','east','west']}}]})

recipe('sieve_wood',['P P','PPP','S S'],{'P':'#minecraft:planks','S':'minecraft:stick'},'sieve_wood')
recipe('heavy_sieve',['L L','LSL','L L'],{'L':'#minecraft:logs','S':'exnihiloomnia:sieve_wood'},'heavy_sieve')
recipe('mesh_string',['SSS','SSS','SSS'],{'S':'minecraft:string'},'mesh_string')
for prev,nxt,ingredient in [('string','flint','flint'),('flint','iron','iron_ingot'),('iron','diamond','diamond')]:
    recipe('mesh_'+nxt,[' X ','XMX',' X '],{'X':'minecraft:'+ingredient,'M':'exnihiloomnia:mesh_'+prev},'mesh_'+nxt)
for mat,ingredient in [('wood','#minecraft:planks'),('stone','minecraft:cobblestone'),('iron','minecraft:iron_ingot'),('gold','minecraft:gold_ingot'),('diamond','minecraft:diamond')]:
    tex('items/hammer_'+mat,'item/hammer_'+mat)
    item_model('hammer_'+mat,'exnihiloomnia:item/hammer_'+mat)
    recipe('hammer_'+mat,['X X',' S ',' S '],{'X':ingredient,'S':'minecraft:stick'},'hammer_'+mat)
# Keep the vanilla smithing template requirement for a netherite tool.
tex('items/hammer_diamond','item/hammer_netherite')
item_model('hammer_netherite','exnihiloomnia:item/hammer_netherite')
write(DATA/'recipe/hammer_netherite.json',{'type':'minecraft:smithing_transform','template':'minecraft:netherite_upgrade_smithing_template','base':'exnihiloomnia:hammer_diamond','addition':'minecraft:netherite_ingot','result':{'id':'exnihiloomnia:hammer_netherite'}})
recipes.append('exnihiloomnia:hammer_netherite')
tex('items/stone','item/stone_pebble')
item_model('stone_pebble','exnihiloomnia:item/stone_pebble')
recipe('cobblestone_from_pebbles',['PP','PP'],{'P':'exnihiloomnia:stone_pebble'},'minecraft:cobblestone')
for ore in ('iron','copper','gold'):
    item_model(ore+'_piece','minecraft:item/'+ore+'_nugget')
    LANG['item.exnihiloomnia.'+ore+'_piece'] = ore.title()+' Piece (Legacy)'
    (DATA/'recipe'/('raw_'+ore+'_from_pieces.json')).unlink(missing_ok=True)
    recipe('nuggets_from_'+ore+'_pieces',['PP','PP'],{'P':'exnihiloomnia:'+ore+'_piece'},'minecraft:'+ore+'_nugget',9)
# Crooks and the original animated silkworm icons.
for material, ingredient in [('wood','minecraft:stick'), ('bone','minecraft:bone')]:
    name = 'crook_'+material
    tex('items/'+name, 'item/'+name)
    item_model(name, 'exnihiloomnia:item/'+name)
    write(ASSETS/'models/item'/(name+'.json'), {'parent':'minecraft:item/handheld','textures':{'layer0':'exnihiloomnia:item/'+name}})
    LANG['item.exnihiloomnia.'+name] = 'Wooden Crook' if material=='wood' else 'Bone Crook'
    recipe(name, ['XX',' X',' X'], {'X':ingredient}, name)
for name, old, label in [('silkworm','silkworm','Silkworm'),('cooked_silkworm','silkworm_cooked','Cooked Silkworm')]:
    tex('items/'+old, 'item/'+name)
    item_model(name, 'exnihiloomnia:item/'+name)
    LANG['item.exnihiloomnia.'+name] = label
write(DATA/'recipe/cooked_silkworm.json', {'type':'minecraft:smelting','category':'food','ingredient':'exnihiloomnia:silkworm','result':{'id':'exnihiloomnia:cooked_silkworm'},'cookingtime':200,'experience':0.0})
recipes.append('exnihiloomnia:cooked_silkworm')
leaf_types = ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','pale_oak','azalea','flowering_azalea','red_poplar','orange_poplar','yellow_poplar']
leaf_ids = []
for species in leaf_types:
    name = 'infested_leaves' if species=='oak' else 'infested_'+species+'_leaves'
    leaf_ids.append('exnihiloomnia:'+name)
    item_model(name, block=True)
    LANG['block.exnihiloomnia.'+name] = 'Infested '+species.replace('_',' ').title()+' Leaves'
    write(ASSETS/'items'/(name+'.json'), {'model':{'type':'minecraft:model','model':'exnihiloomnia:block/'+name,'tints':[{'type':'minecraft:constant','value':-1}]}})
    write(ASSETS/'blockstates'/(name+'.json'), {'variants':{'':{'model':'exnihiloomnia:block/'+name}}})
    write(ASSETS/'models/block'/(name+'.json'), {'parent':'minecraft:block/leaves','textures':{'all':'minecraft:block/'+species+'_leaves','particle':'minecraft:block/'+species+'_leaves'}})
    preserve = {'type':'minecraft:any_of','terms':['minecraft:tool/can_shear','minecraft:tool/can_silk_touch']}
    harvest = {'type':'minecraft:inverted','term':preserve}
    pools = [pool(loot_entry('exnihiloomnia:'+name), [preserve])]
    # Match the supplied Omnia source: two progress-dependent string rolls and
    # an independent 50% string roll. Crooks find worms on ordinary leaves.
    for stage in range(5):
        for chance in [.15 * stage/4, .6 * stage/4]:
            pools.append(pool(loot_entry('minecraft:string'), [harvest,
                {'type':'minecraft:match_block','blocks':'exnihiloomnia:'+name,'state':{'infestation':str(stage)}},
                {'type':'minecraft:random_chance','chance':chance}]))
    pools.append(pool(loot_entry('minecraft:string'), [harvest,{'type':'minecraft:random_chance','chance':.5}]))
    write(DATA/'loot_table/blocks'/(name+'.json'), {'type':'minecraft:block','pools':pools})
write(RES/'data/minecraft/tags/block/leaves.json', {'replace':False,'values':leaf_ids})
write(RES/'data/minecraft/tags/block/mineable/hoe.json', {'replace':False,'values':leaf_ids})
for tag in ['mining','mining_loot','durability']:
    write(RES/'data/minecraft/tags/item/enchantable'/(tag+'.json'), {'replace':False,'values':['exnihiloomnia:crook_wood','exnihiloomnia:crook_bone']})

# Processing barrels reuse the original Omnia shell geometry, with modern
# vanilla material paths. No bitmap assets are generated.
barrel_shell = json.loads((LEGACY.parent/'models/block/barrel_wood.json').read_text())
barrel_shell['parent'] = 'minecraft:block/block'
barrel_shell['textures'] = {'particle':'#barrel'}
write(ASSETS/'models/block/barrel_shell.json', barrel_shell)
wood_types = ['oak','spruce','birch','jungle','acacia','dark_oak','pale_oak','crimson','warped','mangrove','bamboo','cherry','poplar']
colors = ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
barrels = [('barrel_wood' if wood=='oak' else 'barrel_'+wood,wood+'_planks',wood+'_planks',wood+'_slab',wood.replace('_',' ').title()+' Barrel') for wood in wood_types]
barrels += [('barrel_stone','stone','stone','stone_slab','Stone Barrel'),('barrel_glass','glass','glass','glass_pane','Glass Barrel')]
barrels += [('barrel_glass_'+color,color+'_stained_glass',color+'_stained_glass',color+'_stained_glass_pane',color.replace('_',' ').title()+' Glass Barrel') for color in colors]
content_textures = {'compost':'exnihiloomnia:block/compost', 'water':'minecraft:block/water_still','lava':'minecraft:block/lava_still',
    'dirt':'minecraft:block/dirt','clay':'minecraft:block/clay','netherrack':'minecraft:block/netherrack','end_stone':'minecraft:block/end_stone',
    'obsidian':'minecraft:block/obsidian','stone':'minecraft:block/stone','cobblestone':'minecraft:block/cobblestone',
    'grass':'minecraft:block/grass_block_top','mycelium':'minecraft:block/mycelium_top','ice':'minecraft:block/ice','slime':'minecraft:block/slime_block'}
content_textures.update({'witchwater':'exnihiloomnia:block/witchwater_still',
    'witchwater_growing':'exnihiloomnia:block/witchwater_still','summon_blaze':'minecraft:block/lava_still',
    'summon_enderman':'exnihiloomnia:block/witchwater_still','soul_sand':'minecraft:block/soul_sand','chorus':'minecraft:block/chorus_plant'})
tex('blocks/compost','block/compost')
for content,texture in content_textures.items():
    for fill in range(1,9):
        faces = {face:{'texture':'#content'} for face in ['up','down','north','south','east','west']}
        if content in ('compost','water','grass'):
            for face in faces.values(): face['tintindex']=0
        write(ASSETS/'models/block'/(f'barrel_contents_{content}_{fill}.json'), {
            'textures':{'content':texture,'particle':texture},
            'elements':[{'from':[2,1.01,2],'to':[14,1.01+13.8*fill/8,14],'faces':faces}]
        })
for name,texture,material,slab,title in barrels:
    item_model(name,block=True)
    LANG['block.exnihiloomnia.'+name]=title
    write(ASSETS/'models/block'/(name+'.json'),{'parent':'exnihiloomnia:block/barrel_shell','textures':{'barrel':'minecraft:block/'+texture}})
    parts=[{'apply':{'model':'exnihiloomnia:block/'+name}}]
    for content in content_textures:
        for fill in range(1,9):
            parts.append({'when':{'contents':content,'fill':str(fill)},'apply':{'model':f'exnihiloomnia:block/barrel_contents_{content}_{fill}'}})
    write(ASSETS/'blockstates'/(name+'.json'),{'multipart':parts})
    write(DATA/'loot_table/blocks'/(name+'.json'),{'type':'minecraft:block','pools':[pool(loot_entry('exnihiloomnia:'+name),[{'type':'minecraft:survives_explosion'}])]})
    recipe(name,['X X','X X','XYX'],{'X':'minecraft:'+material,'Y':'minecraft:'+slab},name)
wood_barrels=['exnihiloomnia:'+entry[0] for entry in barrels[:len(wood_types)]]
other_barrels=['exnihiloomnia:'+entry[0] for entry in barrels[len(wood_types):]]
write(RES/'data/minecraft/tags/block/mineable/axe.json',{'replace':False,'values':wood_barrels})
write(RES/'data/minecraft/tags/block/mineable/pickaxe.json',{'replace':False,'values':other_barrels})

# Internal IDs use gravel_nether/gravel_ender, but display names should use
# normal word order. BlockItems also resolve their block translation keys.
for name, title in {
    'gravel_nether':'Nether Gravel', 'gravel_ender':'End Gravel',
    'sieve_wood':'Wooden Sieve',
    **{'compressed_'+material:'Compressed '+title for material,title in {
        'dirt':'Dirt','gravel':'Gravel','sand':'Sand','dust':'Dust',
        'soul_sand':'Soul Sand','gravel_nether':'Nether Gravel',
        'gravel_ender':'End Gravel','cobblestone':'Cobblestone'
    }.items()}
}.items():
    LANG['block.exnihiloomnia.'+name] = title
    LANG['item.exnihiloomnia.'+name] = title
write(ASSETS/'lang/en_us.json', LANG)
write(DATA/'tags/block/mineable/pickaxe.json', {'replace':False,'values':['exnihiloomnia:compressed_cobblestone']})
write(DATA/'tags/block/mineable/shovel.json', {'replace':False,'values':['exnihiloomnia:'+n for n in BLOCKS if 'sieve' not in n and n!='compressed_cobblestone']})
write(DATA/'tags/block/mineable/axe.json', {'replace':False,'values':['exnihiloomnia:sieve_wood','exnihiloomnia:heavy_sieve']})
write(DATA/'advancement/recipes/barrels.json', {'criteria':{'inventory':{'trigger':'minecraft:inventory_changed'}},'rewards':{'recipes':['exnihiloomnia:'+name for name, *_ in barrels]}})
write(DATA/'advancement/recipes/leaves.json', {'criteria':{'inventory':{'trigger':'minecraft:inventory_changed'}},'rewards':{'recipes':['exnihiloomnia:crook_wood','exnihiloomnia:crook_bone','exnihiloomnia:cooked_silkworm']}})
write(DATA/'advancement/recipes/core.json', {'criteria':{'inventory':{'trigger':'minecraft:inventory_changed'}},'rewards':{'recipes':recipes}})
# Porcelain progression uses the original artwork, geometry and recipes.
for name in ['ash','porcelain','bucket_porcelain_raw','bucket_porcelain_empty','bucket_porcelain_water','bucket_porcelain_lava','bucket_porcelain_milk']:
    tex('items/'+name,'item/'+name)
    item_model(name,'exnihiloomnia:item/'+name)
for name,title in {'bucket_porcelain_raw':'Unfired Porcelain Bucket','bucket_porcelain_empty':'Porcelain Bucket',
        'bucket_porcelain_water':'Porcelain Water Bucket','bucket_porcelain_lava':'Porcelain Lava Bucket','bucket_porcelain_milk':'Porcelain Milk Bucket'}.items():
    LANG['item.exnihiloomnia.'+name]=title
for name,title in [('crucible','Crucible'),('crucible_raw','Unfired Crucible')]:
    model=json.loads((LEGACY.parent/'models/block'/(name+'.json')).read_text())
    model['parent']='exnihiloomnia:block/crucible' if name=='crucible_raw' else 'minecraft:block/block'
    for key,value in model['textures'].items():
        old=value.removeprefix('exnihiloomnia:blocks/')
        tex('blocks/'+old,'block/'+old)
        model['textures'][key]='exnihiloomnia:block/'+old
    write(ASSETS/'models/block'/(name+'.json'),model)
    item_model(name,block=True)
    LANG['block.exnihiloomnia.'+name]=title
    write(DATA/'loot_table/blocks'/(name+'.json'),{'type':'minecraft:block','pools':[pool(loot_entry('exnihiloomnia:'+name),[{'type':'minecraft:survives_explosion'}])]})
write(ASSETS/'blockstates/crucible_raw.json',{'variants':{'':{'model':'exnihiloomnia:block/crucible_raw'}}})
parts=[{'apply':{'model':'exnihiloomnia:block/crucible'}}]
for content in ['cobblestone','stone','granite','diorite','andesite','polished_granite','polished_diorite','polished_andesite','gravel','netherrack','magma','snow','ice','packed_ice','water','lava']:
    texture='minecraft:block/'+({'magma':'magma','water':'water_still','lava':'lava_still'}.get(content,content))
    for fill in range(1,9):
        faces={face:{'texture':'#content'} for face in ['up','down','north','south','east','west']}
        if content=='water':
            for face in faces.values():face['tintindex']=0
        model_name=f'crucible_contents_{content}_{fill}'
        write(ASSETS/'models/block'/(model_name+'.json'),{'textures':{'content':texture,'particle':texture},
            'elements':[{'from':[1.01,3.01,1.01],'to':[14.99,3.01+12.6*fill/8,14.99],'faces':faces}]})
        parts.append({'when':{'content':content,'fill':str(fill)},'apply':{'model':'exnihiloomnia:block/'+model_name}})
write(ASSETS/'blockstates/crucible.json',{'multipart':parts})
recipe('crucible_raw',['X X','X X','XXX'],{'X':'exnihiloomnia:porcelain'},'crucible_raw')
recipe('bucket_porcelain_raw',['X X',' X '],{'X':'exnihiloomnia:porcelain'},'bucket_porcelain_raw')
write(DATA/'recipe/porcelain.json',{'type':'minecraft:crafting_shapeless','category':'misc',
    'ingredients':['minecraft:clay_ball','exnihiloomnia:ash','minecraft:bone_meal'],'result':{'id':'exnihiloomnia:porcelain','count':1}})
recipes.append('exnihiloomnia:porcelain')
for name,ingredient,result in [('ash','minecraft:stick','exnihiloomnia:ash'),('crucible','exnihiloomnia:crucible_raw','exnihiloomnia:crucible'),('bucket_porcelain','exnihiloomnia:bucket_porcelain_raw','exnihiloomnia:bucket_porcelain_empty')]:
    write(DATA/'recipe'/(name+'.json'),{'type':'minecraft:smelting','category':'misc','ingredient':ingredient,'result':{'id':result},'experience':0,'cookingtime':200})
    recipes.append('exnihiloomnia:'+name)
write(DATA/'advancement/recipes/crucibles.json',{'criteria':{'inventory':{'trigger':'minecraft:inventory_changed'}},'rewards':{'recipes':[r for r in recipes if r.split(':')[1] in ['porcelain','ash','crucible','crucible_raw','bucket_porcelain_raw','bucket_porcelain']]}})
write(ASSETS/'lang/en_us.json',LANG)
write(RES/'data/minecraft/tags/block/mineable/pickaxe.json',{'replace':False,'values':other_barrels+['exnihiloomnia:crucible','exnihiloomnia:crucible_raw']})

# print(f'Generated resources for {len(BLOCKS)+len(leaf_ids)+len(barrels)+2} blocks and {len(recipes)} recipes.')

# Original Witchwater animations, spores and dolls.
for name in ['witchwater_still','witchwater_flowing']:
    tex('blocks/'+name,'block/'+name)
for name,title in {'spores':'Mushroom Spores','doll':'Porcelain Doll','doll_blaze':'Blaze Doll','doll_ender':'End Doll',
        'bucket_witchwater':'Witchwater Bucket','bucket_porcelain_witchwater':'Porcelain Witchwater Bucket'}.items():
    tex('items/'+name,'item/'+name)
    item_model(name,'exnihiloomnia:item/'+name)
    LANG['item.exnihiloomnia.'+name]=title
LANG['block.exnihiloomnia.witchwater']='Witchwater'
write(DATA/'tags/item/black_doll_dyes.json',{'replace':False,'values':['minecraft:black_dye','minecraft:ink_sac']})
write(ASSETS/'blockstates/witchwater.json',{'variants':{'':{'model':'minecraft:block/water'}}})
for gem in ['diamond','emerald']:
    recipe('doll_'+gem,['XGX',' X ','X X'],{'X':'exnihiloomnia:porcelain','G':'minecraft:'+gem},'doll')
recipe('doll_blaze',['BWB','GDG','BRB'],{'B':'minecraft:blaze_powder','W':'minecraft:nether_wart','G':'minecraft:glowstone_dust','D':'exnihiloomnia:doll','R':'minecraft:redstone'},'doll_blaze')
recipe('doll_ender',['BWB','GDG','BRB'],{'B':'#exnihiloomnia:black_doll_dyes','W':'minecraft:nether_wart','G':'minecraft:lapis_lazuli','D':'exnihiloomnia:doll','R':'minecraft:redstone'},'doll_ender')
write(DATA/'advancement/recipes/witchwater.json',{'criteria':{'inventory':{'trigger':'minecraft:inventory_changed'}},'rewards':{'recipes':['exnihiloomnia:doll_'+n for n in ['diamond','emerald','blaze','ender']]}})
write(RES/'data/minecraft/tags/fluid/water.json',{'replace':False,'values':['exnihiloomnia:witchwater','exnihiloomnia:flowing_witchwater']})
write(ASSETS/'lang/en_us.json',LANG)
print(f'Generated resources for {len(BLOCKS)+len(leaf_ids)+len(barrels)+3} blocks and {len(recipes)} recipes.')
