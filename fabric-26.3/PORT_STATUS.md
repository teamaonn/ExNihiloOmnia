# Port checkpoint — 2026-10-25

Target: the full Ex Nihilo Omnia experience on Fabric Minecraft 26.3.
Do not remove pending systems merely because Joshua does not currently use them.

## Current checkpoint: 0.3.0-alpha.1

- Compilable Fabric project and working Gradle 9.7.1 wrapper on Java 25.
- Sieves/heavy sieves, meshes, seven sieve inputs, eight compressed blocks.
- Reversible nine-to-one crafting and nine independent heavy-sieve loot passes.
- Hammers, dust/Nether gravel/End gravel, vanilla metal nuggets and pebbles.
- Original texture reuse, modern recipe/loot/item-model formats and creative tab.
- Persistent sieve state, partial-input cancellation and break recovery.
- Crooks, silkworms, cooked worms, spreading infested leaves and corrected foliage tint.
- Processing barrels with compost, rain, fluids, crafting, hopper/pipe storage and slime hatching.

## Validation completed

`build runGameTest` passed with all sixteen required server tests successful (fifteen
custom test methods and Fabric's built-in test). Assertions cover every compression
round trip; heavy-vs-normal roll equivalence and non-multiplication; mesh gates;
normal/heavy input rules; mesh/input consumption; partial-state serialization;
loaded/empty sieve loot; cancellation/mesh removal; hammer output and durability.
The release JAR's metadata and exclusion of test classes were also checked.

The environment's Loom UNIX-socket capability probe required a local build-tool
workaround to report unsupported sockets. That workaround is outside the repository
and the mod JAR; the project itself uses the published Loom 1.18.2 dependency.
Minecraft service-discovery and system-info warnings occurred in the isolated test
environment; they did not prevent the server tests from passing.

Joshua has confirmed the earlier sieve and leaf features in-game. New barrel
appearance and compatibility with the entire modpack have not yet been verified. The mesh tiers now use distinct vanilla-material lattices. The netherite
hammer still reuses the diamond texture. No new artwork was generated.

## Next implementation work

1. Optional/special barrel states, mod-fluid integrations and configurable registries.
2. Remaining seeds/chorus growth, sifters, End Cake, world-generation options and integrations.
3. Complete legacy-feature review and in-game client/modpack verification.

Keep the legacy root source as the reference. Work on the port under `fabric-26.3`.
The repository's default branch stays `Working_Branch`; the port checkpoint uses
`fabric-26.3-port` until ready to integrate.

## Alpha 2 sieve animation fix

The previous material model stayed at a fixed height and emitted no particles.
Both sieve types now select one of eight material heights from persisted progress
and emit material-matched falling fragments on every processing click, including
the completion click. Heavy sieves emit a denser burst. No new artwork is used.

Alpha 2 validation: `build` passed, including all five required server tests.
All 112 material/progress model combinations reference an existing model and
lower the surface with each successive stage. Client appearance remains unverified.

## Alpha 3 nugget, mesh and naming adaptation

- New metal rewards use vanilla iron, copper and gold nuggets. Successful rolls
  give two nuggets plus a 25% chance of one extra, preserving the former average
  ingot yield. The previous drop chances and mesh gates are retained.
- Old piece IDs remain registered for existing saves. They no longer appear in
  the creative tab or sieve rewards. Four old pieces craft into nine nuggets.
- Mesh items and installed meshes share open lattice geometry and distinct white,
  flint-gray, iron-silver and diamond-cyan vanilla material textures.
- Block items explicitly use block translation keys. Compressed materials have
  readable names, including Compressed Nether Gravel and Compressed End Gravel.
- Progress messages and empty-hand reminders are retained.

Alpha 3 validation: clean build and all six required server tests pass. Vanilla
texture references and shared inventory/installed mesh models were verified.
Actual in-game client appearance still needs a visual check.

## Alpha 4 client-log correction

The supplied client log reported missing `particle` texture references for all
56 sieve-content models. Each model now explicitly uses its material texture
for particles as well as visible faces. This changes resources only. The prior
nugget, mesh, naming, animation and tooltip behavior is retained.

Alpha 4 validation: assembly passes. All 56 warnings in the supplied client log
were matched to model files and their corrected particle references verified in
the release JAR. No gameplay code changed, so the alpha 3 server tests were not
repeated. A fresh client reload log is still needed to confirm the warnings clear.

## 0.2.0-alpha.1: crooks, silkworms and infested leaves

- Wooden/bone crooks preserve original recipes, 88/196 durability, mining speed,
  repair ingredients, doubled ordinary-leaf loot, 2% worm chance and push/pull.
- Crook harvesting uses the actual successful vanilla mining path and handles the
  last durability point. No extra loot is produced before a protected break.
- Silkworm use consumes one in Survival and none in Creative; already-infested or
  ineligible blocks consume nothing. Original worm/crook textures are reused,
  including the live-worm animation metadata. Cooked worms retain their food stats.
- Fourteen vanilla species are registered as infested leaf variants, including the
  three poplar colors. Waterlogging, distance and persistent status are retained.
  Tagged modded leaves without block entities use the oak visual fallback.
- Growth matures in 2,000 loaded ticks. Spread starts beyond 60% with 100-tick
  random-neighbour attempts. Exact growth and partial spread timers are persisted.
- Client block tints blend the source foliage toward white in quarter stages.
  Empty-hand progress reminders are server-authoritative and do not block held items.
- Normal string loot matches the supplied source's 15%/60% growth-dependent rolls
  plus its independent 50% string roll. No speculative extra worm drop was added.
- Shears/Silk Touch preserve the species; placed leaves are permanent. Natural
  decay preserves the source's smaller string chance and restores contained water.
- Vanilla leaf/hoe tags, mining enchantment tags and leaf flammability are included.

This checkpoint completes the announced leaf/string phase, not the full Omnia port.

0.2.0-alpha.1 validation: `build` passes with all ten required server tests.
New texture references and original silkworm animation metadata are verified.
Release metadata and exclusion of test classes are checked. New client tint
appearance still needs in-game verification.

## 0.2.0-alpha.2: retain original leaf color at zero infestation

The first leaf build queried only Fabric's dynamic color factories. Ordinary
vanilla leaves use static tint sources in BlockColors, so the lookup fell back
to white immediately. The client now preserves dynamic factories when present
and otherwise uses the actual vanilla static source (including biome colors)
before applying the infestation blend. Timing and all server mechanics are unchanged.

Validation: build passes, all ten server tests pass, and a headless client-registry
regression checks real oak/birch/spruce colors at all five growth stages. The new
check runs as part of `build` without opening a window. Visual confirmation in the
actual client is still pending.

## 0.3.0-alpha.1: processing barrels

- 31 variants: 13 woods, stone, clear glass and 16 stained glass colors, using the
  supplied shell geometry, modern material textures and visible contents.
- One-bucket water/lava storage, 1 mB/tick exposed rain and full-bucket exchanges.
- Original compost quantities for the old ingredients, plus modern crop support;
  1,000-tick dirt composting, with persistent input, progress and weighted tint.
- Water+dust clay; lava+redstone netherrack; lava+glowstone End stone. Overhead
  water/lava produce obsidian/stone/cobblestone with the source's consumption rules.
- Water freezing, ready-dirt grass/mycelium spreading, and server-authoritative
  moss growth underneath wooden water barrels.
- Ordinary wood burns after 400 hot ticks. Partial lava can ignite fire but never
  creates a full lava source. Modern crimson/warped barrels are fireproof.
- Milk/water slime growth and crook release retain their original timing.
- Vanilla hopper access and transactional Fabric item/fluid storage. Returned
  containers are buffered, output/break recovery is single-use, and sync is throttled.
- New recipe advancements ensure existing worlds receive the barrel/leaf recipes.
- Witchwater/dolls, optional packed ice, mod fluids/gases and source special compost
  modifiers are still pending; shadowed source branches were not silently enabled.

Validation: all 16 server GameTests and the headless foliage tint checks pass.
The six new barrel test methods cover every recipe variant, full compost/load
lifecycle, bucket exchanges, full/partial fluid rules, recipes, real hopper output,
item/fluid transaction rollback, rain/roof/freezing, heat/load persistence, partial
lava safety, break recovery and slime release. Static resource checks validate
248 referenced model graphs, particle aliases and texture files. Actual client
barrel appearance and full modpack compatibility still need in-game checks.


## 0.4.0-alpha.1: porcelain and crucibles

- Original porcelain progression: sticks smelt to ash; clay + ash + bone meal
  make porcelain; seven porcelain form an unfired crucible, three an unfired
  bucket; furnace firing takes 200 ticks. Ash retains its weak fertilizer use.
- Original crucible shell geometry and all supplied porcelain/ash/bucket artwork,
  with validated modern texture paths, hollow collision and visible fill levels.
- Four-block solid reservoir, eight-bucket tank, all original default meltables
  and heat rates. Old generic stone metadata maps to modern granite/diorite/
  andesite IDs, including polished forms. Modern lit campfires, soul heat sources, smokers and blast furnaces
  are explicit equivalents. The snow recipe uses layers, matching Blocks.SNOW.
- Integer work accounting preserves fractional conversion and final output exactly;
  a full tank pauses processing without overflow or loss. Same input material is
  required while a batch remains; incompatible tank fluids reject new ingredients.
- Exact work, conversion remainder, tank and buffered input persist through reload.
  Top-only hopper/item input and fluid extraction match the original default;
  native Fabric container transfers roll back safely, fluid injection is rejected.
- Reusable porcelain water/lava/milk buckets, source pickup/placement/waterlogging,
  effect curing, vanilla lava furnace fuel, dispenser use and transactional item
  fluid handlers. Barrel handling preserves porcelain remainders too.
- New recipe-unlock advancement supports already-existing worlds.
- Custom recipe/heat registries, configurable side access, Witchwater/universal
  mod-fluid buckets and optional integrations remain pending.

Validation: all 22 required server GameTests and the three-species headless
foliage checks pass. Six new tests cover crafting/smelting, all fourteen modern
meltable IDs, heat/no-heat timing, reload at fractional progress, full-tank
backpressure, top-only automation, rollback, actual hoppers and furnace fuel,
world bucket/dispenser exchange, cow milk/effect removal, porcelain barrel
exchange, partial/stacked buckets and ash fertilizer. Resource inheritance,
particle aliases and texture files pass for all 385 referenced models.
Client crucible appearance and full modpack compatibility need in-game checks.


## 0.4.0-alpha.2: unfired crucible visibility

The original raw crucible inherits its shell elements from the fired crucible.
The resource converter had replaced that parent with the geometry-free vanilla
block base, making the raw crucible invisible in inventory, recipes and the world.
Preserved the original mod parent and the raw side/bottom texture overrides.
Added `tools/verify_crucible_models.py` to resolve both shell models and verify
all ten visible elements, texture files, raw/fired overrides and item references.
Both shells and all 385 resource model graphs pass. Gameplay code is unchanged
from alpha.1, whose 22 server tests passed.


## 0.5.0-alpha.1: Witchwater, spores and dolls

- Original Witchwater still/flowing sprites and animation metadata, registered as
  finite water-like source/flowing fluids. Uses the actual 26.3 FluidModel and
  Fabric FluidRenderingRegistry APIs for client rendering.
- Sand sieving supplies spores at the original independent 1% chance. Spores
  convert dirt to mycelium, plant mushrooms and convert cows to mooshrooms.
- Wooden full-water barrels naturally begin fermenting near mycelium with the
  original 1-in-1,000 per-tick trigger; spores start it immediately in any shell.
  Transformation itself takes 1,000 loaded ticks with locked transactional storage.
- Witchwater/sand soul sand and Witchwater/golden-apple chorus fruit crafting.
- Original porcelain/diamond-or-emerald doll recipes, blaze and End dolls, with
  their original ingredients. Ink sacs and modern black dye both satisfy black.
- Lava/blaze-doll and Witchwater/End-doll summons take 1,000 ticks, lock their tank,
  persist exact progress, and wait for a clear mob bounding box/non-Peaceful mode.
  Successful spawn empties the tank once; blocked/repeated operations do not dupe.
- Skeleton, spider, squid, villager, baby-villager, pig and creeper transformations;
  other animals retain vanilla lightning responses. Valid non-spawned lightning
  objects avoid the old null-bolt crash; repeated ordinary-animal hits are throttled.
  Player contact preserves all four effects and their original 210-tick duration.
- Iron and porcelain Witchwater buckets, world source pickup/placement, dispenser
  behavior, Fabric item-container transfers, and retained porcelain remainders.
- Barrels display Witchwater/fermentation/summon/product surfaces and status text;
  fluid injection/extraction is prevented during timed magical states.

Validation: all 28 required server GameTests pass, including six new end-to-end
methods for fermentation, natural triggers, timer reload, locked/aborted transfers,
recipes, doll construction/single summons/Peaceful/blocked space, actual source
flow/pickup/placement, spores/mycelium/mushrooms/cows/sieve drops, mob conversions
and player effects through the actual fluid contact hook. Existing foliage tint
checks pass. All 440 model graphs, original fluid animations and crucible shells
pass resource validation. User confirmed crucible mechanics and the alpha.2 model
fix in-game; new Witchwater/doll client appearance still needs in-game confirmation.
Optional packed ice, special compost modifiers, arbitrary mod fluids and configurable
registries remain future parity work, along with the other unported legacy systems.
