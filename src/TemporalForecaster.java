import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// this class calculates simple trends over time from saved results

public class TemporalForecaster {

    public static ForecastReport buildReport(List<AnalysisResult> results, int daysAhead) {

        if (results == null || results.size() < 2) {
            return new ForecastReport("not enough saved results (need at least 2)");
        }

        // sort by date/time so time order is correct
        results.sort(Comparator.comparing(r -> LocalDateTime.of(r.date, r.time)));

        List<GrowthPoint> points = new ArrayList<>();

        double lastGrowthPerDay = 0;
        double lastIntensityChangePerDay = 0;

        for (int i = 1; i < results.size(); i++) {

            AnalysisResult prev = results.get(i - 1);
            AnalysisResult curr = results.get(i);

            LocalDateTime t1 = LocalDateTime.of(prev.date, prev.time);
            LocalDateTime t2 = LocalDateTime.of(curr.date, curr.time);

            long minutes = Duration.between(t1, t2).toMinutes();
            if (minutes <= 0) continue;

            double days = minutes / (60.0 * 24.0);

            int deltaArea = curr.lesionPixelCount - prev.lesionPixelCount;
            double growthPerDay = deltaArea / days;

            double prevIntensity = intensity(prev);
            double currIntensity = intensity(curr);
            double intensityChangePerDay = (currIntensity - prevIntensity) / days;

            // keep the most recent interval rate (last valid one)
            lastGrowthPerDay = growthPerDay;
            lastIntensityChangePerDay = intensityChangePerDay;

            points.add(new GrowthPoint(t2, growthPerDay, intensityChangePerDay));
        }

        if (points.isEmpty()) {
            return new ForecastReport("time intervals were invalid (same time or backwards)");
        }

        // very simple: average rate over intervals
        double avgGrowthPerDay = 0;
        double avgIntensityChangePerDay = 0;

        for (GrowthPoint p : points) {
            avgGrowthPerDay += p.growthPerDay;
            avgIntensityChangePerDay += p.intensityChangePerDay;
        }

        avgGrowthPerDay /= points.size();
        avgIntensityChangePerDay /= points.size();

        AnalysisResult last = results.get(results.size() - 1);

        int projectedArea = (int) Math.round(last.lesionPixelCount + avgGrowthPerDay * daysAhead);
        if (projectedArea < 0) projectedArea = 0;

        double projectedIntensity = intensity(last) + avgIntensityChangePerDay * daysAhead;

        return new ForecastReport(
                points.size(),
                lastGrowthPerDay,
                lastIntensityChangePerDay,
                avgGrowthPerDay,
                avgIntensityChangePerDay,
                projectedArea,
                projectedIntensity,
                daysAhead
        );
    }

    // simple brightness proxy using avg RGB
    private static double intensity(AnalysisResult r) {
        return (r.avgR + r.avgG + r.avgB) / 3.0;
    }

    // small helper classes so we can print clean output
    public static class GrowthPoint {
        public LocalDateTime time;
        public double growthPerDay;
        public double intensityChangePerDay;

        public GrowthPoint(LocalDateTime time, double growthPerDay, double intensityChangePerDay) {
            this.time = time;
            this.growthPerDay = growthPerDay;
            this.intensityChangePerDay = intensityChangePerDay;
        }
    }

    public static class ForecastReport {
        public String message; // if something is wrong
        public int intervalsUsed;

        public double lastGrowthPerDay;
        public double lastIntensityChangePerDay;

        public double avgGrowthPerDay;
        public double avgIntensityChangePerDay;

        public int projectedArea;
        public double projectedIntensity;
        public int daysAhead;

        public ForecastReport(String message) {
            this.message = message;
        }

        public ForecastReport(int intervalsUsed,
                              double lastGrowthPerDay,
                              double lastIntensityChangePerDay,
                              double avgGrowthPerDay,
                              double avgIntensityChangePerDay,
                              int projectedArea,
                              double projectedIntensity,
                              int daysAhead) {
            this.intervalsUsed = intervalsUsed;
            this.lastGrowthPerDay = lastGrowthPerDay;
            this.lastIntensityChangePerDay = lastIntensityChangePerDay;
            this.avgGrowthPerDay = avgGrowthPerDay;
            this.avgIntensityChangePerDay = avgIntensityChangePerDay;
            this.projectedArea = projectedArea;
            this.projectedIntensity = projectedIntensity;
            this.daysAhead = daysAhead;
        }

        public String toText() {
            if (message != null) return message;

            return "forecast using " + intervalsUsed + " interval(s)\n" +
                    "last growth (pixels/day): " + String.format("%.2f", lastGrowthPerDay) + "\n" +
                    "last intensity change/day: " + String.format("%.2f", lastIntensityChangePerDay) + "\n" +
                    "avg growth (pixels/day): " + String.format("%.2f", avgGrowthPerDay) + "\n" +
                    "avg intensity change/day: " + String.format("%.2f", avgIntensityChangePerDay) + "\n" +
                    "projection for +" + daysAhead + " days:\n" +
                    "  projected area: " + projectedArea + " pixels\n" +
                    "  projected intensity: " + String.format("%.2f", projectedIntensity);
        }
    }
}
