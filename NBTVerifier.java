import java.nio.file.Files;
import java.nio.file.Paths;

public class NBTVerifier {
    public static void main(String[] args) throws Exception {
        byte[] bytes = java.util.zip.GZIPInputStream.class.getConstructor(java.io.InputStream.class).newInstance(new java.io.FileInputStream("src/main/resources/data/oririmod/structure/outpost_scarlet_ruins.nbt")).readAllBytes();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (b >= 32 && b <= 126) {
                sb.append((char) b);
            } else {
                sb.append('.');
            }
        }
        String content = sb.toString();
        
        System.out.println("Contains magenta terracotta? " + content.contains("magenta_glazed_terracotta"));
        System.out.println("Contains chest? " + content.contains("minecraft:chest"));
        System.out.println("Contains LootTable? " + content.contains("LootTable"));
    }
}
