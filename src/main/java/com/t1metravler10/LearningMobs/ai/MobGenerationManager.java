package com.t1metravler10.LearningMobs.ai;

import com.t1metravler10.LearningMobs.data.LearningMobsWorldData;
import com.t1metravler10.LearningMobs.data.MobLearningRecord;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MobGenerationManager {
    private static final MobGenerationManager INSTANCE = new MobGenerationManager();

    private final Map<ResourceLocation, MobSpec> mobSpecs = new HashMap<>();
    private final Map<ResourceLocation, NeuralNetwork> networks = new HashMap<>();

    private LearningMobsWorldData data;
    private ServerLevel overworld;

    private MobGenerationManager() {
        register(EntityType.CREEPER, 5, 3);
        register(EntityType.ZOMBIE, 7, 6);
    }

    public static MobGenerationManager getInstance() {
        return INSTANCE;
    }

    private void register(EntityType<?> type, int inputs, int outputs) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id != null) {
            mobSpecs.put(id, new MobSpec(id, inputs, outputs));
        }
    }

    public void initialize(ServerLevel serverLevel) {
        ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        if (Objects.equals(this.overworld, overworld) && data != null) {
            return;
        }
        this.overworld = overworld;
        this.data = LearningMobsWorldData.get(overworld);
        this.networks.clear();

        for (MobSpec spec : mobSpecs.values()) {
            MobLearningRecord record = data.getRecord(spec.id());
            if (!record.hasNetwork()) {
                NeuralNetwork network = new NeuralNetwork(spec.inputs(), spec.outputs());
                record.setWeights(network.getWeights());
                data.setDirty();
                networks.put(spec.id(), network);
            } else {
                NeuralNetwork network = new NeuralNetwork(spec.inputs(), spec.outputs());
                network.setWeights(record.weightsMatrix());
                networks.put(spec.id(), network);
            }
        }
    }

    private NeuralNetwork getNetwork(ResourceLocation mobId, MobSpec spec) {
        return networks.computeIfAbsent(mobId, id -> {
            NeuralNetwork network = new NeuralNetwork(spec.inputs(), spec.outputs());
            if (data != null) {
                MobLearningRecord record = data.getRecord(id);
                record.setWeights(network.getWeights());
                data.setDirty();
            }
            return network;
        });
    }

    public EvolvingAIController assignToMob(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ensureInitialized(serverLevel);
        if (data == null) {
            return null;
        }
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (mobId == null) {
            return null;
        }
        MobSpec spec = mobSpecs.get(mobId);
        if (spec == null) {
            return null;
        }
        NeuralNetwork network = getNetwork(mobId, spec);
        return new EvolvingAIController(network);
    }

    public void reportFitness(LivingEntity entity, double fitness) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ensureInitialized(serverLevel);
        if (data == null) {
            return;
        }
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (mobId == null) {
            return;
        }
        MobSpec spec = mobSpecs.get(mobId);
        if (spec == null) {
            return;
        }
        MobLearningRecord record = data.getRecord(mobId);
        record.ensureDimensions(spec.inputs(), spec.outputs());
        record.fitnessSamples().add(fitness);
        data.setDirty();
    }

    public void handleLevelTick(ServerLevel level) {
        ensureInitialized(level);
        if (data == null || overworld == null || level != overworld) {
            return;
        }
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay > data.getLastProcessedDay()) {
            evolveAll();
            data.setLastProcessedDay(currentDay);
        }
    }

    private void evolveAll() {
        if (data == null) {
            return;
        }
        for (MobSpec spec : mobSpecs.values()) {
            NeuralNetwork network = networks.get(spec.id());
            if (network == null) {
                continue;
            }
            MobLearningRecord record = data.getRecord(spec.id());
            List<Double> scores = record.fitnessSamples();
            double average = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            NeuralNetwork evolved = GeneticAlgorithm.evolve(network, average);
            network.setWeights(evolved.getWeights());
            record.setWeights(network.getWeights());
            record.clearFitness();
        }
        data.setDirty();
    }

    private void ensureInitialized(ServerLevel level) {
        if (data == null || overworld == null || overworld.getServer() != level.getServer()) {
            initialize(level);
        }
    }

    private static final class MobSpec {
        private final ResourceLocation id;
        private final int inputs;
        private final int outputs;

        private MobSpec(ResourceLocation id, int inputs, int outputs) {
            this.id = id;
            this.inputs = inputs;
            this.outputs = outputs;
        }

        private ResourceLocation id() {
            return id;
        }

        private int inputs() {
            return inputs;
        }

        private int outputs() {
            return outputs;
        }
    }
}
