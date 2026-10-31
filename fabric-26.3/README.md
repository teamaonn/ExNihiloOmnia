# Ex Nihilo Omnia — Fabric 26.3 port

**0.5.0-alpha.2 fixes a client crash when infesting cherry and other untinted leaves.**
Leaves without a color provider now retain their original texture using a neutral
color multiplier. Biome-tinted leaves retain their existing gradual color change.
The headless foliage check covers all fourteen supported species, including
in-world rendering color lookup for leaves without a tint source.

0.5.0-alpha.1 added Witchwater, mushroom spores and summoning dolls.
This is an expanding port, not the finished full-feature Omnia port. The supplied
Forge 1.10.2 source remains at the repository root as the implementation reference.

## Requirements

- Minecraft 26.3, Java 25, Fabric Loader 0.19.5 or newer.
- Fabric API 0.160.7+26.3 or a compatible newer 26.3 build.
- Install the mod and Fabric API on both the client and server.

## Witchwater, spores and dolls

Sand sieving with any mesh has an independent **1% spore drop chance**. Spores
turn dirt into mycelium, plant red/brown mushrooms on suitable ground, and turn
cows into mooshrooms while preserving age, health and custom names.

A full wooden water barrel with mycelium anywhere in the 3-by-3 area one block
below has the original 1-in-1,000 chance per loaded tick to start fermenting.
Alternatively, add one spore to a full water barrel of any material. Fermentation
then takes **1,000 loaded ticks (50 seconds)**. Item/fluid automation cannot drain,
refill or replace the contents while fermenting. Progress survives saving.

Witchwater can be transferred by iron/porcelain buckets, dispensers and Fabric
fluid pipes. Placed fluid has original animated textures and water-like flow;
it does not create additional source blocks. Full barrel recipes consume one
bucket of Witchwater plus one ingredient:

| Ingredient | Output |
| --- | --- |
| Sand | One soul sand |
| Golden apple | One chorus fruit |

Five porcelain and either a diamond or emerald craft a porcelain doll in a body
shape. Surround that base as follows (both recipes also take one nether wart
and one redstone):

| Doll | Other ingredients | Full barrel | Summon |
| --- | --- | --- | --- |
| Blaze doll | Four blaze powder, two glowstone dust | Lava | One blaze |
| End doll | Four ink sacs/black dye, two lapis lazuli | Witchwater | One enderman |

Each summon consumes one doll and takes **1,000 loaded ticks (50 seconds)**.
Summoning locks the tank and waits at completion if space above is blocked or
difficulty is Peaceful. Once a mob is successfully placed, the barrel becomes
empty. Save/reload preserves the timer and selected mob. Failed placement and
repeated clicks/ticks cannot create extra mobs. Wooden barrels do not run their
ordinary lava-burning timer while a blaze summon is active, matching the source.

Placed Witchwater transforms skeletons into wither skeletons, spiders into cave
spiders, squid into ghasts, adult villagers into witches, baby villagers into
baby zombie villagers, and pigs into zombified piglins. It charges creepers and
uses vanilla lightning reactions for other animals. Hostile conversions respect
Peaceful difficulty. Player contact applies blindness, Weakness III, wither and
slowness for 210 ticks; contact refreshes the effects. The source's null-lightning
calls are replaced by valid lightning objects, without spawning a real bolt.

## Porcelain and crucibles

Smelt a stick into ash, then combine **one clay ball + one ash + one bone meal**
(shapeless) for one porcelain. Ash also fertilizes growable plants with the
original 1-in-5 growth roll. Seven porcelain in a tall U make an unfired crucible;
three in a bucket shape make an unfired porcelain bucket. Fire either in a
furnace for 200 ticks to obtain its working version.

A fired crucible holds four blocks of unprocessed solids and **8,000 mB** of
fluid. Right-click with a meltable; put a heat source directly underneath.
Empty-hand right-click reports fluid amount, solid fill and current heat.
One material type can be processed at a time; a remaining fluid must also match
the next material's output. No heat pauses processing, and full tanks preserve
all remaining solids. Progress, fractional conversion and buffered inputs survive
save/reload. Breaking a crucible drops the shell and any still-buffered input;
already-loaded solids and tank contents are lost, as in the original.

