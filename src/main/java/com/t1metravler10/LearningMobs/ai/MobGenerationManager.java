package com.t1metravler10.LearningMobs.ai;

import com.t1metravler10.LearningMobs.Config;
import com.t1metravler10.LearningMobs.data.LearningMobsWorldData;
import com.t1metravler10.LearningMobs.data.MobLearningRecord;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class MobGenerationManager {
    private static final MobGenerationManager INSTANCE = new MobGenerationManager();
    private static final double DEFAULT_INIT_SPREAD = 0.5D;

    private final Map<ResourceLocation, MobSpec> mobSpecs = new HashMap<>();
    private final Map<ResourceLocation, MobPopulation> populations = new HashMap<>();
    private final Map<UUID, AssignedGenome> cohortAssignments = new HashMap<>();
    private final Map<UUID, AssignedGenome> creeperAssignments = new HashMap<>();
    private final Object2DoubleOpenHashMap<UUID> creeperFitness = new Object2DoubleOpenHashMap<>();

    private LearningMobsWorldData data;
    private ServerLevel overworld;
    private final Random random = new Random();

    private MobGenerationManager() {
        register(EntityType.CREEPER, new MobSpec(ForgeRegistries.ENTITY_TYPES.getKey(EntityType.CREEPER), 5, 2, DEFAULT_INIT_SPREAD));
    }

    public static MobGenerationManager getInstance() {
        return INSTANCE;
    }

    public static MobGenerationManager get(ServerLevel level) {
        MobGenerationManager manager = getInstance();
        manager.ensureInitialized(level);
        return manager;
    }

    private void register(EntityType<?> type, MobSpec spec) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id != null) {
            mobSpecs.put(id, spec);
        }
    }

    public void initialize(ServerLevel serverLevel) {
        ServerLevel target = serverLevel.getServer().getLevel(Level.OVERWORLD);
        if (target == null) {
            return;
        }
        if (Objects.equals(overworld, target) && data != null) {
            return;
        }
        overworld = target;
        data = LearningMobsWorldData.get(overworld);
        populations.clear();
        cohortAssignments.clear();
        creeperAssignments.clear();
        creeperFitness.clear();

        for (MobSpec spec : mobSpecs.values()) {
            MobLearningRecord record = data.getRecord(spec.id());
            record.ensureDimensions(spec.inputs(), spec.outputs());
            int populationSize = Math.max(8, Config.creeperPopulationSize);
            double sigma = Math.max(0.01D, Config.creeperMutationSigma);
            MobPopulation population = new MobPopulation(spec, populationSize, sigma, random.nextLong());
            double[][] storedPopulation = record.population();
            if (storedPopulation.length == populationSize && storedPopulation.length > 0
                && storedPopulation[0].length == spec.weightCount()) {
                population.loadFromRecord(storedPopulation, record.generation());
            } else if (record.hasNetwork()) {
                population.seedFromWeights(record.flattenWeights());
            } else {
                population.seedRandom(random);
            }
            record.setPopulation(population.exportPopulation());
            record.setGeneration(population.generation);
            if (population.genomes.isEmpty()) {
                double[] seed = new double[spec.weightCount()];
                population.genomes.add(new Genome(seed));
            }
            record.setFlattenedWeights(population.genomes.get(0).weights.clone(), spec.inputs(), spec.outputs());
            populations.put(spec.id(), population);
        }
        data.setDirty();
    }

    private void ensureInitialized(ServerLevel level) {
        if (data == null || overworld == null || overworld.getServer() != level.getServer()) {
            initialize(level);
        }
    }

    public EvolvingAIController assignToCohort(Creeper creeper, UUID cohortId) {
        if (!(creeper.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ensureInitialized(serverLevel);
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(creeper.getType());
        if (mobId == null) {
            return null;
        }
        MobPopulation population = populations.get(mobId);
        if (population == null) {
            return null;
        }
        AssignedGenome assignment = cohortAssignments.computeIfAbsent(cohortId, id -> population.reserveGenome(cohortId, random));
        assignment.incrementActive();
        creeperAssignments.put(creeper.getUUID(), assignment);
        return new EvolvingAIController(assignment);
    }

    public void release(Creeper creeper) {
        AssignedGenome assignment = creeperAssignments.remove(creeper.getUUID());
        creeperFitness.removeDouble(creeper.getUUID());
        if (assignment != null) {
            if (assignment.decrementActive() == 0) {
                cohortAssignments.remove(assignment.cohortId(), assignment);
            }
        }
    }

    public void addCreeperFitness(UUID id, double delta) {
        if (delta == 0.0D) {
            return;
        }
        creeperFitness.addTo(id, delta);
        AssignedGenome assignment = creeperAssignments.get(id);
        if (assignment != null) {
            assignment.addSessionFitness(delta);
        }
    }

    public double getCreeperFitness(UUID id) {
        return creeperFitness.getOrDefault(id, 0.0D);
    }

    public void clearCreeperFitnessForGen() {
        creeperFitness.clear();
    }

    public void rewardCreeperProximity(Creeper creeper) {
        if (!(creeper.level() instanceof ServerLevel level)) {
            return;
        }
        ensureInitialized(level);
        double maxDistance = Math.max(4.0D, Config.creeperActiveDistance);
        Player player = level.getNearestPlayer(creeper, maxDistance);
        if (player == null) {
            return;
        }
        double distance = creeper.distanceTo(player);
        if (distance > maxDistance) {
            return;
        }
        double closeness = 1.0D - (distance / maxDistance);
        double reward = closeness * Config.creeperProximityReward;
        if (reward > 0.0D) {
            addCreeperFitness(creeper.getUUID(), reward);
        }
    }

    public void rewardCreeperExplosionProximity(Creeper creeper) {
        if (!(creeper.level() instanceof ServerLevel level)) {
            return;
        }
        ensureInitialized(level);
        if (creeper.getPersistentData().getBoolean("learningmobs.rewarded_explosion").orElse(false)) {
            return;
        }
        double rewardRadius = Math.min(Config.creeperActiveDistance, 5.0D);
        Player player = level.getNearestPlayer(creeper, rewardRadius);
        if (player != null) {
            addCreeperFitness(creeper.getUUID(), Config.creeperExplosionReward);
        }
        creeper.getPersistentData().putBoolean("learningmobs.rewarded_explosion", true);
    }

    public void onLevelTick(ServerLevel level) {
        ensureInitialized(level);
        if (data == null || overworld == null || level != overworld) {
            return;
        }
        evaluateProximity(level);
        long currentDay = level.getDayTime() / 24000L;
        if (currentDay > data.getLastProcessedDay()) {
            finalizeGeneration();
            data.setLastProcessedDay(currentDay);
        }
    }

    private void evaluateProximity(ServerLevel level) {
        if (Config.creeperTickBudget <= 0) {
            return;
        }
        double range = Math.max(4.0D, Config.creeperActiveDistance);
        int budget = Math.max(1, Config.creeperTickBudget);
        int processed = 0;
        ObjectOpenHashSet<UUID> seen = new ObjectOpenHashSet<>();
        for (ServerPlayer player : level.players()) {
            if (processed >= budget) {
                break;
            }
            AABB box = player.getBoundingBox().inflate(range);
            List<Creeper> creepers = level.getEntitiesOfClass(Creeper.class, box, Creeper::isAlive);
            for (Creeper creeper : creepers) {
                if (processed >= budget) {
                    break;
                }
                if (seen.add(creeper.getUUID())) {
                    rewardCreeperProximity(creeper);
                    processed++;
                }
            }
        }
    }

    private void finalizeGeneration() {
        if (populations.isEmpty()) {
            return;
        }
        harvestSessionFitness();
        for (MobPopulation population : populations.values()) {
            population.ensureEvaluations(random);
            population.evolve(random);
            MobLearningRecord record = data.getRecord(population.spec().id());
            record.setFlattenedWeights(population.genomes.get(0).weights.clone(),
                population.spec().inputs(), population.spec().outputs());
            record.setPopulation(population.exportPopulation());
            record.setGeneration(population.generation);
            record.clearFitness();
        }
        data.setDirty();
        cohortAssignments.clear();
        creeperAssignments.clear();
        creeperFitness.clear();
    }

    private void harvestSessionFitness() {
        for (AssignedGenome assigned : cohortAssignments.values()) {
            if (assigned.sessionFitness() != 0.0D) {
                Genome genome = assigned.population().genomes.get(assigned.genomeIndex());
                genome.fitnessSum += assigned.sessionFitness();
                genome.evaluationCount++;
                assigned.resetSessionFitness();
            }
        }
    }

    public void reportFitness(LivingEntity entity, double fitness) {
        if (!(entity instanceof Creeper creeper) || fitness == 0.0D || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        ensureInitialized(level);
        addCreeperFitness(creeper.getUUID(), fitness);
    }

    private static final class MobSpec {
        private final ResourceLocation id;
        private final int inputs;
        private final int outputs;
        private final double initSpread;

        private MobSpec(ResourceLocation id, int inputs, int outputs, double initSpread) {
            this.id = id;
            this.inputs = inputs;
            this.outputs = outputs;
            this.initSpread = initSpread;
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

        private int weightCount() {
            return inputs * outputs;
        }
    }

    private static final class MobPopulation {
        private final MobSpec spec;
        private final int populationSize;
        private final double mutationSigma;
        private final List<Genome> genomes = new ArrayList<>();
        private final Random localRandom;
        private long generation = 0L;
        private int nextGenomeIndex = 0;

        private MobPopulation(MobSpec spec, int populationSize, double mutationSigma, long seed) {
            this.spec = spec;
            this.populationSize = populationSize;
            this.mutationSigma = mutationSigma;
            this.localRandom = new Random(seed);
        }

        private MobSpec spec() {
            return spec;
        }

        private void seedRandom(Random random) {
            genomes.clear();
            for (int i = 0; i < populationSize; i++) {
                double[] weights = new double[spec.weightCount()];
                for (int w = 0; w < weights.length; w++) {
                    weights[w] = randomSpread(random);
                }
                genomes.add(new Genome(weights));
            }
            generation = 0L;
            nextGenomeIndex = 0;
        }

        private void seedFromWeights(double[] weights) {
            genomes.clear();
            if (weights.length != spec.weightCount()) {
                throw new IllegalArgumentException("Weight length mismatch");
            }
            for (int i = 0; i < populationSize; i++) {
                double[] copy = weights.clone();
                mutate(copy, mutationSigma * 0.5D, localRandom);
                genomes.add(new Genome(copy));
            }
            generation = 0L;
            nextGenomeIndex = 0;
        }

        private void loadFromRecord(double[][] storedPopulation, long storedGeneration) {
            genomes.clear();
            for (double[] genomeWeights : storedPopulation) {
                if (genomeWeights.length == spec.weightCount()) {
                    genomes.add(new Genome(genomeWeights.clone()));
                }
            }
            if (genomes.isEmpty()) {
                seedRandom(localRandom);
            }
            generation = storedGeneration;
            nextGenomeIndex = 0;
        }

        private AssignedGenome reserveGenome(UUID cohortId, Random random) {
            if (genomes.isEmpty()) {
                seedRandom(random);
            }
            int index = nextGenomeIndex;
            nextGenomeIndex = (nextGenomeIndex + 1) % genomes.size();
            double[] weights = genomes.get(index).weights.clone();
            NeuralNetwork network = new NeuralNetwork(spec.inputs(), spec.outputs(), weights);
            return new AssignedGenome(cohortId, this, index, network);
        }

        private void ensureEvaluations(Random random) {
            boolean evaluated = false;
            for (Genome genome : genomes) {
                if (genome.evaluationCount > 0) {
                    evaluated = true;
                    break;
                }
            }
            if (!evaluated) {
                // No data this cycle, reseed slightly to keep search moving.
                for (Genome genome : genomes) {
                    for (int i = 0; i < genome.weights.length; i++) {
                        genome.weights[i] = randomSpread(random);
                    }
                    genome.resetStats();
                }
            }
        }

        private void evolve(Random random) {
            if (genomes.isEmpty()) {
                seedRandom(random);
                return;
            }
            genomes.sort((a, b) -> Double.compare(b.averageFitness(), a.averageFitness()));
            List<Genome> nextGen = new ArrayList<>(populationSize);
            int eliteCount = Math.max(1, populationSize / 8);
            for (int i = 0; i < eliteCount && i < genomes.size(); i++) {
                nextGen.add(genomes.get(i).copy());
            }
            while (nextGen.size() < populationSize) {
                Genome parentA = selectParent(random);
                Genome parentB = selectParent(random);
                double[] childWeights = crossover(parentA.weights, parentB.weights, random);
                mutate(childWeights, mutationSigma, random);
                nextGen.add(new Genome(childWeights));
            }
            genomes.clear();
            genomes.addAll(nextGen);
            for (Genome genome : genomes) {
                genome.resetStats();
            }
            generation++;
            nextGenomeIndex = 0;
        }

        private Genome selectParent(Random random) {
            Genome g1 = genomes.get(random.nextInt(genomes.size()));
            Genome g2 = genomes.get(random.nextInt(genomes.size()));
            return g1.averageFitness() >= g2.averageFitness() ? g1 : g2;
        }

        private double[] crossover(double[] a, double[] b, Random random) {
            double[] child = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                double weight = random.nextBoolean() ? a[i] : b[i];
                double blend = (a[i] + b[i]) * 0.5D;
                child[i] = clamp(weight * 0.7D + blend * 0.3D);
            }
            return child;
        }

        private void mutate(double[] weights, double sigma, Random random) {
            double mutationScale = Math.max(0.0001D, sigma);
            for (int i = 0; i < weights.length; i++) {
                double delta = random.nextGaussian() * mutationScale;
                weights[i] = clamp(weights[i] + delta);
            }
        }

        private double randomSpread(Random random) {
            return clamp((random.nextDouble() * 2.0D - 1.0D) * spec.initSpread);
        }

        private double[][] exportPopulation() {
            double[][] copy = new double[genomes.size()][];
            for (int i = 0; i < genomes.size(); i++) {
                copy[i] = genomes.get(i).weights.clone();
            }
            return copy;
        }
    }

    static final class AssignedGenome {
        private final UUID cohortId;
        private final MobPopulation population;
        private final int genomeIndex;
        private final NeuralNetwork network;
        private double sessionFitness = 0.0D;
        private int activeCreepers = 0;

        AssignedGenome(UUID cohortId, MobPopulation population, int genomeIndex, NeuralNetwork network) {
            this.cohortId = cohortId;
            this.population = population;
            this.genomeIndex = genomeIndex;
            this.network = network;
        }

        UUID cohortId() {
            return cohortId;
        }

        MobPopulation population() {
            return population;
        }

        int genomeIndex() {
            return genomeIndex;
        }

        NeuralNetwork network() {
            return network;
        }

        void addSessionFitness(double value) {
            sessionFitness += value;
        }

        double sessionFitness() {
            return sessionFitness;
        }

        void resetSessionFitness() {
            sessionFitness = 0.0D;
        }

        void incrementActive() {
            activeCreepers++;
        }

        int decrementActive() {
            activeCreepers = Math.max(0, activeCreepers - 1);
            return activeCreepers;
        }
    }

    private static final class Genome {
        private final double[] weights;
        private double fitnessSum = 0.0D;
        private int evaluationCount = 0;

        private Genome(double[] weights) {
            this.weights = weights;
        }

        private double averageFitness() {
            if (evaluationCount == 0) {
                return -1.0D;
            }
            return fitnessSum / evaluationCount;
        }

        private Genome copy() {
            Genome copy = new Genome(weights.clone());
            return copy;
        }

        private void resetStats() {
            fitnessSum = 0.0D;
            evaluationCount = 0;
        }
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        if (value > 8.0D) {
            return 8.0D;
        }
        if (value < -8.0D) {
            return -8.0D;
        }
        return value;
    }
}
