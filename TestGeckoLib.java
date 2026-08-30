import java.lang.reflect.Method;
import software.bernie.geckolib.cache.object.GeoBone;

public class TestGeckoLib {
    public static void main(String[] args) {
        System.out.println("Methods of GeoBone:");
        for (Method m : GeoBone.class.getMethods()) {
            if (m.getName().toLowerCase().contains("pos") || m.getName().toLowerCase().contains("rot") || m.getName().toLowerCase().contains("set")) {
                System.out.println(m.getName() + "(" + java.util.Arrays.toString(m.getParameterTypes()) + ")");
            }
        }
    }
}
