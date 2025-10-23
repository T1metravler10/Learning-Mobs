package com.t1metravler10.LearningMobs.ai;

public class GeneticAlgorithm {
    public static NeuralNetwork evolve(NeuralNetwork base, double fitness) {
        double[][] oldWeights = base.getWeights();
        double[][] newWeights = new double[oldWeights.length][oldWeights[0].length];

        for (int i = 0; i < oldWeights.length; i++) {
            for (int j = 0; j < oldWeights[i].length; j++) {
                newWeights[i][j] = oldWeights[i][j] + (Math.random() - 0.5) * 0.1;
            }
        }

        NeuralNetwork evolved = new NeuralNetwork(oldWeights[0].length, oldWeights.length);
        evolved.setWeights(newWeights);
        return evolved;
    }
}
