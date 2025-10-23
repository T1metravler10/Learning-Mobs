package com.t1metravler10.LearningMobs.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.ChunkPos;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreeperSquadManager {
    private static final CreeperSquadManager INSTANCE = new CreeperSquadManager();

    private final Map<UUID, EvolvingAIController> cohortControllers = new HashMap<>();
    private final Map<UUID, UUID> creeperToCohort = new HashMap<>();
    private final Map<UUID, Integer> cohortCounts = new HashMap<>();
    private long cachedTick = Long.MIN_VALUE;
    private final Map<Long, UUID> tickCohorts = new HashMap<>();

    public static CreeperSquadManager getInstance() {
        return INSTANCE;
    }

    public void register(Creeper creeper) {
        if (creeperToCohort.containsKey(creeper.getUUID())) {
            return;
        }
        if (!(creeper.level() instanceof ServerLevel level)) {
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime != cachedTick) {
            tickCohorts.clear();
            cachedTick = gameTime;
        }
        ChunkPos chunkPos = new ChunkPos(creeper.blockPosition());
        long chunkKey = ChunkPos.asLong(chunkPos.x, chunkPos.z);
        UUID cohortId = tickCohorts.computeIfAbsent(chunkKey, key -> cohortUuid(level, gameTime, chunkPos));

        MobGenerationManager manager = MobGenerationManager.get(level);
        EvolvingAIController controller = manager.assignToCohort(creeper, cohortId);
        if (controller == null) {
            return;
        }

        cohortControllers.put(cohortId, controller);
        creeperToCohort.put(creeper.getUUID(), cohortId);
        cohortCounts.merge(cohortId, 1, Integer::sum);
    }

    public void unregister(Creeper creeper) {
        UUID cohortId = creeperToCohort.remove(creeper.getUUID());
        if (cohortId == null) {
            return;
        }
        if (creeper.level() instanceof ServerLevel level) {
            MobGenerationManager.get(level).release(creeper);
        }
        cohortCounts.computeIfPresent(cohortId, (id, count) -> {
            int remaining = count - 1;
            return remaining > 0 ? remaining : null;
        });
        if (!cohortCounts.containsKey(cohortId)) {
            cohortControllers.remove(cohortId);
        }
    }

    public EvolvingAIController getController(Creeper creeper) {
        UUID cohortId = creeperToCohort.get(creeper.getUUID());
        if (cohortId == null) {
            register(creeper);
            cohortId = creeperToCohort.get(creeper.getUUID());
        }
        return cohortId == null ? null : cohortControllers.get(cohortId);
    }

    private UUID cohortUuid(ServerLevel level, long tick, ChunkPos chunk) {
        String seed = level.dimension().location() + ":" + tick + ":" + chunk.x + ":" + chunk.z;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
