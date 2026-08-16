public class Test {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent");
        for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
            System.out.println(m.getName() + " -> " + m.getReturnType().getName());
        }
    }
}
