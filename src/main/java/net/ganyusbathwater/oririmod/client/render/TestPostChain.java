package net.ganyusbathwater.oririmod.client.render;

import net.minecraft.client.renderer.PostChain;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class TestPostChain {
    public static void main(String[] args) {
        System.out.println("--- POST CHAIN METHODS ---");
        for (Method m : PostChain.class.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
        System.out.println("--- POST CHAIN FIELDS ---");
        for (Field f : PostChain.class.getDeclaredFields()) {
            System.out.println(f.getName() + " -> " + f.getType().getSimpleName());
        }
    }
}