| Input | Fluid per block |
| --- | --- |
| Cobblestone, stone, granite/diorite/andesite (including polished), gravel | 250 mB lava |
| Netherrack | 1,000 mB lava |
| Magma block | 2,000 mB lava |
| Snow layer | 250 mB water |
| Ice | 1,000 mB water |
| Packed ice | 2,000 mB water |

The original snow entry means a snow **layer**, rather than a snow block.
Original generic stone metadata is mapped to the modern stone/granite/diorite/
andesite IDs, including their polished forms. Four cobblestone produce exactly one lava bucket.

| Heat directly underneath | Heat value | Loaded seconds per input block |
| --- | --- | --- |
| Torch or soul torch | 8 | 156.25 |
| Flowing lava or lit furnace/smoker/blast furnace | 12 | about 104.17 |
| Source lava | 16 | 78.125 |
| Fire, soul fire, magma, lit campfire/soul campfire | 20 | 62.5 |

Every input block occupies the same solid volume, so rich ingredients make more
fluid in the same time. Modern lit heat-source equivalents are included;
extinguished sources provide no heat. Unregistered/modded heat sources and the
original custom registry/configuration system remain future work.

Crucible automation follows the original default: item input and fluid extraction
from the **top**. Vanilla hoppers feed a persistent input buffer; Fabric item
and fluid transfers support transaction rollback. Fluid injection is rejected.

Porcelain buckets collect/place water and lava, including waterlogging and
vanilla Nether evaporation, milk adult cows/mooshrooms, and cure effects when
milk is drunk. They work with barrels, crucibles, Fabric fluid-container transfers
and dispensers. Containers remain porcelain after use, crafting or smelting;
porcelain lava buckets have vanilla lava's furnace fuel component. Empty buckets
stack to 16; filled ones stack to one. A partial tank never yields a full bucket.
Witchwater has both iron and porcelain buckets. Arbitrary mod-fluid buckets remain future work.

## Processing barrels

There are 31 variants: 13 woods (including poplar and bamboo), stone, clear glass,
and all 16 stained-glass colors. They use the original Omnia shell geometry and
modern vanilla material textures, with visible contents in eight fill levels.
Recipes use six matching planks/stone/glass and one matching slab (or glass pane).

Right-click with compost materials to fill a barrel. Eight leaves or saplings
fill it; a full batch takes **1,000 loaded ticks (about 50 seconds)** and makes
one dirt block. Original compost quantities are retained for the original foods,
flowers, mushrooms, flesh, string and other materials. Modern crops/seeds and
all tagged saplings/leaves are also supported. Right-click to collect a ready
output; empty-hand clicks also report fill/processing status.

Exposed rain fills a barrel at **1 mB per loaded tick**, up to one bucket (1,000
mB). Roofs and non-rainy weather/biomes prevent collection. Fluid buckets fill
empty tanks; a full tank can refill an empty bucket. Mixing fluids, overflowing a
tank and taking a full bucket from a partial tank are rejected without consumption.

Full-tank crafting:

| Tank | Added item / overhead fluid | Result |
| --- | --- | --- |
| Water | One dust block | One clay block |
| Lava | One redstone dust | One netherrack |
| Lava | One glowstone dust | One End stone |
| Lava | Water above the barrel | One obsidian |
| Water | Source lava above the barrel | One stone |
| Water | Flowing lava above the barrel | One cobblestone |

Item recipes consume one ingredient and the tank. Overhead reactions consume the
contained fluid but retain the fluid above. The original water/lava-overhead rule
also accepts partial water tanks. Full water barrels have the original 1-in-1,000
per-tick chance to freeze into one ice block in a cold biome. Ready dirt may become
grass or mycelium when a matching nearby block spreads in sufficient light.
Wooden water barrels can slowly mossify cobblestone beneath them in dim conditions;
this originally client-only behavior is now server-authoritative.

Ordinary wooden barrels holding lava burn after **400 loaded ticks (20 seconds)**.
A full one is replaced by its lava source; a partial one can ignite fire above it
without creating extra fluid. Stone, glass and modern crimson/warped barrels safely
hold lava. Growth, composting, fluid amounts, heat and pending items survive saves.

