package com.t1metravler10.LearningMobs.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CustomAIGoal extends Goal {
    private final Mob mob;
    private final EvolvingAIController controller;
    private final MobGenerationManager generationManager;

    public CustomAIGoal(Mob mob) {
        this.mob = mob;
        this.generationManager = MobGenerationManager.getInstance();
        this.controller = generationManager.assignToMob(mob);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return controller != null;
    }

    @Override
    public void start() {
        if (controller != null) {
            System.out.println("Custom AI started for: " + mob.getName().getString());
        }
    }

    @Override
    public void stop() {
        if (controller != null) {
            System.out.println("Custom AI stopped for: " + mob.getName().getString());
            generationManager.reportFitness(mob, calculateFitness());
        }
    }

    @Override
    public void tick() {
        if (controller == null) {
            return;
        }
        double[] inputs = getInputs();
        double[] outputs = controller.think(inputs);
        applyOutputs(outputs);
    }

    private double[] getInputs() {
        return new double[] {1, 0, 0.5, 0.2, 0.8};
    }

    private void applyOutputs(double[] outputs) {
        if (outputs.length < 2) {
            return;
        }
        mob.getLookControl().setLookAt(mob.getX() + outputs[0], mob.getEyeY(), mob.getZ() + outputs[1]);
    }

    private double calculateFitness() {
        return mob.getHealth();
    }
}
