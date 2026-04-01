import java.lang.reflect.Method;

public class TestItem {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = net.minecraft.world.item.Item.class;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().toLowerCase().contains("hurt") || m.getName().toLowerCase().contains("enemy")) {
                System.out.println("Method: " + m.getName());
                for (Class<?> p : m.getParameterTypes()) {
                    System.out.println("  Param: " + p.getName());
                }
            }
        }
    }
}