Milk added to a full water barrel starts slime growth outside Peaceful difficulty.
After 1,000 loaded ticks, use a crook with clear space above to release one small,
low-health slime, costing one crook durability. This retains the original barrel
slime mechanic independently of the doll system.

Hoppers insert from the top/sides and extract finished outputs and returned buckets
from below. Input buffering and bucket returns are persisted. Fabric item/fluid
pipes use transactional storage; simulations/aborts do not consume resources.
Water, lava and Witchwater are the supported pipe fluids. GUI-free operation
and the original one-bucket capacity are retained. Updates are throttled to eight
ticks, rather than sending tank data every rain tick.

Breaking a barrel drops its empty shell, finished item and buffered inventory items
once. **Stored fluid and partial compost are not returned**, matching the original
break behavior. New recipes also unlock in existing worlds on inventory changes.

Optional packed-ice behavior, mod-fluid/gas integrations, and the original unreachable special-compost states
remain in the later parity/configuration work. Ordinary compost continues making
dirt rather than silently reactivating those shadowed modifier branches.

## Crooks, silkworms and leaves

Craft a wooden crook with four sticks, or a bone crook with four bones, in the
original hook pattern: two across the top, one below the right-hand ingredient,
then one below that. Wooden crooks have 88 durability and bone crooks have 196;
they repair with sticks and bone meal respectively and accept mining enchantments.

Harvest ordinary leaves or grass/ferns with a crook for a second independent
normal loot roll and a 2% chance of a silkworm. The normal loot rolls respect the
crook's enchantments. Fortune does not increase the separate worm roll, matching
the supplied registry. The last durability point still supplies enhanced loot.
Left-click a creature to push it without melee damage; right-click to pull it.
Both interactions use one durability. Crooks do not multiply infested-leaf loot.

Right-click ordinary leaves with a silkworm to infest them, consuming one worm
in Survival. All 14 vanilla leaf types retain their species, including poplars.
Waterlogging, leaf distance and persistent/player-placed status are retained.
Tagged modded leaves without block entities can also be infested using the oak
visual fallback; block-entity leaves are skipped to avoid destroying custom data.

Infestation takes **2,000 loaded ticks (about 100 seconds)** to mature. Leaves
lighten through four tint stages and begin random spreading attempts after 60%
growth, with the first attempt around 65 seconds and subsequent attempts roughly
every five seconds. Each attempt selects one of the surrounding 3×3×3 positions;
spreading is not guaranteed on every attempt. Unloaded chunks are never forced in.
Growth and the partly elapsed spreading timer survive saving/reloading.

Empty-hand right-click reports exact growth. The reminder does not intercept held
items. Harvesting normally yields string; shears or Silk Touch recover that leaf
species as an infested leaf block, which starts growing afresh when placed.
Player-placed infested leaves are persistent. Natural unsupported leaves decay,
retain the original small decay-string chance and restore any contained water.

String drops follow the supplied source's actual three-roll behavior: two rolls
scale with growth (up to 15% and 60%), plus an independent 50% string roll. String
is not guaranteed from an individual leaf. Growth-dependent loot uses the current
quarter-growth stage; precise growth/spread timing is stored in the block entity.
Silkworms come from crooking ordinary leaves, not the infested-leaf string table.
Cooked silkworms are obtained by furnace smelting and restore 3 nutrition with
saturation modifier 0.2, as in the original. The live worm icon retains its original
animation metadata; no new artwork was generated.

## Sieving and resource processing

- Regular and heavy sieves; string, flint, iron and diamond meshes.
- Dirt, gravel, sand, dust, soul sand, Nether gravel and End gravel sieving.
- Reversible 9:1 compression for those materials and cobblestone.
- Heavy sieving performs nine independent normal loot-table passes.
- Wood, stone, iron, gold, diamond and netherite hammers.
- Cobblestone/cobbled deepslate → gravel → sand → dust hammer processing.
- Netherrack → Nether gravel; End stone → End gravel.
- Compressed cobblestone → compressed gravel → compressed sand → compressed dust.
- Pebbles (four → cobblestone), and modern crops/saplings among dirt rewards.
- Vanilla iron/copper/gold nugget rewards: a successful metal roll gives two,
  with a 25% chance of a third, preserving the former average piece value.
