package net.ganyusbathwater.oririmod.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import java.lang.reflect.Method;

public class TestUniform {
    public static void main(String[] args) {
        for (Method m : Uniform.class.getDeclaredMethods()) {
            if (m.getName().equals("set")) {
                System.out.print("set(");
                for (Class<?> p : m.getParameterTypes()) {
                    System.out.print(p.getSimpleName() + ", ");
                }
                System.out.println(")");
            }
        }
    }
}
