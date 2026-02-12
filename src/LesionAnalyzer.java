import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;

// this class contains the main image analysis algorithms to count pixels inside a selection and compute average RGB
public class LesionAnalyzer {

    public static AnalysisResult analyzeByColorRange(BufferedImage img, String imagePath, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalTime time = LocalTime.now();
        return analyzeByColorRange(img, imagePath, date, time);
    }

    // analyzes the whole image using fixed RGB threshold rule and evey pixel is checked separately
    public static AnalysisResult analyzeByColorRange(BufferedImage img, String imagePath, LocalDate date, LocalTime time) {

        if (img == null) return null;

        int w = img.getWidth();
        int h = img.getHeight();

        int count = 0;
        long sumR = 0, sumG = 0, sumB = 0;

        // simple thresholding example (will edit later)
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int rgb = img.getRGB(x, y);
                Color c = new Color(rgb);

                int r = c.getRed();
                int g = c.getGreen();
                int b = c.getBlue();

                if (r < 120 && g < 120 && b < 120) {
                    count++;
                    sumR += r;
                    sumG += g;
                    sumB += b;
                }
            }
        }

        double avgR = (count == 0) ? 0 : (sumR * 1.0 / count);
        double avgG = (count == 0) ? 0 : (sumG * 1.0 / count);
        double avgB = (count == 0) ? 0 : (sumB * 1.0 / count);

        return new AnalysisResult(imagePath, date, time, count, avgR, avgG, avgB);
    }

    // analyze inside a user selection mask
    // selected[y][x] == true means that pixel is included
    public static AnalysisResult analyzeFromSelected(BufferedImage img, boolean[][] selected,
                                                     String imagePath, LocalDate date, LocalTime time) {
        if (img == null || selected == null) {
            return new AnalysisResult(imagePath, date, time, 0, 0, 0, 0);
        }

        //counters for computing the average color
        int count = 0;
        long sumR = 0, sumG = 0, sumB = 0;

        int h = img.getHeight();
        int w = img.getWidth();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (y < selected.length && x < selected[y].length && selected[y][x]) {
                    int rgb = img.getRGB(x, y);
                    Color c = new Color(rgb);

                    sumR += c.getRed();
                    sumG += c.getGreen();
                    sumB += c.getBlue();
                    count++;
                }
            }
        }


        // compute averages by dividing sums by number of selected pixels
        if (count == 0) {
            return new AnalysisResult(imagePath, date, time, 0, 0, 0, 0);
        }

        double avgR = (double) sumR / count;
        double avgG = (double) sumG / count;
        double avgB = (double) sumB / count;

        return new AnalysisResult(imagePath, date, time, count, avgR, avgG, avgB);
    }
}