- Four legacy pieces craft into nine nuggets. Old piece IDs remain valid for saves
  but are hidden from the creative tab and no longer appear in sieve rewards.

Place a sieve, install one mesh, add one material block, then right-click eight
times with an empty hand. Heavy sieves accept compressed material. Material lowers
and matching fragments fall through the mesh as processing advances. Higher meshes
retain lower-tier drops. Sneak-right-click with an empty hand to return unfinished
material; once empty, the same action removes the mesh. Breaking a sieve returns
its frame, mesh and unfinished material. Machine progress survives saving/reloading.

Compression is one level, not recursive. The custom loose sieve materials currently
use solid block physics; falling-block parity is pending. Creative hammer breaking
keeps vanilla Creative behavior. Mesh models use distinct vanilla-material lattices.
The netherite hammer still reuses the diamond hammer texture.

## Remaining full-port scope

- Optional/special barrel states, mod-fluid/gas integrations and configurable registries.
- Remaining ore-processing variants/adaptations and customizable recipe registries.
- Remaining seeds, chorus growth, end cake and astrolabe/void-world support.
- Sifters, remaining enchantment behavior, optional integrations and automation
  actually present in the supplied source.
- Full legacy-feature review and client/modpack verification.

## Development and validation

From this folder, with Java 25:

```sh
./gradlew build
./gradlew runGameTest
python3 tools/generate_resources.py
```

The server tests cover sieve lifecycle, heavy-roll independence, mesh gates,
compression round trips, nugget migration, readable block-item name keys, hammer
behavior, all leaf species, worm consumption, growth/spread/save/reload, decay,
string/shears/Silk Touch loot, crook harvesting and creature interaction, and cooking.
Barrel tests add all 31 crafting variants, compost/reload timing, bucket exchange,
fluid recipes, hopper extraction, transactional rollback, weather/ice, lava reactions,
heat persistence, partial-fluid safety, break recovery and slime hatching.
Client visual behavior still requires in-game verification. Resource references
are checked separately against the bundled original and vanilla assets.

Original Omnia source and artwork credit: Jozufozu and the original contributors.

The client tint regression check (`verifyLeafTints`, included in `build`) uses
Minecraft's actual vanilla color registry without opening a window. It verifies
that oak/birch/spruce retain their original colors at zero growth and only reach
white at the final growth stage. Full client appearance still needs an in-game check.


## Optional JEI support (0.5.0-alpha.3)

Install JEI for Minecraft 26.3 on the client to browse processing recipes with
JEI's recipe and use keys. Ex Nihilo does not require JEI, including on servers.
No JEI implementation or API is bundled in the release JAR.

The optional `jei_mod_plugin` entrypoint adds Sieving, Hammering, Barrel
Composting, Barrel Processing, Crucible Melting, Crucible Heat Sources, and
Crooks and Silkworms. Vanilla crafting and cooking recipes remain handled by
JEI itself. The integration compiles against JEI 31.0.0.5 and 31.17.0.103;
the Gradle development dependency uses 31.17.0.103.

Sieving pages show each eligible mesh, the regular or heavy sieve, the chance
of at least one reward, and independent roll counts. Heavy sieves repeat the
live drop table nine times; their displayed output stack is a single successful
roll, with the maximum total stated below. Metal rolls show the extra-nugget
chance. Composting pages show how many identical items fill a barrel; materials
can also be mixed. Fluid slots use Fabric's native units while notes show mB.
Tools and meshes are listed as crafting stations, rather than consumed inputs.
World-placement requirements, random growth, lava-safe barrel requirements,
and mob previews are explained on the relevant pages. Spawn eggs on summoning
pages represent the spawned mob, not a craftable egg.

Recipe data is read from the gameplay tables for sieving, hammering, composting,
barrel item/fluid conversions, crucible melting, and heat. Server-side recipe
regressions are included in the existing Fabric GameTest suite.

Validation: all production sources compiled against both API versions; a real
Fabric dedicated server without any JEI classes passed the catalog regression
checks after loading vanilla tags. This is headless verification; JEI's rendered
interface still needs an in-game check.
