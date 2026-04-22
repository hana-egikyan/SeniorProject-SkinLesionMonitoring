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

        // keep only chronologically later results and skip duplicate or non-increasing timestamps
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

        // compute the "last interval" slope separately: the rate of change
        // between the second-to-last and last observations. this is distinct
        // from the overall regression slope and can reveal recent acceleration
        // or deceleration that the overall fit may not capture.
        int n = xDays.size();
        double lastIntervalAreaSlope;
        double lastIntervalIntensitySlope;

        double x1 = xDays.get(n - 2);
        double x2 = xDays.get(n - 1);
        double dx = x2 - x1;

        if (dx > 1e-9) {
            lastIntervalAreaSlope = (yArea.get(n - 1) - yArea.get(n - 2)) / dx;
            lastIntervalIntensitySlope = (yIntensity.get(n - 1) - yIntensity.get(n - 2)) / dx;
        } else {
            // fallback if the last two points somehow have the same timestamp
            lastIntervalAreaSlope = areaFit.slope;
            lastIntervalIntensitySlope = intensityFit.slope;
        }

        double lastX = x2;
        double futureX = lastX + daysAhead;

        int projectedArea = (int) Math.round(areaFit.intercept + areaFit.slope * futureX);
        if (projectedArea < 0) projectedArea = 0;

        double projectedIntensity = intensityFit.intercept + intensityFit.slope * futureX;
        if (projectedIntensity < 0) projectedIntensity = 0;

        return new ForecastReport(
                filtered.size(),
                lastIntervalAreaSlope,
                lastIntervalIntensitySlope,
                areaFit.slope,
                intensityFit.slope,
                projectedArea,
                projectedIntensity,
                daysAhead
        );
    }

    // luminance formula — the standard weighted conversion from
    // RGB to perceived brightness. this gives a better approximation of
    // "how bright the lesion looks" than an unweighted average.

    private static double intensity(AnalysisResult r) {
        return 0.299 * r.avgR + 0.587 * r.avgG + 0.114 * r.avgB;
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
        public int pointsUsed;

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

        public ForecastReport(int pointsUsed,
                              double lastGrowthPerDay,
                              double lastIntensityChangePerDay,
                              double avgGrowthPerDay,
                              double avgIntensityChangePerDay,
                              int projectedArea,
                              double projectedIntensity,
                              int daysAhead) {
            this.pointsUsed = pointsUsed;
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

            return  "Forecast using regression on " + pointsUsed + " filtered saved point(s)\n\n" +
                    "Overall trend (from full regression):\n" +
                    "  Average growth: " + String.format("%.2f", avgGrowthPerDay) + " pixels/day\n" +
                    "  Average intensity change: " + String.format("%.2f", avgIntensityChangePerDay) + "/day\n\n" +
                    "Most recent interval (between last two observations):\n" +
                    "  Last-interval growth: " + String.format("%.2f", lastGrowthPerDay) + " pixels/day\n" +
                    "  Last-interval intensity change: " + String.format("%.2f", lastIntensityChangePerDay) + "/day\n\n" +
                    "Projection for +" + daysAhead + " days (based on overall trend):\n" +
                    "  Projected area: " + projectedArea + " pixels\n" +
                    "  Projected intensity: " + String.format("%.2f", projectedIntensity) + "\n\n" +
                    "Trend interpretation:\n" +
                    areaTrendText() + "\n" +
                    intensityTrendText() + "\n" +
                    consistencyText() + "\n\n" +
                    "Plain-language summary:\n" +
                    plainLanguageSummary() + "\n\n" +
                    forecastMeaningText();
        }

        // compares the overall regression slope with the last-interval slope
        // and tells the user whether the two agree (trend is stable) or
        // disagree (trend may have changed recently)
        private String consistencyText() {
            double diff = Math.abs(lastGrowthPerDay - avgGrowthPerDay);
            double magnitude = Math.max(Math.abs(avgGrowthPerDay), 1.0);
            double ratio = diff / magnitude;

            if (ratio < 0.3) {
                return "The overall trend and the most recent interval agree, which suggests the observed pattern is stable over time.";
            } else if (lastGrowthPerDay > avgGrowthPerDay) {
                return "The most recent interval shows faster growth than the overall trend, which may indicate recent acceleration. Consider observing more closely in the coming weeks.";
            } else {
                return "The most recent interval shows slower growth (or more shrinkage) than the overall trend, which may indicate recent deceleration or healing.";
            }
        }

        // translates the raw numbers into a short plain-language summary
        // framed around what the user actually cares about
        private String plainLanguageSummary() {
            if (pointsUsed < 3) {
                return "Note: this forecast is based on only " + pointsUsed + " observations, which is the minimum possible. Forecasts based on so few points are mathematically valid but should be interpreted only as rough trend indicators. Save more observations over time to get more meaningful projections.";
            }

            StringBuilder sb = new StringBuilder();

            // describe the overall trajectory in plain words
            if (avgGrowthPerDay > 5) {
                sb.append("Over the observation period, the lesion region has been growing at roughly ")
                        .append(String.format("%.1f", avgGrowthPerDay))
                        .append(" pixels per day on average. ");
            } else if (avgGrowthPerDay < -5) {
                sb.append("Over the observation period, the lesion region has been shrinking at roughly ")
                        .append(String.format("%.1f", Math.abs(avgGrowthPerDay)))
                        .append(" pixels per day on average. ");
            } else {
                sb.append("Over the observation period, the lesion region has been approximately stable in size. ");
            }

            // project the percentage change rather than raw pixels
            // percentage is easier to interpret than a raw count
            // (the first observation's area is needed to compute this)
            if (daysAhead > 0) {
                sb.append("If the current overall trend continues, the projected area in ")
                        .append(daysAhead)
                        .append(" days would be ")
                        .append(projectedArea)
                        .append(" pixels. ");
            }

            // recommendation framed as an action for the user
            if (avgGrowthPerDay > 5 || (lastGrowthPerDay > avgGrowthPerDay && lastGrowthPerDay > 5)) {
                sb.append("Because the lesion is growing, it would be reasonable to take another observation sooner rather than later - for example, within the next one to two weeks - to confirm whether the trend continues.");
            } else if (avgGrowthPerDay < -5) {
                sb.append("Because the lesion is shrinking, the trajectory may suggest healing. Continuing to save observations at regular intervals will help confirm whether the trend continues.");
            } else {
                sb.append("Because the lesion appears stable, routine follow-up observations at normal intervals (for example, every few weeks) should be sufficient to detect any future change.");
            }

            sb.append("\n\nImportant: this software is a software-engineering prototype, not a clinical tool. Any concerns about a real lesion should be discussed with a qualified medical professional.");

            return sb.toString();
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
