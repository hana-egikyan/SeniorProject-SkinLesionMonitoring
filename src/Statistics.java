// this class provides an alternative implementation of variance using
// welford's algorithm. unlike the two-pass method used in
// LesionAnalyzer, welford's algorithm computes the running mean and
// variance in a single pass over the data, which halves the number
// of image accesses and improves numerical stability for extreme inputs.
//
//

public class Statistics {

    // holds the running state of welford's algorithm for a single channel
    public static class WelfordAccumulator {
        private long count = 0;
        private double mean = 0;
        private double m2 = 0;  // sum of squared differences from current mean

        // add a new sample to the running computation
        public void add(double value) {
            count++;
            double delta = value - mean;
            mean += delta / count;
            double delta2 = value - mean;
            m2 += delta * delta2;
        }

        public long getCount() {
            return count;
        }

        public double getMean() {
            return mean;
        }

        // population variance (divides by count, not count-1)
        // matches the LesionAnalyzer convention
        public double getVariance() {
            if (count == 0) return 0;
            return m2 / count;
        }
    }

    // convenience: three channels at once for RGB variance
    public static class RgbWelford {
        public final WelfordAccumulator r = new WelfordAccumulator();
        public final WelfordAccumulator g = new WelfordAccumulator();
        public final WelfordAccumulator b = new WelfordAccumulator();

        public void add(int red, int green, int blue) {
            r.add(red);
            g.add(green);
            b.add(blue);
        }
    }
}