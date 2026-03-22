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

        results.sort(Comparator.comparing(r -> LocalDateTime.of(r.date, r.time)));

        List<AnalysisResult> filtered = new ArrayList<>();
        filtered.add(results.get(0));

        // keep only points that are at least 12 hours apart
        for (int i = 1; i < results.size(); i++) {
            AnalysisResult prevKept = filtered.get(filtered.size() - 1);
            AnalysisResult curr = results.get(i);

            LocalDateTime t1 = LocalDateTime.of(prevKept.date, prevKept.time);
            LocalDateTime t2 = LocalDateTime.of(curr.date, curr.time);

            long minutes = Duration.between(t1, t2).toMinutes();
            if (minutes > 0) {
                filtered.add(curr);
            }
        }

        if (filtered.size() < 2) {
            return new ForecastReport("Not enough valid timestamped results for forecasting.");
        }

        LocalDateTime baseTime = LocalDateTime.of(filtered.get(0).date, filtered.get(0).time);

        List<Double> xDays = new ArrayList<>();
        List<Double> yArea = new ArrayList<>();
        List<Double> yIntensity = new ArrayList<>();

        for (AnalysisResult r : filtered) {
            LocalDateTime t = LocalDateTime.of(r.date, r.time);
            long minutes = Duration.between(baseTime, t).toMinutes();
            double days = minutes / (60.0 * 24.0);

            xDays.add(days);
            yArea.add((double) r.lesionPixelCount);
            yIntensity.add(intensity(r));
        }

        RegressionResult areaFit = fitLine(xDays, yArea);
        RegressionResult intensityFit = fitLine(xDays, yIntensity);

        double lastX = xDays.get(xDays.size() - 1);
        double futureX = lastX + daysAhead;

        int projectedArea = (int) Math.round(areaFit.intercept + areaFit.slope * futureX);
        if (projectedArea < 0) projectedArea = 0;

        double projectedIntensity = intensityFit.intercept + intensityFit.slope * futureX;
        if (projectedIntensity < 0) projectedIntensity = 0;

        return new ForecastReport(
                filtered.size() - 1,
                areaFit.slope,
                intensityFit.slope,
                areaFit.slope,
                intensityFit.slope,
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
    private static RegressionResult fitLine(List<Double> x, List<Double> y) {
        int n = x.size();

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double xi = x.get(i);
            double yi = y.get(i);

            sumX += xi;
            sumY += yi;
            sumXY += xi * yi;
            sumX2 += xi * xi;
        }

        double denom = n * sumX2 - sumX * sumX;

        if (Math.abs(denom) < 1e-9) {
            return new RegressionResult(0, y.get(y.size() - 1));
        }

        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;

        return new RegressionResult(slope, intercept);
    }

    static class RegressionResult {
        double slope;
        double intercept;

        RegressionResult(double slope, double intercept) {
            this.slope = slope;
            this.intercept = intercept;
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

            return  "Forecast using regression on " + (intervalsUsed + 1) + " saved point(s)\n" +
                    "Estimated growth (pixels/day): " + String.format("%.2f", avgGrowthPerDay) + "\n" +
                    "Estimated intensity change/day: " + String.format("%.2f", avgIntensityChangePerDay) + "\n" +
                    "Projection for +" + daysAhead + " days:\n" +
                    "Projected area: " + projectedArea + " pixels\n" +
                    "Projected intensity: " + String.format("%.2f", projectedIntensity) + "\n\n" +
                    "Trend interpretation:\n" +
                    areaTrendText() + "\n" +
                    intensityTrendText() + "\n" +
                    forecastMeaningText();
        }

        private String areaTrendText() {
            if (avgGrowthPerDay > 5) {
                return "The detected lesion region shows an upward size trend over time.";
            } else if (avgGrowthPerDay < -5) {
                return "The detected lesion region shows a downward size trend over time.";
            } else {
                return "The detected lesion region appears relatively stable in size over time.";
            }
        }

        private String intensityTrendText() {
            if (avgIntensityChangePerDay > 1) {
                return "The lesion appears to be getting lighter on average over time.";
            } else if (avgIntensityChangePerDay < -1) {
                return "The lesion appears to be getting darker on average over time.";
            } else {
                return "The lesion color intensity appears relatively stable over time.";
            }
        }

        private String forecastMeaningText() {
            return "This forecast is based on the timestamped saved results and estimates the future trend from the observed changes between them.";
        }
    }
}
