package net.ganyusbathwater.oririmod.util.vestiges;

public record VestigeBonusSnapshot(double healthBonus, float stepHeightBonus, float luckBonus) {

    public static VestigeBonusSnapshot empty() {
        return new VestigeBonusSnapshot(0.0D, 0.0F, 0.0F);
    }

    public boolean hasHealthBonus() {
        return healthBonus > 0.0D;
    }

    public boolean hasStepBonus() {
        return stepHeightBonus > 0.0F;
    }

    public boolean hasLuckBonus() {
        return luckBonus > 0.0F;
    }
}