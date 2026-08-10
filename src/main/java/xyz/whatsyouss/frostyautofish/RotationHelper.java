package xyz.whatsyouss.frostyautofish;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class RotationHelper {
    private final Minecraft minecraft;

    RotationHelper(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    void aimAt(Vec3 position, float smoothing) {
        if (minecraft.player == null) {
            return;
        }
        float[] rotation = rotationTo(minecraft.player.getEyePosition(), position);
        setYaw(rotation[0], smoothing);
        setPitch(rotation[1], smoothing);
    }

    void setYaw(float target, float smoothing) {
        float current = minecraft.player.getYRot();
        float next = current + Mth.wrapDegrees(target - current) / smoothing;
        minecraft.player.setYRot(mouseStep(next, current));
    }

    void setPitch(float target, float smoothing) {
        float current = minecraft.player.getXRot();
        float next = current + Mth.wrapDegrees(target - current) / smoothing;
        minecraft.player.setXRot(mouseStep(next, current));
    }

    boolean closeTo(float yaw, float pitch, float threshold) {
        return Math.abs(Mth.wrapDegrees(yaw - minecraft.player.getYRot())) <= threshold
                && Math.abs(Mth.wrapDegrees(pitch - minecraft.player.getXRot())) <= threshold;
    }

    void snapTo(float yaw, float pitch) {
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.setYRot(yaw);
        minecraft.player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
    }

    boolean canHit(Entity target, double reach) {
        Vec3 eye = minecraft.player.getEyePosition();
        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        Vec3 direction = new Vec3(
                -Math.sin(yawRadians) * Math.cos(pitchRadians),
                -Math.sin(pitchRadians),
                Math.cos(yawRadians) * Math.cos(pitchRadians)
        );
        Vec3 end = eye.add(direction.scale(reach));

        HitResult block = minecraft.level.clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player
        ));
        if (block.getType() != HitResult.Type.MISS
                && block.getLocation().distanceToSqr(eye) < target.getBoundingBox().getCenter().distanceToSqr(eye)) {
            return false;
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                minecraft.player,
                eye,
                end,
                target.getBoundingBox().inflate(0.3),
                entity -> entity == target,
                reach
        );
        return entityHit != null && entityHit.getEntity() == target;
    }

    static float[] rotationTo(Vec3 from, Vec3 to) {
        double x = to.x - from.x;
        double y = to.y - from.y;
        double z = to.z - from.z;
        double horizontal = Math.sqrt(x * x + z * z);
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, horizontal));
        return new float[]{Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch)};
    }

    private float mouseStep(float target, float current) {
        float sensitivity = minecraft.options.sensitivity().get().floatValue();
        float factor = sensitivity * 0.6F + 0.2F;
        float gcd = factor * factor * factor * 8.0F * 0.15F;
        int pixels = Math.round(Mth.wrapDegrees(target - current) / gcd);
        return current + pixels * gcd;
    }
}
