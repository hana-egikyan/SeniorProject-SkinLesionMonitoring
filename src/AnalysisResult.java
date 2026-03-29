import java.time.LocalDate;
import java.time.LocalTime;

// this class stores the output of one analysis
// it represents one saved result for one image on one date/time
// and the computed metrics such as pixel count and average RGB
// it does NOT perform analysis, it only stores the result

public class AnalysisResult {

    // path to the analyzed image file (as chosen by the user)
    String imagePath;

    // date/time when the result was saved
    LocalDate date;
    LocalTime time;

    // the extracted features
    int lesionPixelCount;
    int perimeterPixelCount;
    double circularity;
    double avgR;
    double avgG;
    double avgB;
    String acceptanceVerdict;


    // extra extracted features (color variance)
    double varR;
    double varG;
    double varB;

    // parameters used for segmentation
    int toleranceUsed;
    int minR;
    int maxR;
    int minG;
    int maxG;
    int minB;
    int maxB;

    // constructor used by the analyzer = storage for the values
    public AnalysisResult(String imagePath,
                          LocalDate date,
                          LocalTime time,
                          int lesionPixelCount,
                          int perimeterPixelCount,
                          double circularity,
                          double avgR,
                          double avgG,
                          double avgB,
                          double varR,
                          double varG,
                          double varB,
                          int toleranceUsed,
                          int minR,
                          int maxR,
                          int minG,
                          int maxG,
                          int minB,
                          int maxB,
                          String acceptanceVerdict) {

        this.imagePath = imagePath;
        this.date = date;
        this.time = time;
        this.lesionPixelCount = lesionPixelCount;
        this.perimeterPixelCount = perimeterPixelCount;
        this.circularity = circularity;
        this.avgR = avgR;
        this.avgG = avgG;
        this.avgB = avgB;
        this.varR = varR;
        this.varG = varG;
        this.varB = varB;

        this.toleranceUsed = toleranceUsed;
        this.minR = minR;
        this.maxR = maxR;
        this.minG = minG;
        this.maxG = maxG;
        this.minB = minB;
        this.maxB = maxB;
        this.acceptanceVerdict = acceptanceVerdict;
    }

    // convert to a readable .txt format
    public String toTextBlock() {

        return "imagePath=" + imagePath + "\n" +
                "date=" + date + "\n" +
                "time=" + time + "\n" +
                "lesionPixelCount=" + lesionPixelCount + "\n" +
                "acceptanceVerdict=" + acceptanceVerdict + "\n" +
                "perimeterPixelCount=" + perimeterPixelCount + "\n" +
                "circularity=" + String.format("%.6f", circularity) + "\n" +
                "avgR=" + String.format("%.2f", avgR) + "\n" +
                "avgG=" + String.format("%.2f", avgG) + "\n" +
                "avgB=" + String.format("%.2f", avgB) + "\n" +
                "varR=" + String.format("%.2f", varR) + "\n" +
                "varG=" + String.format("%.2f", varG) + "\n" +
                "varB=" + String.format("%.2f", varB) + "\n" +
                "toleranceUsed=" + toleranceUsed + "\n" +
                "minR=" + minR + "\n" +
                "maxR=" + maxR + "\n" +
                "minG=" + minG + "\n" +
                "maxG=" + maxG + "\n" +
                "minB=" + minB + "\n" +
                "maxB=" + maxB + "\n";
    }

    // helper used by ResultStorage.loadAll()
    public static AnalysisResult fromTextBlock(String text) {

        if (text == null) return null;

        String imagePath = "";
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        int count = 0;
        int perimeterPixelCount = 0;
        double circularity = 0;

        double avgR = 0;
        double avgG = 0;
        double avgB = 0;

        double varR = 0;
        double varG = 0;
        double varB = 0;

        int toleranceUsed = 0;
        int minR = 0;
        int maxR = 0;
        int minG = 0;
        int maxG = 0;
        int minB = 0;
        int maxB = 0;

        String acceptanceVerdict = "NOT_CHECKED";

        String[] lines = text.split("\\R"); // split on any newline

        for (String line : lines) {

            int eq = line.indexOf('=');
            if (eq < 0) continue;

            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();

            try {
                switch (key) {
                    case "imagePath" -> imagePath = value;
                    case "date" -> date = LocalDate.parse(value);
                    case "time" -> time = LocalTime.parse(value);
                    case "lesionPixelCount" -> count = Integer.parseInt(value);
                    case "perimeterPixelCount" -> perimeterPixelCount = Integer.parseInt(value);
                    case "circularity" -> circularity = Double.parseDouble(value);
                    case "acceptanceVerdict" -> acceptanceVerdict = value;
                    case "avgR" -> avgR = Double.parseDouble(value);
                    case "avgG" -> avgG = Double.parseDouble(value);
                    case "avgB" -> avgB = Double.parseDouble(value);
                    case "varR" -> varR = Double.parseDouble(value);
                    case "varG" -> varG = Double.parseDouble(value);
                    case "varB" -> varB = Double.parseDouble(value);
                    case "toleranceUsed" -> toleranceUsed = Integer.parseInt(value);
                    case "minR" -> minR = Integer.parseInt(value);
                    case "maxR" -> maxR = Integer.parseInt(value);
                    case "minG" -> minG = Integer.parseInt(value);
                    case "maxG" -> maxG = Integer.parseInt(value);
                    case "minB" -> minB = Integer.parseInt(value);
                    case "maxB" -> maxB = Integer.parseInt(value);
                }
            } catch (Exception ignored) {
                // if a line is not how it should be, ignore it
            }
        }

        return new AnalysisResult(
                imagePath, date, time, count,
                perimeterPixelCount, circularity,
                avgR, avgG, avgB,
                varR, varG, varB,
                toleranceUsed,
                minR, maxR,
                minG, maxG,
                minB, maxB,
                acceptanceVerdict
        );
    }
}