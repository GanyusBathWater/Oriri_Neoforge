import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateAssets {
    public static void main(String[] args) throws Exception {
        // 1. Generate black.png
        BufferedImage black = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        black.setRGB(0, 0, new Color(0, 0, 0, 255).getRGB());
        File blackOut = new File("src/main/resources/assets/oririmod/textures/misc/black.png");
        blackOut.getParentFile().mkdirs();
        ImageIO.write(black, "png", blackOut);

        // 2. Generate accretion_disk.png
        int size = 256;
        BufferedImage disk = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        float cx = size / 2.0f;
        float cy = size / 2.0f;
        
        // Inner radius = 0.25 (the black hole core)
        // Middle radius = 0.40 (white hot)
        // Outer radius = 1.00 (orange fade)
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                float dx = x - cx + 0.5f;
                float dy = y - cy + 0.5f;
                float dist = (float) Math.sqrt(dx*dx + dy*dy) / cx; // 0.0 to 1.0
                
                int r = 0, g = 0, b = 0, a = 0;
                
                if (dist < 0.22f) {
                    // Inside the event horizon - transparent so the 3D core shows through
                    a = 0;
                } else if (dist < 0.35f) {
                    // Photon ring: Blinding White
                    float t = (dist - 0.22f) / (0.35f - 0.22f);
                    r = 255; g = 255; b = 255;
                    // Fade in sharply
                    a = (int)(Math.min(1.0f, t * 5.0f) * 255);
                } else if (dist < 0.60f) {
                    // Mid zone: Yellow
                    float t = (dist - 0.35f) / (0.60f - 0.35f);
                    r = 255; 
                    g = (int)(255 - t * (255 - 200)); // 255 -> 200
                    b = (int)(255 - t * 255);         // 255 -> 0
                    a = 255;
                } else if (dist <= 1.0f) {
                    // Outer zone: Orange fading to transparent
                    float t = (dist - 0.60f) / (0.40f);
                    r = 255;
                    g = (int)(200 - t * 100); // 200 -> 100
                    b = 0;
                    // Exponential fade out for alpha
                    float alphaFade = 1.0f - t;
                    alphaFade = (float) Math.pow(alphaFade, 1.5);
                    a = (int)(alphaFade * 255);
                }
                
                disk.setRGB(x, y, new Color(r, g, b, a).getRGB());
            }
        }
        
        File diskOut = new File("src/main/resources/assets/oririmod/textures/misc/accretion_disk.png");
        ImageIO.write(disk, "png", diskOut);
        
        System.out.println("Assets generated!");
    }
}
