import java.util.Comparator;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.time.LocalDateTime;


// this class compares saved results
// for now it compares the last two results (most recent saves)
public class Comparer {

    // this makes a short text that can be seen in a popup
    public static String compareLatestTwo(List<AnalysisResult> all) {

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        if (all == null || all.size() < 2) {
            return "not enough saved results to compare (need at least 2)";
        }

        // sort by date then time so the newest is at the end
        all.sort(
                Comparator.comparing((AnalysisResult a) -> a.date)
                        .thenComparing(a -> a.time)
        );

        AnalysisResult last = all.getLast();                 // newest
        AnalysisResult prev = all.get(all.size() - 2);       // second newest

        LocalDateTime prevDateTime = LocalDateTime.of(prev.date, prev.time);
        LocalDateTime lastDateTime = LocalDateTime.of(last.date, last.time);
        long elapsedMinutes = Duration.between(prevDateTime, lastDateTime).toMinutes();
        double elapsedDays = elapsedMinutes / (60.0 * 24.0);


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
        text += "Comparison of the Two Most Recent Saved Results\n\n";

        text += "Previous Result:\n";
        text += "Date: " + prev.date + "\n";
        text += "Time: " + prev.time.format(timeFmt) + "\n";
        text += "Area (pixels): " + prev.lesionPixelCount + "\n";
        text += "Average RGB: " + String.format("%.2f", prev.avgR) + ", "
                + String.format("%.2f", prev.avgG) + ", "
                + String.format("%.2f", prev.avgB) + "\n\n";

        text += "Current Result:\n";
        text += "Date: " + last.date + "\n";
        text += "Time: " + last.time.format(timeFmt) + "\n";
        text += "Area (pixels): " + last.lesionPixelCount + "\n";
        text += "Average RGB: " + String.format("%.2f", last.avgR) + ", "
                + String.format("%.2f", last.avgG) + ", "
                + String.format("%.2f", last.avgB) + "\n\n";

        text += "Observed Change:\n";
        text += "Area Difference: " + diffPixels + " pixels (" + String.format("%.2f", pct) + "%)\n";
        text += "Average RGB Difference: "
                + String.format("%.2f", diffR) + ", "
                + String.format("%.2f", diffG) + ", "
                + String.format("%.2f", diffB) + "\n";

        text += "\nInterpretation:\n";
        text += "Elapsed time: " + String.format("%.2f", elapsedDays) + " day(s)\n";

        if (diffPixels > 0) {
            text += "The detected lesion region became larger between the two saved observations.\n";
        } else if (diffPixels < 0) {
            text += "The detected lesion region became smaller between the two saved observations.\n";
        } else {
            text += "The detected lesion region size stayed the same between the two saved observations.\n";
        }

        double prevIntensity = (prev.avgR + prev.avgG + prev.avgB) / 3.0;
        double lastIntensity = (last.avgR + last.avgG + last.avgB) / 3.0;
        double intensityDiff = lastIntensity - prevIntensity;

        if (intensityDiff > 1) {
            text += "The lesion appears lighter on average in the newer image.\n";
        } else if (intensityDiff < -1) {
            text += "The lesion appears darker on average in the newer image.\n";
        } else {
            text += "The lesion brightness appears relatively unchanged between the two images.\n";
        }

        return text;
    }
}
