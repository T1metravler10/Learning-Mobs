package com.t1metravler10.LearningMobs.ai;

public class NeuralNetwork {
    private final int inputSize;
    private final int outputSize;
    private final double[] weights;

    public NeuralNetwork(int inputSize, int outputSize, double[] flatWeights) {
        if (inputSize <= 0 || outputSize <= 0) {
            throw new IllegalArgumentException("Input and output sizes must be positive");
        }
        if (flatWeights.length != inputSize * outputSize) {
            throw new IllegalArgumentException("Expected " + (inputSize * outputSize) + " weights, got " + flatWeights.length);
        }
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.weights = flatWeights.clone();
    }

    public int inputSize() {
        return inputSize;
    }

    public int outputSize() {
        return outputSize;
    }

    public int weightCount() {
        return weights.length;
    }

    public double[] feedForward(double[] inputs) {
        if (inputs.length != inputSize) {
            throw new IllegalArgumentException("Expected " + inputSize + " inputs, got " + inputs.length);
        }
        double[] outputs = new double[outputSize];
        for (int o = 0; o < outputSize; o++) {
            double sum = 0.0;
            int offset = o * inputSize;
            for (int i = 0; i < inputSize; i++) {
                sum += weights[offset + i] * inputs[i];
            }
            outputs[o] = activation(sum);
        }
        return outputs;
    }

    public double[] weights() {
        return weights.clone();
    }

    public void setWeights(double[] flat) {
        if (flat.length != weights.length) {
            throw new IllegalArgumentException("Expected " + weights.length + " weights");
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] = sanitize(flat[i]);
        }
    }

    private static double activation(double value) {
        // Sigmoid for 0..1 output range.
        if (value > 30) {
            return 1.0;
        }
        if (value < -30) {
            return 0.0;
        }
        return 1.0 / (1.0 + Math.exp(-value));
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        if (value > 8.0) {
            return 8.0;
        }
        if (value < -8.0) {
            return -8.0;
        }
        return value;
    }
}
