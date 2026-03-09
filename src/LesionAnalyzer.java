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
                                                     LocalTime time,
                                                     int toleranceUsed,
                                                     int minR, int maxR,
                                                     int minG, int maxG,
                                                     int minB, int maxB) {

        if (img == null || selected == null) {
            return new AnalysisResult(
                    imagePath, date, time,
                    0, 0, 0, 0,
                    0, 0, 0,
                    toleranceUsed,
                    minR, maxR,
                    minG, maxG,
                    minB, maxB
            );
        }

        int count = 0;

        long sumR = 0;
        long sumG = 0;
        long sumB = 0;

        int h = img.getHeight();
        int w = img.getWidth();

        // -------- FIRST PASS: compute averages --------
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
            return new AnalysisResult(
                    imagePath, date, time,
                    0, 0, 0, 0,
                    0, 0, 0,
                    toleranceUsed,
                    minR, maxR,
                    minG, maxG,
                    minB, maxB
            );
        }

        double avgR = (double) sumR / count;
        double avgG = (double) sumG / count;
        double avgB = (double) sumB / count;

        // -------- SECOND PASS: compute variance --------
        double sumSqR = 0;
        double sumSqG = 0;
        double sumSqB = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                if (y < selected.length &&
                        x < selected[y].length &&
                        selected[y][x]) {

                    int rgb = img.getRGB(x, y);
                    Color c = new Color(rgb);

                    double dr = c.getRed() - avgR;
                    double dg = c.getGreen() - avgG;
                    double db = c.getBlue() - avgB;

                    sumSqR += dr * dr;
                    sumSqG += dg * dg;
                    sumSqB += db * db;
                }
            }
        }

        double varR = sumSqR / count;
        double varG = sumSqG / count;
        double varB = sumSqB / count;

        return new AnalysisResult(
                imagePath, date, time,
                count,
                avgR, avgG, avgB,
                varR, varG, varB,
                toleranceUsed,
                minR, maxR,
                minG, maxG,
                minB, maxB
        );
    }
}