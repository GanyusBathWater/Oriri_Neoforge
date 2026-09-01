public class TestMeleeAttackGoal {
    public static void main(String[] args) {
        for(java.lang.reflect.Method m : net.minecraft.world.entity.ai.goal.MeleeAttackGoal.class.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
    }
}
