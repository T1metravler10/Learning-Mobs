package com.t1metravler10.LearningMobs.ai;

final class EvolvingAIController {
    private final MobGenerationManager.AssignedGenome assignment;

    EvolvingAIController(MobGenerationManager.AssignedGenome assignment) {
        this.assignment = assignment;
    }

    MobGenerationManager.AssignedGenome assignment() {
        return assignment;
    }

    double[] evaluate(double[] normalizedInputs) {
        return assignment.network().feedForward(normalizedInputs);
    }
}
