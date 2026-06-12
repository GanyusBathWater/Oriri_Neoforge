import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateVortex {
    public static void main(String[] args) throws Exception {
        int size = 1024;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        float cx = size / 2.0f;
        float cy = size / 2.0f;
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                float dx = x - cx;
                float dy = y - cy;
                float dist = (float) Math.sqrt(dx*dx + dy*dy) / cx;
                float angle = (float) Math.atan2(dy, dx);
                float angleMod = (angle + (float)Math.PI * 4) % ((float)Math.PI * 2);
                
                Color white = new Color(255, 255, 255, 255);
                Color lightBlue = new Color(200, 225, 255, 255);
                Color midBlue = new Color(100, 150, 255, 255);
                Color darkBlue = new Color(50, 80, 255, 255);
                Color transparent = new Color(0, 0, 0, 0);
                
                // Add some subtle brush waviness to the main rings
                float wave = (float)Math.sin(angle * 5.0f) * 0.005f + (float)Math.sin(angle * 11.0f) * 0.003f;
                
                float innerEdge = 0.44f;
                float whiteEdge = 0.65f + wave;
                float lightBlueEdge = 0.77f + wave * 1.5f;
                float midBlueEdge = 0.85f + wave * 2.0f;
                
                if (dist < innerEdge) {
                    img.setRGB(x, y, transparent.getRGB());
                } else if (dist < whiteEdge) {
                    img.setRGB(x, y, white.getRGB());
                } else if (dist < lightBlueEdge) {
                    img.setRGB(x, y, lightBlue.getRGB());
                } else if (dist < midBlueEdge) {
                    img.setRGB(x, y, midBlue.getRGB());
                } else {
                    // Outer Streaks
                    boolean isStreak = false;
                    boolean isLightStreak = false;
                    
                    // 3 main dark blue tapering streaks
                    for (int i = 0; i < 3; i++) {
                        float streakCenter = i * ((float)Math.PI * 2.0f / 3.0f);
                        float diff = Math.abs(angleMod - streakCenter);
                        if (diff > Math.PI) diff = (float)Math.PI * 2 - diff;
                        
                        // Tapering
                        if (diff < 1.8f) { // Long tail
                            float thickness = 0.06f * (float)Math.pow(1.0f - (diff / 1.8f), 1.5f);
                            float streakRadiusOffset = 0.01f * diff; // Spirals slightly outwards
                            
                            if (dist > midBlueEdge + streakRadiusOffset && dist < midBlueEdge + streakRadiusOffset + thickness) {
                                isStreak = true;
                            }
                        }
                    }
                    
                    // 3 detached, thin light blue speed lines
                    for (int i = 0; i < 3; i++) {
                        float streakCenter = i * ((float)Math.PI * 2.0f / 3.0f) + 0.5f;
                        float diff = Math.abs(angleMod - streakCenter);
                        if (diff > Math.PI) diff = (float)Math.PI * 2 - diff;
                        
                        if (diff < 1.2f) {
                            float thickness = 0.015f * (float)Math.pow(1.0f - (diff / 1.2f), 2.0f);
                            float streakDist = midBlueEdge + 0.04f + 0.03f * diff;
                            if (dist > streakDist && dist < streakDist + thickness) {
                                isLightStreak = true;
                            }
                        }
                    }
                    
                    if (isLightStreak) {
                        img.setRGB(x, y, midBlue.getRGB());
                    } else if (isStreak) {
                        img.setRGB(x, y, darkBlue.getRGB());
                    } else {
                        img.setRGB(x, y, transparent.getRGB());
                    }
                }
            }
        }
        
        File out = new File("src/main/resources/assets/oririmod/textures/misc/vortex.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println("Perfect Terraria Smear Ring generated!");
    }
}
