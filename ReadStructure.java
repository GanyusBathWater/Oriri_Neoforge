import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.HashSet;
import java.io.ByteArrayOutputStream;

public class ReadStructure {
    public static void main(String[] args) {
        try {
            FileInputStream fis = new FileInputStream("src/main/resources/data/oririmod/structure/elderwoods_outpost_0_0.nbt");
            GZIPInputStream gis = new GZIPInputStream(fis);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            gis.close();
            
            String content = new String(baos.toByteArray(), "UTF-8");
            
            Pattern pattern = Pattern.compile("(oririmod:[a-z_]+|minecraft:[a-z_]+)");
            Matcher matcher = pattern.matcher(content);
            Set<String> blocks = new HashSet<>();
            while (matcher.find()) {
                blocks.add(matcher.group(1));
            }
            
            for (String b : blocks) {
                System.out.println(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
