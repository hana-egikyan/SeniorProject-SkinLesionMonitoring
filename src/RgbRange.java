// right now analyzeByColorRange used a fixed darkish rule,
// which could break when lighting changes.
// this class defines an explicit RGB range for more stable detection.

public class RgbRange {

    public final int minR, maxR;
    public final int minG, maxG;
    public final int minB, maxB;

    public RgbRange(int minR, int maxR,
                    int minG, int maxG,
                    int minB, int maxB) {

        this.minR = clamp(minR);
        this.maxR = clamp(maxR);

        this.minG = clamp(minG);
        this.maxG = clamp(maxG);

        this.minB = clamp(minB);
        this.maxB = clamp(maxB);
    }

    // checks whether a pixel's rgb values are inside the allowed range
    public boolean contains(int r, int g, int b) {
        return r >= minR && r <= maxR
                && g >= minG && g <= maxG
                && b >= minB && b <= maxB;
    }

    // helper method to build a range around a seed pixel
    public static RgbRange around(int r, int g, int b, int tol) {
        return new RgbRange(
                r - tol, r + tol,
                g - tol, g + tol,
                b - tol, b + tol
        );
    }

    // clamps a value between 0 and 255
    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
