package com.t1metravler10.LearningMobs.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class MobLearningRecord {
    private int inputSize;
    private int outputSize;
    private double[] weights = new double[0];
    private final List<Double> fitnessSamples = new ArrayList<>();

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
        if (inputSize == inputs && outputSize == outputs && weights.length == inputs * outputs) {
            return;
        }
        inputSize = inputs;
        outputSize = outputs;
        weights = new double[inputs * outputs];
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
        outputSize = matrix.length;
        inputSize = matrix.length == 0 ? 0 : matrix[0].length;
        weights = new double[inputSize * outputSize];
        for (int o = 0; o < outputSize; o++) {
            for (int i = 0; i < inputSize; i++) {
                weights[o * inputSize + i] = matrix[o][i];
            }
        }
    }

    public List<Double> fitnessSamples() {
        return fitnessSamples;
    }

    public void clearFitness() {
        fitnessSamples.clear();
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("inputs", inputSize);
        tag.putInt("outputs", outputSize);

        ListTag weightList = new ListTag();
        for (double weight : weights) {
            weightList.add(DoubleTag.valueOf(weight));
        }
        tag.put("weights", weightList);

        ListTag fitnessTag = new ListTag();
        for (Double value : fitnessSamples) {
            fitnessTag.add(DoubleTag.valueOf(value));
        }
        tag.put("fitness", fitnessTag);
        return tag;
    }

    public static MobLearningRecord fromNbt(CompoundTag tag) {
        MobLearningRecord record = new MobLearningRecord();
        record.inputSize = tag.getInt("inputs");
        record.outputSize = tag.getInt("outputs");

        ListTag weightList = tag.getList("weights", Tag.TAG_DOUBLE);
        record.weights = new double[weightList.size()];
        for (int i = 0; i < weightList.size(); i++) {
            record.weights[i] = weightList.getDouble(i);
        }

        ListTag fitnessTag = tag.getList("fitness", Tag.TAG_DOUBLE);
        for (int i = 0; i < fitnessTag.size(); i++) {
            record.fitnessSamples.add(fitnessTag.getDouble(i));
        }
        return record;
    }
}
