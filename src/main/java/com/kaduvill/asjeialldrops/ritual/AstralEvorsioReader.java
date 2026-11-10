package com.kaduvill.asjeialldrops.ritual;

import com.kaduvill.asjeialldrops.ore.AstralOreTableReader;
import com.kaduvill.asjeialldrops.ore.OreTable;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectEvorsio;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

/** Read-only snapshot of corrupted Evorsio's block placement, never executing its random selector. */
public final class AstralEvorsioReader {
    private static final double ORE_ATTEMPT = 1.0 / 20;

    public Snapshot read() throws ReflectiveOperationException {
        if (!CEffectEvorsio.enabled) return null;
        AstralOreTableReader reader = new AstralOreTableReader();
        boolean empty = reader.isEmpty("RITUAL_MINERALIS");
        OreTable ores = reader.readUniform("RITUAL_MINERALIS", true);
        OreTable.Builder outputs = new OreTable.Builder();
        // A nonempty table's exhausted or unplaceable result retains the initial dirt/stone choice.
        // With no rows AS instead throws from nextInt(0), before it can place that base choice.
        double baseChance = empty ? (1 - ORE_ATTEMPT) / 2
                : (1 - ORE_ATTEMPT + ORE_ATTEMPT * ores.noResult) / 2;
        outputs.add(new ItemStack(Blocks.DIRT), baseChance);
        outputs.add(new ItemStack(Blocks.STONE), baseChance);
        for (OreTable.Output output : ores.outputs) outputs.add(output.getStack(), ORE_ATTEMPT * output.probability);
        return new Snapshot(CEffectEvorsio.searchRange, outputs.build(empty ? ORE_ATTEMPT : 0), empty);
    }

    public static final class Snapshot {
        public final double range;
        public final OreTable table;
        public final boolean emptyOreTable;

        private Snapshot(double range, OreTable table, boolean emptyOreTable) {
            this.range = range;
            this.table = table;
            this.emptyOreTable = emptyOreTable;
        }
    }
}
