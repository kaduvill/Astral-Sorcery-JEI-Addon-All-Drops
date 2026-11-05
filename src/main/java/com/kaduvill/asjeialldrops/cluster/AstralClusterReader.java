package com.kaduvill.asjeialldrops.cluster;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import com.kaduvill.asjeialldrops.cluster.ClusterRecipes.Condition;
import com.kaduvill.asjeialldrops.cluster.ClusterRecipes.Output;
import com.kaduvill.asjeialldrops.cluster.ClusterRecipes.Recipe;
import com.kaduvill.asjeialldrops.cluster.ClusterRecipes.Stage;
import hellfirepvp.astralsorcery.common.block.BlockCelestialCrystals;
import hellfirepvp.astralsorcery.common.block.BlockCustomOre.OreType;
import hellfirepvp.astralsorcery.common.block.BlockGemCrystals;
import hellfirepvp.astralsorcery.common.block.BlockGemCrystals.GrowthStageType;
import hellfirepvp.astralsorcery.common.item.ItemCraftingComponent.MetaType;
import hellfirepvp.astralsorcery.common.item.gem.ItemPerkGem.GemType;
import hellfirepvp.astralsorcery.common.lib.BlocksAS;
import hellfirepvp.astralsorcery.common.lib.ItemsAS;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;

/** Snapshots installed block/item forms and the inspected AS 1.10.27 harvest rules without RNG or a world. */
public final class AstralClusterReader {
    public List<Recipe> read() {
        List<Recipe> result = new ArrayList<>(5);
        try {
            if (BlocksAS.gemCrystals != null && ItemsAS.perkGem != null) {
                addGem(result, Condition.DAYLIGHT, GrowthStageType.STAGE_2_DAY, GemType.DAY);
                addGem(result, Condition.TWILIGHT, GrowthStageType.STAGE_2_SKY, GemType.SKY);
                addGem(result, Condition.NIGHT, GrowthStageType.STAGE_2_NIGHT, GemType.NIGHT);
            }
        } catch (RuntimeException | LinkageError failure) {
            ASJeiAllDrops.LOGGER.warn("Cannot describe AS Gem Crystal cluster growth", failure);
        }
        try {
            if (BlocksAS.celestialCrystals != null && ItemsAS.celestialCrystal != null
                    && ItemsAS.craftingComponent != null) addCelestial(result);
        } catch (RuntimeException | LinkageError failure) {
            ASJeiAllDrops.LOGGER.warn("Cannot describe AS Celestial Crystal cluster growth", failure);
        }
        return Collections.unmodifiableList(result);
    }

    private static void addGem(List<Recipe> result, Condition condition, GrowthStageType mature, GemType gem) {
        List<Stage> stages = new ArrayList<>(3);
        for (GrowthStageType stage : BlockGemCrystals.STAGE.getAllowedValues()) {
            if (stage.getGrowthStage() < mature.getGrowthStage() || stage == mature) {
                stages.add(new Stage(block(BlocksAS.gemCrystals.getDefaultState()
                        .withProperty(BlockGemCrystals.STAGE, stage)), stage.getGrowthStage(), mature.getGrowthStage()));
            }
        }
        result.add(new Recipe(condition, stages, Collections.emptyList(),
                Collections.singletonList(new Output(gem.asStack(), 0, Collections.emptyList()))));
    }

    private static void addCelestial(List<Recipe> result) {
        List<Stage> stages = new ArrayList<>();
        int maximum = Collections.max(BlockCelestialCrystals.STAGE.getAllowedValues());
        for (Integer stage : BlockCelestialCrystals.STAGE.getAllowedValues()) {
            stages.add(new Stage(block(BlocksAS.celestialCrystals.getDefaultState()
                    .withProperty(BlockCelestialCrystals.STAGE, stage)), stage, maximum));
        }
        // Bare item forms cover all generated properties and never execute AS's random factories.
        ItemStack crystal = new ItemStack(ItemsAS.celestialCrystal);
        Output dust = new Output(MetaType.STARDUST.asStack(), 0.5, Collections.singletonList("fortune_dust"));
        result.add(new Recipe(Condition.ORDINARY, stages, Collections.emptyList(), Arrays.asList(
                new Output(crystal, 0, Collections.singletonList("random_properties")), dust)));
        if (BlocksAS.customOre != null) {
            ItemStack starmetal = OreType.STARMETAL.asStack();
            if (!starmetal.isEmpty()) result.add(new Recipe(Condition.STARMETAL,
                    stages, Collections.singletonList(starmetal), Arrays.asList(
                            new Output(crystal, 1.0 / 3, Collections.singletonList("random_properties")), dust)));
        }
    }

    private static ItemStack block(IBlockState state) {
        Block block = state.getBlock();
        return new ItemStack(block, 1, block.getMetaFromState(state));
    }
}
