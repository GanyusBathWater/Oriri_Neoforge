import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateGlow {
    public static void main(String[] args) throws Exception {
        int size = 128;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        float cx = size / 2.0f;
        float cy = size / 2.0f;
        float maxDist = cx;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                float dx = x - cx + 0.5f;
                float dy = y - cy + 0.5f;
                float dist = (float) Math.sqrt(dx*dx + dy*dy);
                
                float alpha = 1.0f - (dist / maxDist);
                if (alpha < 0) alpha = 0;
                
                // Exponentiate to make the glow fade quickly but smoothly
                alpha = (float) Math.pow(alpha, 1.5);
                
                int a = (int)(alpha * 255);
                img.setRGB(x, y, new Color(255, 255, 255, a).getRGB());
            }
        }
        
        File out = new File("src/main/resources/assets/oririmod/textures/misc/soft_glow.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println("Done!");
    }
}
