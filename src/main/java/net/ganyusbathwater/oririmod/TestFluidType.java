package net.ganyusbathwater.oririmod;

import net.neoforged.neoforge.fluids.FluidType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

public class TestFluidType {
    public static void main(String[] args) {
        System.out.println("Methods in FluidType.Properties:");
        Method[] methods = FluidType.Properties.class.getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        for (Method m : methods) {
            System.out.println(m.getName() + " - " + Arrays.toString(m.getParameterTypes()));
        }
        
        System.out.println("\nMethods in FluidType:");
        Method[] typeMethods = FluidType.class.getDeclaredMethods();
        Arrays.sort(typeMethods, Comparator.comparing(Method::getName));
        for (Method m : typeMethods) {
            System.out.println(m.getName() + " - " + Arrays.toString(m.getParameterTypes()));
        }
    }
}
