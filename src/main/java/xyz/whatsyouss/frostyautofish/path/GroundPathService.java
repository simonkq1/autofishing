package xyz.whatsyouss.frostyautofish.path;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class GroundPathService {
    private final NavMeshGenerator generator = new NavMeshGenerator();
    private final NavMeshPathfinder pathfinder = new NavMeshPathfinder();
    private long tick;

    public void tick() {
        tick++;
    }

    public NavMeshPath find(Vec3 start, Vec3 goal, ClientLevel level, int maximumRange) {
        Vec3 groundedStart = findGround(start, level);
        Vec3 groundedGoal = findGround(goal, level);
        if (groundedStart == null || groundedGoal == null) {
            return NavMeshPath.empty();
        }

        NavMesh mesh = generator.generate(level, groundedStart, groundedGoal, maximumRange);
        if (mesh.getPolyCount() == 0) {
            return NavMeshPath.empty();
        }
        return pathfinder.findPath(mesh, groundedStart, groundedGoal, tick, level);
    }

    private Vec3 findGround(Vec3 position, ClientLevel level) {
        BlockPos block = BlockPos.containing(position);
        for (int distance = 0; distance <= 10; distance++) {
            for (int direction = 0; direction <= 1; direction++) {
                int offset = direction == 0 ? distance : -distance;
                if (distance == 0 && direction == 1) {
                    continue;
                }
                BlockPos candidate = block.offset(0, offset, 0);
                double surface = NavMeshGenerator.getSurfaceY(candidate, level);
                if (!Double.isNaN(surface)) {
                    return new Vec3(position.x, surface, position.z);
                }

                if (!level.hasChunk(candidate.getX() >> 4, candidate.getZ() >> 4)) {
                    continue;
                }
                var state = level.getBlockState(candidate);
                var shape = state.getCollisionShape(level, candidate);
                if (shape.isEmpty()
                        || NavMeshGenerator.isNonSolidObstacle(state)
                        || NavMeshGenerator.isHazardous(state)) {
                    continue;
                }
                double top = candidate.getY() + shape.max(Direction.Axis.Y);
                BlockPos feet = BlockPos.containing(position.x, top, position.z);
                double feetSurface = NavMeshGenerator.getSurfaceY(feet, level);
                if (!Double.isNaN(feetSurface) && Math.abs(feetSurface - top) < 0.1) {
                    return new Vec3(position.x, top, position.z);
                }
            }
        }
        return null;
    }
}
