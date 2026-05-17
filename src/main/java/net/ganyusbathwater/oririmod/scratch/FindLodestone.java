package net.ganyusbathwater.oririmod.scratch;

public class FindLodestone {
    public static void check() {
        try {
            Class<?> clazz = Class.forName("team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry");
            System.out.println("Found " + clazz.getName());
        } catch (Exception e) {
            System.out.println("LodestoneParticleRegistry not found. Looking for alternatives...");
        }
    }
}
