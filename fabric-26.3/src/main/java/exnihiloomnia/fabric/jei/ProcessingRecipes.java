package exnihiloomnia.fabric.jei;

import exnihiloomnia.fabric.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.*;

/** Viewer data only. No JEI or client classes are referenced by this catalog. */
public final class ProcessingRecipes {
    public enum Kind { SIEVING, HAMMERING, COMPOSTING, BARREL, CRUCIBLE, HEAT, CROOK }
    public record FluidAmount(Fluid fluid, int millibuckets) {}
    public record Page(Kind kind, List<ItemStack> inputs, List<ItemStack> tools,
                       ItemStack output, FluidAmount fluidInput, FluidAmount fluidOutput,
                       List<String> notes) {
        public Page { inputs=List.copyOf(inputs); tools=List.copyOf(tools); notes=List.copyOf(notes); }
    }
    private ProcessingRecipes() {}
    private static ItemStack own(String id) { return new ItemStack(ExNihiloOmnia.ITEMS.get(id)); }
    private static List<ItemStack> tools(Class<?> type) {
        return ExNihiloOmnia.ITEMS.values().stream().filter(type::isInstance).map(ItemStack::new).toList();
    }
    private static Page page(Kind kind,List<ItemStack> in,List<ItemStack> tools,ItemStack out,String... notes) {
        return new Page(kind,in,tools,out,null,null,List.of(notes));
    }
    public static List<Page> all() {
        List<Page> out=new ArrayList<>();
        sieving(out);
        var hammers=tools(ExNihiloOmnia.HammerItem.class);
        for(Block block:BuiltInRegistries.BLOCK) {
            Block result=ExNihiloOmnia.hammerOutput(block);
            if(result!=null)out.add(page(Kind.HAMMERING,List.of(new ItemStack(block)),hammers,new ItemStack(result),"Break with a hammer","Tool takes durability damage"));
        }
        for(Item item:BuiltInRegistries.ITEM) {
            var stack=new ItemStack(item);
            int compost=BarrelCompost.value(stack);
            if(compost>0)out.add(page(Kind.COMPOSTING,List.of(stack.copyWithCount((1000+compost-1)/compost)),List.of(),new ItemStack(Items.DIRT),
                    String.format(Locale.ROOT,"Each item: %.1f%% full",compost/10.0),"Mix materials to reach 100%","Empty barrel; wait 50 seconds"));
            var melt=CrucibleRecipes.melt(stack);
            if(melt!=null)out.add(new Page(Kind.CRUCIBLE,List.of(stack),List.of(),ItemStack.EMPTY,null,new FluidAmount(melt.fluid(),melt.millibuckets()),
                    List.of(melt.millibuckets()+" mB per item","Place a heat source below","Heat sources: crucible uses")));
        }
        heat(out);
        barrels(out);
        crooks(out);
        return List.copyOf(out);
    }
    private static void sieving(List<Page> pages) {
        for(int mesh=1;mesh<=4;mesh++)for(int input=1;input<=7;input++)for(boolean heavy:new boolean[]{false,true}) {
            Map<Item,List<SieveDrops.Drop>> grouped=new LinkedHashMap<>();
            for(var drop:SieveDrops.entries())if(drop.input()==input&&drop.minimumMesh()<=mesh)
                grouped.computeIfAbsent(drop.item(),k->new ArrayList<>()).add(drop);
            int passes=heavy?9:1;
            for(var entry:grouped.entrySet()) {
                double miss=1;int min=Integer.MAX_VALUE,max=0;
                for(var drop:entry.getValue()) {miss*=Math.pow(1.0-drop.probability(),passes);min=Math.min(min,drop.count());max+=passes*(drop.count()+(drop.bonusChance()>0?1:0));}
                String yield=entry.getValue().getFirst().bonusChance()>0?"Each hit: 2; 25% chance of 3":"Each hit: 1 item";
                pages.add(page(Kind.SIEVING,List.of(new ItemStack(ExNihiloOmnia.materialItem(input,heavy))),
                        List.of(own(ExNihiloOmnia.MESHES[mesh-1]),own(heavy?"heavy_sieve":"sieve_wood")),new ItemStack(entry.getKey(),min),
                        String.format(Locale.ROOT,"At least one: %.2f%%",100*(1-miss)),
                        passes*entry.getValue().size()+" independent roll(s)",yield,"Maximum total: "+max));
            }
        }
    }
    private static void heat(List<Page> out) {
        for(Block block:BuiltInRegistries.BLOCK) {
            if(block.asItem()==Items.AIR)continue;
            var state=block.defaultBlockState();
            if(state.hasProperty(BlockStateProperties.LIT))state=state.setValue(BlockStateProperties.LIT,true);
            int heat=CrucibleRecipes.heat(state);
            if(heat<=0)continue;
            out.add(page(Kind.HEAT,List.of(new ItemStack(block)),List.of(),own("crucible"),"Place below the crucible","Heat: "+heat+(state.hasProperty(BlockStateProperties.LIT)?" (must be lit)":""),
                    String.format(Locale.ROOT,"%.1f seconds per item",CrucibleEntity.ITEM_WORK/(heat*2.0*20))));
        }
    }
    private static void barrels(List<Page> out) {
        for(Fluid fluid:List.of(Fluids.WATER,Fluids.LAVA,ExNihiloOmnia.WITCHWATER))for(Item item:BuiltInRegistries.ITEM) {
            var input=new ItemStack(item);var result=BarrelEntity.fluidRecipe(fluid,input);
            if(!result.isEmpty())out.add(new Page(Kind.BARREL,List.of(input),List.of(),result,new FluidAmount(fluid,1000),null,
                    List.of("Consumes a full fluid barrel",fluid==Fluids.LAVA?"Use stone or glass for lava":"Right-click to collect output")));
        }
        out.add(new Page(Kind.BARREL,List.of(own("spores")),List.of(),ItemStack.EMPTY,new FluidAmount(Fluids.WATER,1000),new FluidAmount(ExNihiloOmnia.WITCHWATER,1000),List.of("Transforms in 50 seconds")));
        out.add(new Page(Kind.BARREL,List.of(new ItemStack(Items.MYCELIUM)),List.of(),ItemStack.EMPTY,new FluidAmount(Fluids.WATER,1000),new FluidAmount(ExNihiloOmnia.WITCHWATER,1000),List.of("Mycelium below or adjacent below","Wooden barrel; random start","Then transforms in 50 seconds")));
        out.add(new Page(Kind.BARREL,List.of(new ItemStack(Items.WATER_BUCKET)),List.of(),new ItemStack(Items.OBSIDIAN),new FluidAmount(Fluids.LAVA,1000),null,List.of("Water fluid above a full barrel","Use stone or glass for lava","Water above is not consumed")));
        out.add(new Page(Kind.BARREL,List.of(new ItemStack(Items.LAVA_BUCKET)),List.of(),new ItemStack(Items.STONE),new FluidAmount(Fluids.WATER,1),null,List.of("Source lava directly above","Any water amount in barrel","Lava above is not consumed")));
        out.add(new Page(Kind.BARREL,List.of(new ItemStack(Items.LAVA_BUCKET)),List.of(),new ItemStack(Items.COBBLESTONE),new FluidAmount(Fluids.WATER,1),null,List.of("Flowing lava directly above","Any water amount in barrel","Lava above is not consumed")));
        out.add(new Page(Kind.BARREL,List.of(),List.of(),new ItemStack(Items.ICE),new FluidAmount(Fluids.WATER,1000),null,List.of("Cold enough to snow","Full water barrel; random tick")));
        out.add(page(Kind.BARREL,List.of(new ItemStack(Items.DIRT),new ItemStack(Items.GRASS_BLOCK)),List.of(),new ItemStack(Items.GRASS_BLOCK),"Dirt output remains in barrel","Nearby grass; light level 9+","Random growth; grass retained"));
        out.add(page(Kind.BARREL,List.of(new ItemStack(Items.DIRT),new ItemStack(Items.MYCELIUM)),List.of(),new ItemStack(Items.MYCELIUM),"Dirt output remains in barrel","Nearby mycelium; light 9+","Random growth; mycelium retained"));
        out.add(new Page(Kind.BARREL,List.of(new ItemStack(Items.COBBLESTONE)),List.of(),new ItemStack(Items.MOSSY_COBBLESTONE),new FluidAmount(Fluids.WATER,1),null,List.of("Cobble below wooden barrel","Low light (<5); random spread","More water speeds up growth","Changes world block; water kept")));
        summon(out,"doll_blaze",Fluids.LAVA,Items.BLAZE_SPAWN_EGG,"Spawns a Blaze; 50 seconds");
        summon(out,"doll_ender",ExNihiloOmnia.WITCHWATER,Items.ENDERMAN_SPAWN_EGG,"Spawns an Enderman; 50 sec");
        out.add(new Page(Kind.BARREL,List.of(new ItemStack(Items.MILK_BUCKET)),tools(CrookItem.class),new ItemStack(Items.SLIME_SPAWN_EGG),new FluidAmount(Fluids.WATER,1000),null,List.of("Wait 50 sec; release with crook","Clear space; non-Peaceful","Spawns Slime, not a spawn egg")));
    }
    private static void summon(List<Page> out,String doll,Fluid fluid,Item preview,String text) {
        out.add(new Page(Kind.BARREL,List.of(own(doll)),List.of(),new ItemStack(preview),new FluidAmount(fluid,1000),null,
                List.of(text,"Clear space; non-Peaceful","Mob preview, not a spawn egg",fluid==Fluids.LAVA?"Use stone or glass for lava":"Consumes Witchwater")));
    }
    private static void crooks(List<Page> out) {
        var crooks=tools(CrookItem.class);
        for(Block block:BuiltInRegistries.BLOCK) {
            if(block.asItem()==Items.AIR)continue;
            if(CrookItem.crookable(block.defaultBlockState()))out.add(page(Kind.CROOK,List.of(new ItemStack(block)),crooks,own("silkworm"),"Break with a crook: 2% chance","Also adds a normal loot roll"));
            if(!InfestedLeavesBlock.canInfest(block.defaultBlockState()))continue;
            var infested=ExNihiloOmnia.INFESTABLE.getOrDefault(block,ExNihiloOmnia.INFESTABLE.get(Blocks.OAK_LEAVES));
            out.add(page(Kind.CROOK,List.of(new ItemStack(block),own("silkworm")),List.of(),new ItemStack(infested),"Use silkworm on leaves","Matures in 100 seconds","Spreads to nearby leaves"));
        }
        for(var block:ExNihiloOmnia.INFESTABLE.values())out.add(page(Kind.CROOK,List.of(new ItemStack(block)),List.of(),new ItemStack(Items.STRING),"Harvest mature leaves","Two rolls: 15% and 60%","Plus 50% bonus string roll","Avoid shears and Silk Touch"));
    }
}
