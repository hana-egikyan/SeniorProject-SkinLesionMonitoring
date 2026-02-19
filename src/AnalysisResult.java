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
    double avgR;
    double avgG;
    double avgB;

    // constructor used by the analyzer = storage for the values
    public AnalysisResult(String imagePath, LocalDate date, LocalTime time,
                          int lesionPixelCount, double avgR, double avgG, double avgB) {
        this.imagePath = imagePath;
        this.date = date;
        this.time = time;
        this.lesionPixelCount = lesionPixelCount;
        this.avgR = avgR;
        this.avgG = avgG;
        this.avgB = avgB;
    }

    // convert to a readable .txt format
    public String toTextBlock() {

        return "imagePath=" + imagePath + "\n" +
                "date=" + date + "\n" +
                "time=" + time + "\n" +
                "lesionPixelCount=" + lesionPixelCount + "\n" +
                "avgR=" + String.format("%.2f", avgR) + "\n" +
                "avgG=" + String.format("%.2f", avgG) + "\n" +
                "avgB=" + String.format("%.2f", avgB) + "\n";
    }

    // helper used by ResultStorage.loadAll()
    public static AnalysisResult fromTextBlock(String text) {

        if (text == null) return null;

        String imagePath = "";
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        int count = 0;
        double avgR = 0;
        double avgG = 0;
        double avgB = 0;

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
                    case "avgR" -> avgR = Double.parseDouble(value);
                    case "avgG" -> avgG = Double.parseDouble(value);
                    case "avgB" -> avgB = Double.parseDouble(value);
                }
            } catch (Exception ignored) {
                // if a line is not how it should be, ignore it
            }
        }

        return new AnalysisResult(imagePath, date, time, count, avgR, avgG, avgB);
    }
}
