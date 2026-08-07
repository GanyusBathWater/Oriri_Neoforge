package net.ganyusbathwater.oririmod.util;

public class ShaderDetector {
    private static java.lang.reflect.Method irisApiGetInstance;
    private static java.lang.reflect.Method irisApiIsShaderPackInUse;
    private static java.lang.reflect.Method optifineIsShaders;
    private static boolean initialized = false;

    public static boolean isShaderActive() {
        if (!initialized) {
            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApiGetInstance = irisApiClass.getMethod("getInstance");
                irisApiIsShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
            } catch (Exception e) {}
            
            try {
                Class<?> optifineConfigClass = Class.forName("net.optifine.Config");
                optifineIsShaders = optifineConfigClass.getMethod("isShaders");
            } catch (Exception e) {}
            
            initialized = true;
        }
        
        // Check Iris/Oculus
        if (irisApiGetInstance != null && irisApiIsShaderPackInUse != null) {
            try {
                Object instance = irisApiGetInstance.invoke(null);
                if (instance != null) {
                    if ((Boolean) irisApiIsShaderPackInUse.invoke(instance)) {
                        return true;
                    }
                }
            } catch (Exception e) {}
        }
        
        // Check OptiFine
        if (optifineIsShaders != null) {
            try {
                if ((Boolean) optifineIsShaders.invoke(null)) {
                    return true;
                }
            } catch (Exception e) {}
        }
        
        return false;
    }
}
