import java.lang.reflect.Method;
public class ReflectClass {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("net.neoforged.neoforge.client.event.RenderTooltipEvent");
        for (Method m : clazz.getMethods()) {
            System.out.println("Base: " + m.getName() + " -> " + m.getReturnType().getSimpleName());
        }
        for (Class<?> inner : clazz.getDeclaredClasses()) {
            System.out.println("Inner class: " + inner.getSimpleName());
            for (Method m : inner.getMethods()) {
                System.out.println("  " + m.getName() + " -> " + m.getReturnType().getSimpleName());
            }
        }
    }
}
