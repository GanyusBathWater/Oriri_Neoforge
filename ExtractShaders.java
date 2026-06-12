import java.io.*;
import java.util.zip.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
public class ExtractShaders {
    public static void main(String[] args) throws Exception {
        File dir = new File(System.getProperty("user.home"), ".gradle/caches");
        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".jar") && file.toString().contains("client") && file.toString().contains("extra")) {
                    try {
                        ZipFile zf = new ZipFile(file.toFile());
                        ZipEntry entry = zf.getEntry("assets/minecraft/shaders/post/creeper.json");
                        if (entry != null) {
                            System.out.println("Found in: " + file);
                            InputStream in = zf.getInputStream(entry);
                            System.out.println("creeper.json:\n" + new String(in.readAllBytes()));
                            in.close();
                            
                            ZipEntry entry2 = zf.getEntry("assets/minecraft/shaders/program/blit.json");
                            if (entry2 != null) {
                                InputStream in2 = zf.getInputStream(entry2);
                                System.out.println("blit.json:\n" + new String(in2.readAllBytes()));
                                in2.close();
                            }
                            System.exit(0);
                        }
                        zf.close();
                    } catch (Exception e) {}
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
