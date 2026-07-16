import java.lang.reflect.*;

public class Test {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("net.minecraft.world.level.levelgen.structure.structures.JigsawStructure");
        for (Method m : clazz.getDeclaredMethods()) {
            System.out.println(m);
        }
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            System.out.println(c);
        }
    }
}
