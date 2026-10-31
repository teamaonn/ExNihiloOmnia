package exnihiloomnia.fabric.jei;

import exnihiloomnia.fabric.*;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.fabricmc.api.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import static exnihiloomnia.fabric.jei.ProcessingRecipes.*;

/** Only JEI requests this entrypoint. Ordinary client/server initialization never does. */
@Environment(EnvType.CLIENT)
@JeiPlugin
public final class ExNihiloJeiPlugin implements IModPlugin {
    public static final Map<Kind,RecipeType<Page>> TYPES;
    static {
        Map<Kind,RecipeType<Page>> types=new EnumMap<>(Kind.class);
        for(Kind kind:Kind.values())types.put(kind,RecipeType.create(ExNihiloOmnia.MOD_ID,kind.name().toLowerCase(Locale.ROOT),Page.class));
        TYPES=Collections.unmodifiableMap(types);
    }
    @Override public Identifier getPluginUid() { return ExNihiloOmnia.id("jei_processing"); }
    private static String icon(Kind kind) {
        return switch(kind) {
            case SIEVING -> "sieve_wood"; case HAMMERING -> "hammer_wood";
            case COMPOSTING,BARREL -> "barrel_wood"; case CRUCIBLE,HEAT -> "crucible"; case CROOK -> "crook_wood";
        };
    }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        for(Kind kind:Kind.values())registration.addRecipeCategories(new Category(kind,
                registration.getJeiHelpers().getGuiHelper().createDrawableItemLike(ExNihiloOmnia.ITEMS.get(icon(kind)))));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        var all=ProcessingRecipes.all();
        for(Kind kind:Kind.values())registration.addRecipes(TYPES.get(kind),all.stream().filter(page->page.kind()==kind).toList());
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for(var entry:ExNihiloOmnia.ITEMS.entrySet()) {
            String id=entry.getKey();ItemStack stack=new ItemStack(entry.getValue());
            if(id.equals("sieve_wood")||id.equals("heavy_sieve"))registration.addRecipeCatalyst(stack,TYPES.get(Kind.SIEVING));
            if(entry.getValue() instanceof ExNihiloOmnia.HammerItem)registration.addRecipeCatalyst(stack,TYPES.get(Kind.HAMMERING));
            if(entry.getValue() instanceof CrookItem)registration.addRecipeCatalyst(stack,TYPES.get(Kind.CROOK));
            if(ExNihiloOmnia.BARRELS.containsKey(id))registration.addRecipeCatalyst(stack,TYPES.get(Kind.COMPOSTING),TYPES.get(Kind.BARREL));
            if(id.equals("crucible"))registration.addRecipeCatalyst(stack,TYPES.get(Kind.CRUCIBLE),TYPES.get(Kind.HEAT));
        }
    }
    static final class Category implements IRecipeCategory<Page> {
        private final Kind kind;private final IDrawable icon;
        Category(Kind kind,IDrawable icon){this.kind=kind;this.icon=icon;}
        @Override public RecipeType<Page> getRecipeType(){return TYPES.get(kind);}
        @Override public Component getTitle(){return Component.translatable("jei.exnihiloomnia."+kind.name().toLowerCase(Locale.ROOT));}
        @Override public IDrawable getIcon(){return icon;}
        @Override public int getWidth(){return 186;}
        @Override public int getHeight(){return 103;}
        @Override public void setRecipe(IRecipeLayoutBuilder builder,Page page,IFocusGroup focus) {
            for(int i=0;i<page.inputs().size();i++)builder.addInputSlot(4+i*22,6).addItemStack(page.inputs().get(i));
            if(page.fluidInput()!=null)builder.addInputSlot(72,6).addFluidStack(page.fluidInput().fluid(),units(page.fluidInput()));
            for(int i=0;i<page.tools().size();i++) {
                // Tools are alternatives, except sieving which requires both the listed mesh and sieve.
                if(kind==Kind.SIEVING)builder.addSlot(RecipeIngredientRole.CRAFTING_STATION,4+i*22,30).addItemStack(page.tools().get(i));
            }
            if(!page.tools().isEmpty()&&kind!=Kind.SIEVING)builder.addSlot(RecipeIngredientRole.CRAFTING_STATION,4,30).addItemStacks(page.tools());
            if(!page.output().isEmpty())builder.addOutputSlot(162,6).addItemStack(page.output());
            if(page.fluidOutput()!=null)builder.addOutputSlot(162,6).addFluidStack(page.fluidOutput().fluid(),units(page.fluidOutput()));
        }
        private static long units(FluidAmount fluid){return FluidConstants.BUCKET*fluid.millibuckets()/1000;}
        @Override public void draw(Page page,IRecipeSlotsView slots,GuiGraphicsExtractor graphics,double mouseX,double mouseY) {
            var font=Minecraft.getInstance().font;
            graphics.text(font,"->",126,10,0xFF404040,false);
            if(page.fluidInput()!=null)graphics.text(font,page.fluidInput().millibuckets()+" mB",94,32,0xFF404040,false);
            for(int i=0;i<page.notes().size();i++)graphics.text(font,page.notes().get(i),4,50+i*12,0xFF404040,false);
        }
    }
}
