package com.t1metravler10.LearningMobs.ai;

import java.util.Random;

public class NeuralNetwork {
    private double[][] weights;

    public NeuralNetwork(int inputSize, int outputSize) {
        weights = new double[outputSize][inputSize];
        Random rand = new Random();
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                weights[i][j] = rand.nextDouble() * 2 - 1;
            }
        }
    }

    public double[] feedForward(double[] inputs) {
        double[] outputs = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            double sum = 0;
            for (int j = 0; j < weights[i].length; j++) {
                sum += weights[i][j] * inputs[j];
            }
            outputs[i] = sigmoid(sum);
        }
        return outputs;
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public double[][] getWeights() {
        return weights;
    }

    public void setWeights(double[][] newWeights) {
        this.weights = newWeights;
    }
}
