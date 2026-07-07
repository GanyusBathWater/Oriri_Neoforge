import java.lang.reflect.*;

public class InspectFlowerPot {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("net.minecraft.world.level.block.FlowerPotBlock");
        System.out.println("Constructors:");
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            System.out.println("  " + c);
        }
        System.out.println("Methods:");
        for (Method m : clazz.getDeclaredMethods()) {
            System.out.println("  " + m);
        }
    }
}
