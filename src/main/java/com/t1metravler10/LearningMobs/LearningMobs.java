package com.t1metravler10.LearningMobs;

import com.t1metravler10.LearningMobs.ai.CreeperAIGoal;
import com.t1metravler10.LearningMobs.ai.CreeperSquadManager;
import com.t1metravler10.LearningMobs.ai.MobGenerationManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LearningMobs.MODID)
public class LearningMobs {
    public static final String MODID = "learningmobs";
    private static final Logger LOGGER = LogUtils.getLogger();

    public LearningMobs(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            MobGenerationManager.getInstance().initialize(serverLevel);
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.level instanceof net.minecraft.server.level.ServerLevel serverLevel && event.phase == TickEvent.Phase.END) {
            MobGenerationManager.getInstance().handleLevelTick(serverLevel);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        CreeperSquadManager.getInstance().register(creeper);
        boolean hasGoal = creeper.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapper -> wrapper.getGoal() instanceof CreeperAIGoal);
        if (!hasGoal) {
            creeper.goalSelector.addGoal(2, new CreeperAIGoal(creeper));
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
