import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import java.lang.reflect.Method;
public class ReflectMethods {
    public static void main(String[] args) {
        System.out.println("MeleeAttackGoal methods:");
        for(Method m : MeleeAttackGoal.class.getDeclaredMethods()) {
            System.out.println(m.getName() + " -> " + m.getReturnType().getName());
        }
        System.out.println("LivingEntity methods:");
        for(Method m : net.minecraft.world.entity.LivingEntity.class.getDeclaredMethods()) {
            if(m.getName().toLowerCase().contains("attack") || m.getName().toLowerCase().contains("reach")) {
                System.out.println(m.getName() + " -> " + m.getReturnType().getName());
            }
        }
    }
}
