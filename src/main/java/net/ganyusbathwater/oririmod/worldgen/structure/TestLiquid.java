import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

public class TestLiquid {
    public static void main(String[] args) {
        for (LiquidSettings ls : LiquidSettings.values()) {
            System.out.println(ls.name());
        }
    }
}
