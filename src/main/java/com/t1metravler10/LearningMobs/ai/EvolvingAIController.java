package com.t1metravler10.LearningMobs.ai;

public class EvolvingAIController {
    private final NeuralNetwork network;

    public EvolvingAIController(NeuralNetwork baseNetwork) {
        this.network = baseNetwork;
    }

    public double[] think(double[] inputs) {
        return network.feedForward(inputs);
    }
}
