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

    @Config.Name("Fornax")
    @Config.Comment("Show the Fornax Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean fornax = true;

    @Config.Name("FornaxCorrupted")
    @Config.Comment("Show corrupted pages. Requires Fornax=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean fornaxCorrupted = true;

    @Config.Name("Evorsio")
    @Config.Comment("Show the Evorsio Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean evorsio = true;

    @Config.Name("EvorsioCorrupted")
    @Config.Comment("Show corrupted pages. Requires Evorsio=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean evorsioCorrupted = true;

    @Config.Name("Aevitas")
    @Config.Comment("Show the Aevitas Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean aevitas = true;

    @Config.Name("AevitasCorrupted")
    @Config.Comment("Show corrupted pages. Requires Aevitas=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean aevitasCorrupted = true;

    @Config.Name("Armara")
    @Config.Comment("Show the Armara Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean armara = true;

    @Config.Name("ArmaraCorrupted")
    @Config.Comment("Show corrupted pages. Requires Armara=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean armaraCorrupted = true;

    @Config.Name("Discidia")
    @Config.Comment("Show the Discidia Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean discidia = true;

    @Config.Name("DiscidiaCorrupted")
    @Config.Comment("Show corrupted pages. Requires Discidia=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean discidiaCorrupted = true;

    @Config.Name("Horologium")
    @Config.Comment("Show the Horologium Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean horologium = true;

    @Config.Name("HorologiumCorrupted")
    @Config.Comment("Show corrupted pages. Requires Horologium=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean horologiumCorrupted = true;

    @Config.Name("Lucerna")
    @Config.Comment("Show the Lucerna Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean lucerna = true;

    @Config.Name("LucernaCorrupted")
    @Config.Comment("Show corrupted pages. Requires Lucerna=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean lucernaCorrupted = true;

    @Config.Name("Vicio")
    @Config.Comment("Show the Vicio Ritual category. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean vicio = true;

    @Config.Name("VicioCorrupted")
    @Config.Comment("Show corrupted pages. Requires Vicio=true; normal pages are unaffected. Client restart required.")
    @Config.RequiresMcRestart
    public static boolean vicioCorrupted = true;

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
