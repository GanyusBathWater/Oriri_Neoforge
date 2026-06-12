package net.oriri.test;
import net.minecraft.client.renderer.PostChain;
import java.lang.reflect.Method;
public class Test {
    public static void main(String[] args) {
        for(Method m : PostChain.class.getMethods()) {
            System.out.println(m.getName());
        }
    }
}
