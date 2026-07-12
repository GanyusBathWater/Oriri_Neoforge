import java.nio.file.Files;
import java.nio.file.Paths;

public class DumpStrings {
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
        int idx = content.toLowerCase().indexOf("loottable");
        if (idx != -1) {
            System.out.println("FOUND LootTable! Snippet: " + content.substring(Math.max(0, idx - 20), Math.min(content.length(), idx + 100)));
        } else {
            System.out.println("LootTable NOT FOUND in outpost_scarlet_ruins.unzipped");
        }
        
        // Also check intact
        byte[] bytes2 = java.util.zip.GZIPInputStream.class.getConstructor(java.io.InputStream.class).newInstance(new java.io.FileInputStream("src/main/resources/data/oririmod/structure/elderwoods_outpost_intact.nbt")).readAllBytes();
        StringBuilder sb2 = new StringBuilder();
        for (byte b : bytes2) {
            if (b >= 32 && b <= 126) {
                sb2.append((char) b);
            } else {
                sb2.append('.');
            }
        }
        String c2 = sb2.toString();
        idx = c2.toLowerCase().indexOf("loottable");
        if (idx != -1) {
            System.out.println("FOUND LootTable in intact! Snippet: " + c2.substring(Math.max(0, idx - 20), Math.min(c2.length(), idx + 100)));
        } else {
            System.out.println("LootTable NOT FOUND in elderwoods_outpost_intact.nbt");
        }
    }
}
