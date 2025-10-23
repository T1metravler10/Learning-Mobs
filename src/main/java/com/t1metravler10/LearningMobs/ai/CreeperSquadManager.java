package com.t1metravler10.LearningMobs.ai;

import net.minecraft.world.entity.monster.Creeper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreeperSquadManager {
    private static final CreeperSquadManager INSTANCE = new CreeperSquadManager();
    private final Map<UUID, EvolvingAIController> squadControllers = new HashMap<>();
    private final Map<UUID, UUID> creeperToSquad = new HashMap<>();

    public static CreeperSquadManager getInstance() {
        return INSTANCE;
    }

    public void register(Creeper creeper) {
        EvolvingAIController controller = MobGenerationManager.getInstance().assignToMob(creeper);
        if (controller == null) {
            return;
        }

        UUID squadId = UUID.randomUUID();
        squadControllers.put(squadId, controller);
        creeperToSquad.put(creeper.getUUID(), squadId);
    }

    public EvolvingAIController getController(Creeper creeper) {
        UUID squadId = creeperToSquad.get(creeper.getUUID());
        return squadControllers.getOrDefault(squadId, MobGenerationManager.getInstance().assignToMob(creeper));
    }
}
