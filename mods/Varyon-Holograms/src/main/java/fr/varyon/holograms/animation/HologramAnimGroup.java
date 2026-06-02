package fr.varyon.holograms.animation;

import org.joml.Vector3d;
import org.joml.Vector3f;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HologramAnimGroup {

    public static final class Member {
        public final UUID entityId;
        public final Vector3d lineOffset;
        public final Vector3f baseRotation;
        public final float baseScale;
        @Nullable private Vector3d lastSetPosition;

        public Member(@Nonnull UUID entityId, @Nonnull Vector3d lineOffset,
                      @Nonnull Vector3f baseRotation, float baseScale) {
            this.entityId = entityId;
            this.lineOffset = new Vector3d(lineOffset);
            this.baseRotation = new Vector3f(baseRotation);
            this.baseScale = baseScale;
        }

        @Nullable
        public Vector3d getLastSetPosition() {
            return lastSetPosition != null ? new Vector3d(lastSetPosition) : null;
        }

        public void setLastSetPosition(@Nonnull Vector3d position) {
            this.lastSetPosition = new Vector3d(position);
        }
    }

    @Nonnull private final UUID worldId;
    @Nonnull private final AnimationData animation;
    @Nonnull private Vector3d anchorPosition;
    private float elapsed;
    @Nullable private Vector3d lastSetAnchor;
    @Nonnull private final List<Member> members = new ArrayList<>();

    public HologramAnimGroup(@Nonnull UUID worldId, @Nonnull AnimationData animation,
                               @Nonnull Vector3d anchorPosition) {
        this.worldId = worldId;
        this.animation = animation;
        this.anchorPosition = new Vector3d(anchorPosition);
    }

    public void addMember(@Nonnull Member member) {
        members.add(member);
    }

    @Nonnull public UUID getWorldId() { return worldId; }
    @Nonnull public AnimationData getAnimation() { return animation; }
    @Nonnull public Vector3d getAnchorPosition() { return new Vector3d(anchorPosition); }
    @Nonnull public List<Member> getMembers() { return members; }
    public float getElapsed() { return elapsed; }

    public void setAnchorPosition(@Nonnull Vector3d position) {
        this.anchorPosition = new Vector3d(position);
    }

    @Nullable
    public Vector3d getLastSetAnchor() {
        return lastSetAnchor != null ? new Vector3d(lastSetAnchor) : null;
    }

    public void setLastSetAnchor(@Nonnull Vector3d position) {
        this.lastSetAnchor = new Vector3d(position);
    }

    public void tick(float deltaSeconds) {
        elapsed += deltaSeconds;
        if (animation.isLoop() && animation.getDuration() > 0) {
            elapsed %= animation.getDuration();
        }
    }

    @Nonnull
    public Vector3d getAnimatedAnchor() {
        Keyframe kf = animation.sample(elapsed);
        if (kf == null || kf.getPositionOffset() == null) {
            return new Vector3d(anchorPosition);
        }
        Vector3d off = kf.getPositionOffset();
        return new Vector3d(anchorPosition.x + off.x, anchorPosition.y + off.y, anchorPosition.z + off.z);
    }

    @Nonnull
    public Vector3d getMemberPosition(@Nonnull Vector3d lineOffset) {
        Vector3d anchor = getAnimatedAnchor();
        return new Vector3d(anchor.x + lineOffset.x, anchor.y + lineOffset.y, anchor.z + lineOffset.z);
    }

    @Nonnull
    public Vector3f getMemberRotation(@Nonnull Vector3f baseRotation) {
        Keyframe kf = animation.sample(elapsed);
        if (kf == null || kf.getRotationDelta() == null) {
            return new Vector3f(baseRotation);
        }
        Vector3f delta = kf.getRotationDelta();
        return new Vector3f(
            (float) Math.toRadians(baseRotation.x + delta.x),
            (float) Math.toRadians(baseRotation.y + delta.y),
            (float) Math.toRadians(baseRotation.z + delta.z)
        );
    }

    public float getMemberScale(float baseScale) {
        Keyframe kf = animation.sample(elapsed);
        return kf == null ? baseScale : baseScale * kf.getScale();
    }
}
