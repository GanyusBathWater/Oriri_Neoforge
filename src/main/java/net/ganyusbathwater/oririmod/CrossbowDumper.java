import net.minecraft.world.item.CrossbowItem;
import java.lang.reflect.Method;

public class CrossbowDumper {
    public static void main(String[] args) {
        Method[] methods = CrossbowItem.class.getDeclaredMethods();
        for (Method m : methods) {
            System.out.println(m.getReturnType().getSimpleName() + " " + m.getName() + "(...");
        }
    }
}
