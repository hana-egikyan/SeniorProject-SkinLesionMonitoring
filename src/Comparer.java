import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// this class compares saved results
// for now it compares the last two results (most recent saves)
public class Comparer {

    // this makes a short text that can be seen in a popup
    public static String compareLatestTwo(List<AnalysisResult> all) {

        if (all == null || all.size() < 2) {
            return "not enough saved results to compare (need at least 2)";
        }

        // sort by date then time so the newest is at the end
        Collections.sort(all, new Comparator<AnalysisResult>() {
            @Override
            public int compare(AnalysisResult a, AnalysisResult b) {

                int d = a.date.compareTo(b.date);
                if (d != 0) return d;

                return a.time.compareTo(b.time);
            }
        });

        AnalysisResult prev = all.get(all.size() - 2);
        AnalysisResult last = all.get(all.size() - 1);

        int diffPixels = last.lesionPixelCount - prev.lesionPixelCount;

        double prevCount = prev.lesionPixelCount;
        double pct = 0;
        if (prevCount > 0) {
            pct = (diffPixels * 100.0) / prevCount;
        }

        double diffR = last.avgR - prev.avgR;
        double diffG = last.avgG - prev.avgG;
        double diffB = last.avgB - prev.avgB;

        String text = "";
        text += "Comparing latest two saves\n\n";

        text += "Older:\n";
        text += "date " + prev.date + " time " + prev.time + "\n";
        text += "pixels " + prev.lesionPixelCount + "\n";
        text += "avg rgb " + String.format("%.2f", prev.avgR) + " "
                + String.format("%.2f", prev.avgG) + " "
                + String.format("%.2f", prev.avgB) + "\n\n";

        text += "Newer:\n";
        text += "date " + last.date + " time " + last.time + "\n";
        text += "pixels " + last.lesionPixelCount + "\n";
        text += "avg rgb " + String.format("%.2f", last.avgR) + " "
                + String.format("%.2f", last.avgG) + " "
                + String.format("%.2f", last.avgB) + "\n\n";

        text += "Change:\n";
        text += "pixel diff " + diffPixels + " (" + String.format("%.2f", pct) + "%)\n";
        text += "avg rgb diff "
                + String.format("%.2f", diffR) + " "
                + String.format("%.2f", diffG) + " "
                + String.format("%.2f", diffB) + "\n";

        return text;
    }
}
