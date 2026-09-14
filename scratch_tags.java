public class Test {
    public static void main(String[] args) {
        for (java.lang.reflect.Field f : net.minecraft.tags.ItemTags.class.getFields()) {
            System.out.println(f.getName());
        }
    }
}
