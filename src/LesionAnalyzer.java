import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;

// this class contains the main image analysis algorithm
// it analyzes only the user-selected region produced by region growing
public class LesionAnalyzer {

    // analyze inside a user selection mask
    // selected[y][x] == true means that pixel is included
    public static AnalysisResult analyzeFromSelected(BufferedImage img,
                                                     boolean[][] selected,
                                                     String imagePath,
                                                     LocalDate date,
                                                     LocalTime time) {

        if (img == null || selected == null) {
            return new AnalysisResult(imagePath, date, time, 0, 0, 0, 0);
        }

        int count = 0;
        long sumR = 0;
        long sumG = 0;
        long sumB = 0;

        int h = img.getHeight();
        int w = img.getWidth();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                if (y < selected.length &&
                        x < selected[y].length &&
                        selected[y][x]) {

                    int rgb = img.getRGB(x, y);
                    Color c = new Color(rgb);

                    sumR += c.getRed();
                    sumG += c.getGreen();
                    sumB += c.getBlue();
                    count++;
                }
            }
        }

        if (count == 0) {
            return new AnalysisResult(imagePath, date, time, 0, 0, 0, 0);
        }

        double avgR = (double) sumR / count;
        double avgG = (double) sumG / count;
        double avgB = (double) sumB / count;

        return new AnalysisResult(imagePath, date, time, count, avgR, avgG, avgB);
    }
}
