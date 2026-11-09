package com.kaduvill.asjeialldrops.config;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import net.minecraftforge.common.config.Config;

/** Client startup switches for this addon's JEI categories and corrupted pages. */
@Config(modid = ASJeiAllDrops.MOD_ID, name = ASJeiAllDrops.MOD_ID, category = "jei")
public final class ASJeiAllDropsConfig {
    @Config.Name("Mineralis")
    @Config.Comment("Show the Mineralis Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean mineralis = true;

    @Config.Name("MineralisCorrupted")
    @Config.Comment("Show corrupted pages. Requires Mineralis=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean mineralisCorrupted = true;

    @Config.Name("Pelotrio")
    @Config.Comment("Show the Pelotrio Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean pelotrio = true;

    @Config.Name("PelotrioCorrupted")
    @Config.Comment("Show corrupted pages. Requires Pelotrio=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean pelotrioCorrupted = true;

    @Config.Name("Octans")
    @Config.Comment("Show the Octans Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean octans = true;

    @Config.Name("OctansCorrupted")
    @Config.Comment("Show corrupted pages. Requires Octans=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean octansCorrupted = true;

    @Config.Name("Bootes")
    @Config.Comment("Show the Bootes Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean bootes = true;

    @Config.Name("BootesCorrupted")
    @Config.Comment("Show corrupted pages. Requires Bootes=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean bootesCorrupted = true;

    @Config.Name("ContainmentChalice")
    @Config.Comment("Show the Containment Chalice reactions category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean containmentChalice = true;

    @Config.Name("LiquidStarlightInteractions")
    @Config.Comment("Show the Liquid Starlight Interactions category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean liquidStarlightInteractions = true;

    @Config.Name("CrystalClusterGrowth")
    @Config.Comment("Show the Crystal Cluster Growth category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean crystalClusterGrowth = true;

    @Config.Name("NeromanticPrime")
    @Config.Comment("Show the Neromantic Prime fluids category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean neromanticPrime = true;

    @Config.Name("StoneEnrichment")
    @Config.Comment("Show the Stone Enrichment category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean stoneEnrichment = true;

    @Config.Name("TrashToTreasure")
    @Config.Comment("Show the Trash to Treasure category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean trashToTreasure = true;

    @Config.Name("TreasureShrine")
    @Config.Comment("Show the Treasure Shrine category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean treasureShrine = true;

    private ASJeiAllDropsConfig() {}
}
