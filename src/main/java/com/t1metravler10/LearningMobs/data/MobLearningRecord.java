package com.t1metravler10.LearningMobs.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class MobLearningRecord {
    private static final double DEFAULT_WEIGHT = 0.0;
    private static final String VALUE_KEY = "v";

    private int inputSize;
    private int outputSize;
    private double[] weights = new double[0];
    private final List<Double> fitnessSamples = new ArrayList<>();
    private double[][] population = new double[0][];
    private long generation = 0L;

    public int inputSize() {
        return inputSize;
    }

    public int outputSize() {
        return outputSize;
    }

    public boolean hasNetwork() {
        return inputSize > 0 && outputSize > 0 && weights.length == inputSize * outputSize;
    }

    public void ensureDimensions(int inputs, int outputs) {
        int expected = Math.max(0, inputs) * Math.max(0, outputs);
        if (inputSize == inputs && outputSize == outputs && weights.length == expected) {
            return;
        }
        if (expected <= 0) {
            inputSize = 0;
            outputSize = 0;
            weights = new double[0];
            return;
        }
        inputSize = inputs;
        outputSize = outputs;
        weights = new double[expected];
        population = new double[0][];
    }

    public void randomizeWeights(Random random, double spread) {
        Objects.requireNonNull(random, "random");
        if (!hasNetwork()) {
            return;
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] = clamp(spread * (random.nextDouble() * 2 - 1));
        }
    }

    public double[][] weightsMatrix() {
        double[][] matrix = new double[outputSize][inputSize];
        for (int o = 0; o < outputSize; o++) {
            for (int i = 0; i < inputSize; i++) {
                matrix[o][i] = weights[o * inputSize + i];
            }
        }
        return matrix;
    }

    public void setWeights(double[][] matrix) {
        Objects.requireNonNull(matrix, "matrix");
        int outs = matrix.length;
        int ins = outs == 0 ? 0 : matrix[0].length;
        ensureDimensions(ins, outs);
        for (int o = 0; o < outs; o++) {
            if (matrix[o].length != ins) {
                throw new IllegalArgumentException("Non-rectangular weight matrix");
            }
            for (int i = 0; i < ins; i++) {
                weights[o * ins + i] = sanitizeWeight(matrix[o][i]);
            }
        }
    }

    public double[] flattenWeights() {
        return weights.clone();
    }

    public void setFlattenedWeights(double[] flat, int inputs, int outputs) {
        Objects.requireNonNull(flat, "flat");
        int expected = Math.max(0, inputs) * Math.max(0, outputs);
        ensureDimensions(inputs, outputs);
        if (expected == 0) {
            return;
        }
        if (flat.length != expected) {
            throw new IllegalArgumentException("Expected " + expected + " weights, got " + flat.length);
        }
        for (int i = 0; i < expected; i++) {
            weights[i] = sanitizeWeight(flat[i]);
        }
    }

    public List<Double> fitnessSamples() {
        return fitnessSamples;
    }

    public void clearFitness() {
        fitnessSamples.clear();
    }

    public double[][] population() {
        double[][] copy = new double[population.length][];
        for (int i = 0; i < population.length; i++) {
            copy[i] = population[i].clone();
        }
        return copy;
    }

    public void setPopulation(double[][] genomes) {
        Objects.requireNonNull(genomes, "genomes");
        population = new double[genomes.length][];
        for (int i = 0; i < genomes.length; i++) {
            double[] genome = genomes[i];
            if (genome == null) {
                population[i] = new double[weights.length];
                continue;
            }
            if (weights.length > 0 && genome.length != weights.length) {
                throw new IllegalArgumentException("Genome length mismatch");
            }
            double[] copy = new double[genome.length];
            for (int j = 0; j < genome.length; j++) {
                copy[j] = sanitizeWeight(genome[j]);
            }
            population[i] = copy;
        }
    }

    public long generation() {
        return generation;
    }

    public void setGeneration(long generation) {
        this.generation = Math.max(generation, 0L);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("inputs", inputSize);
        tag.putInt("outputs", outputSize);

        ListTag weightList = new ListTag();
        for (double weight : weights) {
            CompoundTag entry = new CompoundTag();
            entry.putDouble(VALUE_KEY, sanitizeWeight(weight));
            weightList.add(entry);
        }
        tag.put("weights", weightList);

        ListTag fitnessTag = new ListTag();
        for (Double value : fitnessSamples) {
            if (value != null) {
                CompoundTag entry = new CompoundTag();
                entry.putDouble(VALUE_KEY, sanitizeWeight(value));
                fitnessTag.add(entry);
            }
        }
        tag.put("fitness", fitnessTag);

        ListTag populationTag = new ListTag();
        if (population.length > 0 && weights.length > 0) {
            for (double[] genome : population) {
                if (genome == null || genome.length != weights.length) {
                    continue;
                }
                ListTag genomeWeights = new ListTag();
                for (double value : genome) {
                    CompoundTag weightEntry = new CompoundTag();
                    weightEntry.putDouble(VALUE_KEY, sanitizeWeight(value));
                    genomeWeights.add(weightEntry);
                }
                CompoundTag genomeTag = new CompoundTag();
                genomeTag.put("weights", genomeWeights);
                populationTag.add(genomeTag);
            }
        }
        if (populationTag.size() > 0) {
            tag.put("population", populationTag);
        }
        tag.putLong("generation", generation);
        return tag;
    }

    public static MobLearningRecord fromNbt(CompoundTag tag) {
        MobLearningRecord record = new MobLearningRecord();
        int inputs = tag.getInt("inputs").orElse(0);
        int outputs = tag.getInt("outputs").orElse(0);
        inputs = Math.max(0, inputs);
        outputs = Math.max(0, outputs);
        record.ensureDimensions(inputs, outputs);

        int expected = inputs * outputs;

        CompoundTag weightsTag = tag.getCompound("weights").orElseGet(CompoundTag::new);
        if (expected > 0) {
            for (int i = 0; i < expected; i++) {
                String key = Integer.toString(i);
                double value = weightsTag.getDouble(key).orElse(DEFAULT_WEIGHT);
                record.weights[i] = sanitizeWeight(value);
            }
        }

        ListTag fitnessList = tag.getList("fitness").orElseGet(ListTag::new);
        for (int i = 0; i < fitnessList.size(); i++) {
            net.minecraft.nbt.Tag entry = fitnessList.get(i);
            double value = DEFAULT_WEIGHT;
            if (entry instanceof CompoundTag fitnessEntry) {
                value = fitnessEntry.getDouble(VALUE_KEY).orElse(DEFAULT_WEIGHT);
            }
            record.fitnessSamples.add(sanitizeWeight(value));
        }

        record.generation = Math.max(0L, tag.getLong("generation").orElse(0L));

        if (expected > 0) {
            ListTag populationList = tag.getList("population").orElseGet(ListTag::new);
            if (populationList.size() > 0) {
                List<double[]> genomes = new ArrayList<>();
                for (int i = 0; i < populationList.size(); i++) {
                    net.minecraft.nbt.Tag entry = populationList.get(i);
                    if (!(entry instanceof CompoundTag genomeTag)) {
                        continue;
                    }
                    double[] genome = new double[expected];
                    for (int j = 0; j < expected; j++) {
                        String key = Integer.toString(j);
                        genome[j] = sanitizeWeight(genomeTag.getDouble(key).orElse(DEFAULT_WEIGHT));
                    }
                    genomes.add(genome);
                }
                record.population = genomes.toArray(new double[0][]);
            }
        }

        return record;
    }

    private static double sanitizeWeight(double value) {
        if (!Double.isFinite(value)) {
            return DEFAULT_WEIGHT;
        }
        return clamp(value);
    }

    private static double clamp(double value) {
        if (value > 8.0) {
            return 8.0;
        }
        if (value < -8.0) {
            return -8.0;
        }
        return value;
    }
}
