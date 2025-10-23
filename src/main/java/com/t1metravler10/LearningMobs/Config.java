package com.t1metravler10.LearningMobs;

import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = LearningMobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK;
    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER;
    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS;

    private static final ForgeConfigSpec.IntValue CREEPER_POPULATION_SIZE;
    private static final ForgeConfigSpec.DoubleValue CREEPER_MUTATION_SIGMA;
    private static final ForgeConfigSpec.IntValue CREEPER_TICK_BUDGET;
    private static final ForgeConfigSpec.DoubleValue CREEPER_ACTIVE_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue CREEPER_DAMAGE_REWARD_SCALE;
    private static final ForgeConfigSpec.DoubleValue CREEPER_PROXIMITY_REWARD;
    private static final ForgeConfigSpec.DoubleValue CREEPER_EXPLOSION_REWARD;

    static {
        LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

        MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

        MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

        ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

        BUILDER.comment("Evolutionary AI settings").push("evolution");
        CREEPER_POPULATION_SIZE = BUILDER
            .comment("Population size for creeper neural networks.")
            .defineInRange("creeperPopulation", 32, 8, 512);
        CREEPER_MUTATION_SIGMA = BUILDER
            .comment("Mutation standard deviation for creeper genomes.")
            .defineInRange("creeperMutationSigma", 0.35D, 0.01D, 5.0D);
        CREEPER_TICK_BUDGET = BUILDER
            .comment("Maximum creepers evaluated per tick for proximity rewards.")
            .defineInRange("creeperTickBudget", 32, 1, 512);
        CREEPER_ACTIVE_DISTANCE = BUILDER
            .comment("Maximum distance (blocks) within which creepers are considered active for proximity rewards.")
            .defineInRange("creeperActiveDistance", 24.0D, 4.0D, 128.0D);
        CREEPER_DAMAGE_REWARD_SCALE = BUILDER
            .comment("Multiplier applied to damage dealt to players when rewarding creeper fitness.")
            .defineInRange("creeperDamageRewardScale", 10.0D, 0.1D, 200.0D);
        CREEPER_PROXIMITY_REWARD = BUILDER
            .comment("Per-tick reward scale for creeper proximity to players (after normalization).")
            .defineInRange("creeperProximityRewardPerTick", 0.05D, 0.0D, 5.0D);
        CREEPER_EXPLOSION_REWARD = BUILDER
            .comment("Bonus reward when a creeper detonates near a player.")
            .defineInRange("creeperExplosionReward", 15.0D, 0.0D, 200.0D);
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;
    public static int creeperPopulationSize;
    public static double creeperMutationSigma;
    public static int creeperTickBudget;
    public static double creeperActiveDistance;
    public static double creeperDamageRewardScale;
    public static double creeperProximityReward;
    public static double creeperExplosionReward;

    private static boolean validateItemName(final Object obj) {
        if (!(obj instanceof String itemName)) {
            return false;
        }
        ResourceLocation id = parseResourceLocation(itemName);
        return id != null && ForgeRegistries.ITEMS.containsKey(id);
    }

    private static ResourceLocation parseResourceLocation(String name) {
        String namespace;
        String path;
        int idx = name.indexOf(':');
        if (idx >= 0) {
            namespace = name.substring(0, idx);
            path = name.substring(idx + 1);
        } else {
            namespace = ResourceLocation.DEFAULT_NAMESPACE;
            path = name;
        }
        if (path.isEmpty()) {
            return null;
        }
        try {
            return ResourceLocation.fromNamespaceAndPath(namespace, path);
        } catch (ResourceLocationException e) {
            return null;
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        items = ITEM_STRINGS.get().stream()
            .map(Config::parseResourceLocation)
            .filter(Objects::nonNull)
            .map(ForgeRegistries.ITEMS::getValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        creeperPopulationSize = CREEPER_POPULATION_SIZE.get();
        creeperMutationSigma = CREEPER_MUTATION_SIGMA.get();
        creeperTickBudget = CREEPER_TICK_BUDGET.get();
        creeperActiveDistance = CREEPER_ACTIVE_DISTANCE.get();
        creeperDamageRewardScale = CREEPER_DAMAGE_REWARD_SCALE.get();
        creeperProximityReward = CREEPER_PROXIMITY_REWARD.get();
        creeperExplosionReward = CREEPER_EXPLOSION_REWARD.get();
    }
}
