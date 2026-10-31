package exnihiloomnia.fabric;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class JeiGameTests {
    @GameTest
    public void processingCatalogMatchesGameplay(GameTestHelper helper) {
        System.out.println("EXNIHILO JEI RECIPE CHECKS: " + JeiRecipeChecks.verify());
        helper.succeed();
    }
}
