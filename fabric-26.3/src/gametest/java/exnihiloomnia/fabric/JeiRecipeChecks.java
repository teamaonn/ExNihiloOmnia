package exnihiloomnia.fabric;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.*;
import exnihiloomnia.fabric.*;
import exnihiloomnia.fabric.jei.ProcessingRecipes;
import static exnihiloomnia.fabric.jei.ProcessingRecipes.*;
public final class JeiRecipeChecks {
 static int checks;static void check(boolean good,String why){if(!good)throw new AssertionError(why);checks++;}
 public static int verify(){
  checks=0;
  var pages=ProcessingRecipes.all();
  for(Kind kind:Kind.values())check(pages.stream().anyMatch(p->p.kind()==kind),"category present: "+kind);
  Map<Kind,Long> totals=new EnumMap<>(Kind.class);for(Kind k:Kind.values())totals.put(k,pages.stream().filter(p->p.kind()==k).count());System.out.println("EXNIHILO JEI PAGES: "+totals);
  for(var p:pages){
   check(!p.output().isEmpty()||p.fluidOutput()!=null,"every page has a searchable output");
   check(p.inputs().stream().noneMatch(ItemStack::isEmpty),"no air input");
   check(p.tools().stream().noneMatch(ItemStack::isEmpty),"no missing tools");
   check(p.notes().size()<=4,"notes fit layout");
   check(p.notes().stream().allMatch(s->s.length()<=34),"note length fits layout: "+p.notes());
  }
  for(int mesh=1;mesh<=4;mesh++)for(int input=1;input<=7;input++)for(boolean heavy:new boolean[]{false,true}){
   var material=ExNihiloOmnia.materialItem(input,heavy);var installed=ExNihiloOmnia.ITEMS.get(ExNihiloOmnia.MESHES[mesh-1]);
   var rows=pages.stream().filter(p->p.kind()==Kind.SIEVING&&p.inputs().getFirst().is(material)&&p.tools().getFirst().is(installed)).toList();
   Set<Item> expected=new HashSet<>();for(var d:SieveDrops.entries())if(d.input()==input&&d.minimumMesh()<=mesh)expected.add(d.item());
   check(rows.size()==expected.size(),"one indexed result per live eligible drop");
   for(var row:rows)check(expected.contains(row.output().getItem()),"no invented sieve reward");
  }
  var heavy=pages.stream().filter(p->p.kind()==Kind.SIEVING&&p.inputs().getFirst().is(ExNihiloOmnia.materialItem(2,true))&&p.output().is(Items.DIAMOND)).findFirst().orElseThrow();
  check(heavy.notes().get(1).equals("18 independent roll(s)"),"two diamond entries times nine heavy passes");
  check(heavy.notes().getFirst().equals("At least one: 16.55%"),"heavy chance combines independent rolls");
  check(pages.stream().noneMatch(p->p.kind()==Kind.SIEVING&&p.output().is(Items.DIAMOND)&&!p.tools().getFirst().is(ExNihiloOmnia.ITEMS.get("mesh_diamond"))),"diamonds require diamond mesh");
  for(Item item:BuiltInRegistries.ITEM){var s=new ItemStack(item);var melt=CrucibleRecipes.melt(s);if(melt!=null)check(pages.stream().anyMatch(p->p.kind()==Kind.CRUCIBLE&&p.inputs().getFirst().is(item)&&p.fluidOutput().fluid()==melt.fluid()&&p.fluidOutput().millibuckets()==melt.millibuckets()),"real crucible yield indexed");int amount=BarrelCompost.value(s);if(amount>0)check(pages.stream().anyMatch(p->p.kind()==Kind.COMPOSTING&&p.inputs().getFirst().is(item)&&p.inputs().getFirst().getCount()==(1000+amount-1)/amount),"real compost fill indexed");}
  for(Block block:BuiltInRegistries.BLOCK){var result=ExNihiloOmnia.hammerOutput(block);if(result!=null)check(pages.stream().anyMatch(p->p.kind()==Kind.HAMMERING&&p.inputs().getFirst().is(block.asItem())&&p.output().is(result.asItem())),"hammer mapping indexed");}
  for(Fluid fluid:List.of(Fluids.WATER,Fluids.LAVA,ExNihiloOmnia.WITCHWATER))for(Item item:BuiltInRegistries.ITEM){var result=BarrelEntity.fluidRecipe(fluid,new ItemStack(item));if(!result.isEmpty())check(pages.stream().anyMatch(p->p.kind()==Kind.BARREL&&!p.inputs().isEmpty()&&p.inputs().getFirst().is(item)&&p.fluidInput().fluid()==fluid&&p.fluidInput().millibuckets()==1000&&p.output().is(result.getItem())),"shared barrel conversion indexed");}
  check(pages.stream().anyMatch(p->p.kind()==Kind.CROOK&&p.inputs().getFirst().is(Items.CHERRY_LEAVES)&&p.output().is(ExNihiloOmnia.ITEMS.get("infested_cherry_leaves"))),"cherry infestation indexed");
  check(pages.stream().anyMatch(p->p.kind()==Kind.BARREL&&p.output().is(Items.BLAZE_SPAWN_EGG)&&p.notes().contains("Mob preview, not a spawn egg")),"summoning preview explicitly labeled");
  return checks;
 }
}
