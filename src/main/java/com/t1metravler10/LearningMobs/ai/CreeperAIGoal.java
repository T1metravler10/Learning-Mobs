package com.t1metravler10.LearningMobs.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CreeperAIGoal extends Goal {
    private final Creeper creeper;
    private final EvolvingAIController controller;
    private final MobGenerationManager generationManager;

    public CreeperAIGoal(Creeper creeper) {
        this.creeper = creeper;
        this.generationManager = MobGenerationManager.getInstance();
        this.controller = CreeperSquadManager.getInstance().getController(creeper);
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return controller != null && !creeper.isIgnited();
    }

    @Override
    public void tick() {
        if (controller == null) {
            return;
        }
        Player target = creeper.level().getNearestPlayer(creeper, 50.0D);
        if (target == null) {
            return;
        }

        Vec3 toTarget = target.position().subtract(creeper.position());
        double distance = toTarget.length();

        double[] inputs = new double[] {
            distance / 50.0,
            creeper.getHealth() / creeper.getMaxHealth(),
            target.getHealth() / target.getMaxHealth(),
            (creeper.tickCount % 24000) / 24000.0,
            creeper.level().isDay() ? 1.0 : 0.0
        };

        double[] outputs = controller.think(inputs);

        if (outputs.length > 0 && outputs[0] > 0.5) {
            Path path = creeper.getNavigation().createPath(target, 0);
            if (path != null) {
                creeper.getNavigation().moveTo(path, 1.2D);
            }
        }

        if (outputs.length > 1 && outputs[1] > 0.8) {
            creeper.ignite();
        }
    }

    @Override
    public void stop() {
        if (controller != null) {
            generationManager.reportFitness(creeper, creeper.getHealth());
        }
    }
}
