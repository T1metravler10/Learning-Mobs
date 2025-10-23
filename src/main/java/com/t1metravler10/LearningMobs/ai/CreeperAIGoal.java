package com.t1metravler10.LearningMobs.ai;

import com.t1metravler10.LearningMobs.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CreeperAIGoal extends Goal {
    private final Creeper creeper;
    private final EvolvingAIController controller;

    public CreeperAIGoal(Creeper creeper) {
        this.creeper = creeper;
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
        double maxDistance = Math.max(4.0D, Config.creeperActiveDistance);
        double distance = Math.min(maxDistance, toTarget.length());

        double distanceNorm = clamp01(1.0D - (distance / maxDistance));
        double creeperHealthNorm = clamp01(creeper.getHealth() / creeper.getMaxHealth());
        double playerHealthNorm = clamp01(target.getHealth() / target.getMaxHealth());
        double dayFraction = (creeper.level().getDayTime() % 24000L) / 24000.0D;
        double brightness = creeper.level().isBrightOutside() ? 1.0D : 0.0D;

        double[] inputs = new double[] {
            distanceNorm,
            creeperHealthNorm,
            playerHealthNorm,
            clamp01(dayFraction),
            brightness
        };

        double[] outputs = controller.evaluate(inputs);
        if (outputs.length > 0 && outputs[0] > 0.55D) {
            Path path = creeper.getNavigation().createPath(target, 0);
            if (path != null) {
                double speed = clamp(0.6D + outputs[0] * 0.8D, 0.6D, 1.4D);
                creeper.getNavigation().moveTo(path, speed);
            }
        }

        if (outputs.length > 1 && outputs[1] > 0.85D) {
            MobGenerationManager.get((ServerLevel) creeper.level()).rewardCreeperExplosionProximity(creeper);
            creeper.ignite();
        }
    }

    @Override
    public void stop() {
        // Fitness reporting handled by event hooks.
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 1.0D) {
            return 1.0D;
        }
        return value;
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
