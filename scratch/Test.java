import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import java.lang.reflect.Method;
public class Test {
    public static void main(String[] args) {
        for (Method m : RenderTooltipEvent.Pre.class.getMethods()) {
            System.out.println("Pre: " + m.getName() + " returns " + m.getReturnType().getSimpleName());
        }
        for (Method m : RenderTooltipEvent.Color.class.getMethods()) {
            System.out.println("Color: " + m.getName() + " returns " + m.getReturnType().getSimpleName());
        }
    }
}
