import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


// test runner (printing pass and fail)
public class Tests {

    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) {
        // redirect all persistence to a separate temp directory so the
        // test runner does not pollute the real data folder used by the
        // application. cleanupTestData() removes the temp directory after
        // all tests have finished, so successive runs start fresh.
        ResultStorage.setBaseDir("data-test");
        cleanupTestData();

        try {
        testSaveAndLoad();
        testCompareLatestTwo();
        testAcceptancePass();
        testAcceptanceFailZeroPixels();
        testAcceptanceFailTooLarge();
        testForecastNeedsTwoResults();
        testForecastWorksWithTwoResults();
        testForecastSkipsTinyIntervals();
        testPerimeterSinglePixel();
        testPerimeterSolidBlock();
        testCircularityRange();
        testAnalysisResultRoundTrip();
        testLesionAnalyzerEndToEnd();
        testRgbRangeClamping();
        testRgbRangeAround();
        testCompareDetectsRealDifference();
        testWelfordMatchesTwoPass();

        } finally {
            // always clean up and reset, even if a test threw an exception
            cleanupTestData();
            ResultStorage.setBaseDir(null);
        }

        System.out.println();
        System.out.println("passed " + passed);
        System.out.println("failed " + failed);

        if (failed > 0) System.exit(1);
    }

    static void assertTrue(boolean cond, String name) {
        if (cond) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    // recursively deletes the test data directory so each test run
    // starts from a clean state. ignores any failures since
    // missing files are the expected starting condition
    static void cleanupTestData() {
        java.io.File dir = new java.io.File("data-test");
        deleteRecursive(dir);
    }

    static void deleteRecursive(java.io.File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            java.io.File[] children = f.listFiles();
            if (children != null) {
                for (java.io.File c : children) deleteRecursive(c);
            }
        }
        f.delete();
    }

    // this checks that save creates files and loadAll returns something
    static void testSaveAndLoad() {

        AnalysisResult r = new AnalysisResult(
                "dummy.png",
                "testSeries",
                LocalDate.now(),
                LocalTime.now().withSecond(0).withNano(0),

                10, 6, 0.75,
                1.0, 2.0, 3.0,
                0.0, 0.0, 0.0,

                20,          // toleranceUsed
                0, 255,      // minR, maxR
                0, 255,      // minG, maxG
                0, 255,      // minB, maxB
                "PASS"
        );

        java.io.File saved = ResultStorage.save(r);
        assertTrue(saved != null && saved.exists(), "ResultStorage.save creates a txt file");

        List<AnalysisResult> all = ResultStorage.loadAll();
        assertTrue(!all.isEmpty(), "ResultStorage.loadAll returns at least 1 result");
    }

    // this checks compare works when we have at least two results
    static void testCompareLatestTwo() {

        // create 2 fake results and save them
        AnalysisResult r1 = new AnalysisResult(
                "a.png",
                "testSeries",
                LocalDate.now(),
                LocalTime.now().withSecond(0).withNano(0),

                10, 6, 0.75,
                10.0, 10.0, 10.0,
                0.0, 0.0, 0.0,

                20,          // toleranceUsed
                0, 255,      // minR, maxR
                0, 255,      // minG, maxG
                0, 255,      // minB, maxB
                "PASS"
        );

        AnalysisResult r2 = new AnalysisResult(
                "b.png",
                "testSeries",
                LocalDate.now(),
                LocalTime.now().withSecond(1).withNano(0),

                10, 6, 0.75,
                10.0, 10.0, 10.0,
                0.0, 0.0, 0.0,

                40,          // toleranceUsed
                0, 255,      // minR, maxR
                0, 255,      // minG, maxGz
                0, 255,      // minB, maxB
                "PASS"
        );

        ResultStorage.save(r1);
        ResultStorage.save(r2);

        List<AnalysisResult> all = ResultStorage.loadAll();
        String text = Comparer.compareLatestTwo(all);

        assertTrue(!text.isEmpty(), "Comparer.compareLatestTwo returns text");
        assertTrue(text.contains("Comparison of the Two Most Recent Saved Results"), "compare text contains header");
    }

    // this checks a normal valid case passes
    static void testAcceptancePass() {

        boolean[][] selected = new boolean[5][5];
        selected[2][2] = true;

        AnalysisResult r = new AnalysisResult(
                "ok.png",
                "testSeries",
                LocalDate.now(),
                LocalTime.now().withSecond(0).withNano(0),

                20, 8, 0.70,
                20.0, 20.0, 20.0,
                0.0, 0.0, 0.0,

                20,
                0, 255,
                0, 255,
                0, 255,
                "PASS"
        );

        String verdict = AcceptanceCriteria.validate(selected, 5, 5, r, 1);
        assertTrue(verdict.equals("PASS"), "AcceptanceCriteria PASS case");
    }

    // this checks zero selected pixels fail
    static void testAcceptanceFailZeroPixels() {

        boolean[][] selected = new boolean[5][5];

        AnalysisResult r = new AnalysisResult(
                "zero.png",
                "testSeries",
                LocalDate.now(),
                LocalTime.now().withSecond(0).withNano(0),

                0, 0, 0.0,
                100, 120, 140,
                1.0, 1.0, 1.0,

                20,
                0, 255,
                0, 255,
                0, 255,
                "FAIL"
        );

        String verdict = AcceptanceCriteria.validate(selected, 5, 5, r, 0);
        assertTrue(verdict.contains("no pixels selected"), "AcceptanceCriteria zero pixels fail");
    }

    // this checks huge selections fail
    static void testAcceptanceFailTooLarge() {

        boolean[][] selected = new boolean[10][10];
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                selected[y][x] = true;
            }
        }

        AnalysisResult r = new AnalysisResult(
                "large.png",
                "testSeries",
                LocalDate.now(),
                LocalTime.now().withSecond(0).withNano(0),

                100, 36, 0.5,
                100, 120, 140,
                1.0, 1.0, 1.0,

                20,
                0, 255,
                0, 255,
                0, 255,
                "FAIL"
        );

        String verdict = AcceptanceCriteria.validate(selected, 10, 10, r, 100);
        assertTrue(verdict.contains("region too large"), "AcceptanceCriteria too large fail");
    }

    // this checks forecast fails when there are not enough results
    static void testForecastNeedsTwoResults() {

        List<AnalysisResult> one = new ArrayList<>();

        one.add(new AnalysisResult(
                "one.png",
                "testSeries",
                LocalDate.now(),
                LocalTime.now().withSecond(0).withNano(0),

                100, 36, 0.5,
                100, 120, 140,
                1.0, 1.0, 1.0,

                20,
                0, 255,
                0, 255,
                0, 255,
                "PASS"
        ));

        TemporalForecaster.ForecastReport report = TemporalForecaster.buildReport(one, 7);
        assertTrue(report.message != null && report.message.contains("not enough saved results"),
                "TemporalForecaster needs at least 2 results");
    }

    // this checks forecast works with two valid results
    static void testForecastWorksWithTwoResults() {

        List<AnalysisResult> two = new ArrayList<>();

        two.add(new AnalysisResult(
                "first.png",
                "testSeries",
                LocalDate.of(2026, 2, 20),
                LocalTime.of(12, 0),

                10, 6, 0.75,
                100, 100, 100,
                0.0, 0.0, 0.0,

                20,
                0, 255,
                0, 255,
                0, 255,
                "PASS"
        ));

        two.add(new AnalysisResult(
                "second.png",
                "testSeries",
                LocalDate.of(2026, 2, 21),
                LocalTime.of(12, 0),

                10, 6, 0.75,
                100, 100, 100,
                0.0, 0.0, 0.0,

                20,
                0, 255,
                0, 255,
                0, 255,
                "PASS"
        ));

        TemporalForecaster.ForecastReport report = TemporalForecaster.buildReport(two, 7);

        assertTrue(report.message == null, "TemporalForecaster valid report");
        System.out.println("projectedArea = " + report.projectedArea);
        assertTrue(report.projectedArea > 0, "TemporalForecaster projected area is computed");
    }

    static void testForecastSkipsTinyIntervals() {

        List<AnalysisResult> list = new ArrayList<>();

        list.add(new AnalysisResult(
                "a.png",
                "testSeries",
                LocalDate.of(2026, 3, 1),
                LocalTime.of(10, 0),

                20, 8, 0.70,
                110, 110, 110,
                0.0, 0.0, 0.0,

                20,
                0, 255,
                0, 255,
                0, 255,
                "PASS"
        ));

        list.add(new AnalysisResult(
                "b.png",
                "testSeries",
                LocalDate.of(2026, 3, 1),
                LocalTime.of(10, 5),

                100, 30, 0.60,
                100, 100, 100,
                0.0, 0.0, 0.0,

                20,
                0, 255,
                0, 255,
                0, 255,
                "PASS"
        ));

        list.add(new AnalysisResult(
                "c.png",
                "testSeries",
                LocalDate.of(2026, 3, 10),
                LocalTime.of(10, 0),

                5000, 100, 0.20,
                100, 100, 100,
                0.0, 0.0, 0.0,

                20,
                0, 255,
                0, 255,
                0, 255,
                "PASS"
        ));

        TemporalForecaster.ForecastReport report = TemporalForecaster.buildReport(list, 7);

        assertTrue(report.message == null, "TemporalForecaster skips tiny intervals");
        assertTrue(report.projectedArea >= 0, "TemporalForecaster stays stable with tiny intervals");
    }

    static void testPerimeterSinglePixel() {
        boolean[][] selected = new boolean[3][3];
        selected[1][1] = true;

        int perimeter = LesionAnalyzer.computePerimeter4(selected, 3, 3);
        assertTrue(perimeter == 1, "Perimeter of single pixel is 1 boundary pixel");
    }

    static void testPerimeterSolidBlock() {
        boolean[][] selected = new boolean[4][4];
        selected[1][1] = true;
        selected[1][2] = true;
        selected[2][1] = true;
        selected[2][2] = true;

        int perimeter = LesionAnalyzer.computePerimeter4(selected, 4, 4);
        assertTrue(perimeter == 4, "Perimeter of 2x2 block has 4 boundary pixels with 4-neighborhood rule");
    }

    static void testCircularityRange() {
        double c1 = LesionAnalyzer.computeCircularity(100, 40);
        double c2 = LesionAnalyzer.computeCircularity(100, 10);

        assertTrue(c1 > 0 && c1 <= 1.0, "Circularity stays in range 0..1");
        assertTrue(c2 <= 1.0, "Circularity is clamped to 1 when needed");
    }

    // checks that saving an AnalysisResult to text and parsing it back
    // produces an equivalent object. this is critical because all stored
    // results pass through this
    static void testAnalysisResultRoundTrip() {

        AnalysisResult original = new AnalysisResult(
                "/some/path/lesion.png",
                "lesionA",
                LocalDate.of(2026, 3, 15),
                LocalTime.of(14, 30, 45),

                123, 42, 0.6789,
                100.5, 110.25, 95.75,
                12.5, 8.25, 15.0,

                25,
                10, 200,
                20, 210,
                30, 220,
                "PASS"
        );

        String text = original.toTextBlock();
        AnalysisResult parsed = AnalysisResult.fromTextBlock(text);

        assertTrue(parsed != null, "round-trip parse returns non-null");
        assertTrue(parsed.imagePath.equals(original.imagePath), "round-trip preserves imagePath");
        assertTrue(parsed.seriesId.equals(original.seriesId), "round-trip preserves seriesId");
        assertTrue(parsed.date.equals(original.date), "round-trip preserves date");
        assertTrue(parsed.time.equals(original.time), "round-trip preserves time");
        assertTrue(parsed.lesionPixelCount == original.lesionPixelCount, "round-trip preserves lesionPixelCount");
        assertTrue(parsed.perimeterPixelCount == original.perimeterPixelCount, "round-trip preserves perimeterPixelCount");
        assertTrue(Math.abs(parsed.circularity - original.circularity) < 1e-4, "round-trip preserves circularity");
        assertTrue(Math.abs(parsed.avgR - original.avgR) < 1e-2, "round-trip preserves avgR");
        assertTrue(Math.abs(parsed.avgG - original.avgG) < 1e-2, "round-trip preserves avgG");
        assertTrue(Math.abs(parsed.avgB - original.avgB) < 1e-2, "round-trip preserves avgB");
        assertTrue(Math.abs(parsed.varR - original.varR) < 1e-2, "round-trip preserves varR");
        assertTrue(parsed.toleranceUsed == original.toleranceUsed, "round-trip preserves toleranceUsed");
        assertTrue(parsed.minR == original.minR && parsed.maxR == original.maxR, "round-trip preserves R bounds");
        assertTrue(parsed.acceptanceVerdict.equals(original.acceptanceVerdict), "round-trip preserves verdict");
    }

    // builds a small synthetic image with a known red square in the middle
    // and runs LesionAnalyzer on a mask covering exactly that square. checks
    // that area, average rgb, and variance are exactly what we expect.
    static void testLesionAnalyzerEndToEnd() {

        int w = 10, h = 10;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);

        // fill background with white
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, new java.awt.Color(255, 255, 255).getRGB());
            }
        }

        // paint a 4x4 red square at (3,3)..(6,6) and build a matching mask
        boolean[][] mask = new boolean[h][w];
        int redPixelCount = 0;
        for (int y = 3; y <= 6; y++) {
            for (int x = 3; x <= 6; x++) {
                img.setRGB(x, y, new java.awt.Color(200, 50, 50).getRGB());
                mask[y][x] = true;
                redPixelCount++;
            }
        }

        AnalysisResult r = LesionAnalyzer.analyzeFromSelected(
                img, mask,
                "synthetic.png", "testSeries",
                LocalDate.now(), LocalTime.now(),
                20,
                0, 255, 0, 255, 0, 255
        );

        assertTrue(r != null, "analyzer returns non-null result");
        assertTrue(r.lesionPixelCount == 16, "synthetic 4x4 region has 16 pixels");
        assertTrue(r.lesionPixelCount == redPixelCount, "lesionPixelCount matches mask");

        // all selected pixels are exactly (200,50,50), so avg should equal that
        // and variance should be 0
        assertTrue(Math.abs(r.avgR - 200.0) < 1e-6, "avgR is exactly 200 for uniform region");
        assertTrue(Math.abs(r.avgG - 50.0) < 1e-6, "avgG is exactly 50 for uniform region");
        assertTrue(Math.abs(r.avgB - 50.0) < 1e-6, "avgB is exactly 50 for uniform region");
        assertTrue(r.varR < 1e-6 && r.varG < 1e-6 && r.varB < 1e-6,
                "variance is 0 for uniform-color region");

        // a 4x4 block has 2x2=4 interior pixels and 16-4=12 boundary pixels
        // under the 4-neighbor rule
        assertTrue(r.perimeterPixelCount == 12,
                "4x4 solid block has 12 boundary pixels under 4-neighborhood rule");
    }

    // checks that RgbRange clamps out-of-bounds values to [0,255]
    // and that the contains check works correctly.
    static void testRgbRangeClamping() {

        // values below 0 should clamp to 0, above 255 should clamp to 255
        RgbRange r = new RgbRange(-50, 300, -10, 100, 50, 999);

        assertTrue(r.minR == 0, "minR clamped from -50 to 0");
        assertTrue(r.maxR == 255, "maxR clamped from 300 to 255");
        assertTrue(r.minG == 0, "minG clamped from -10 to 0");
        assertTrue(r.maxG == 100, "maxG unchanged at 100");
        assertTrue(r.minB == 50, "minB unchanged at 50");
        assertTrue(r.maxB == 255, "maxB clamped from 999 to 255");

        // contains: pixel inside all three ranges
        assertTrue(r.contains(128, 50, 200), "pixel inside all bounds is contained");

        // contains: pixel just outside green upper bound
        assertTrue(!r.contains(128, 101, 200), "pixel above maxG is not contained");

        // contains: pixel exactly on a boundary (inclusive)
        assertTrue(r.contains(0, 100, 255), "pixels on boundaries are contained (inclusive)");
    }

    // checks the around() helper builds a symmetric range around a seed pixel
    // and clamps correctly when the tolerance pushes past the [0,255] limits.
    static void testRgbRangeAround() {

        RgbRange r1 = RgbRange.around(100, 150, 200, 20);
        assertTrue(r1.minR == 80 && r1.maxR == 120, "around() builds symmetric R range");
        assertTrue(r1.minG == 130 && r1.maxG == 170, "around() builds symmetric G range");
        assertTrue(r1.minB == 180 && r1.maxB == 220, "around() builds symmetric B range");

        // edge case: seed near 0 with large tolerance
        RgbRange r2 = RgbRange.around(10, 10, 10, 50);
        assertTrue(r2.minR == 0, "around() clamps below 0 to 0");
        assertTrue(r2.maxR == 60, "around() upper bound unchanged");

        // edge case: seed near 255 with large tolerance
        RgbRange r3 = RgbRange.around(250, 250, 250, 50);
        assertTrue(r3.minR == 200, "around() lower bound unchanged near 255");
        assertTrue(r3.maxR == 255, "around() clamps above 255 to 255");
    }

    // checks that Comparer correctly reports a non-zero difference when
    // the two results actually have different metrics. the existing
    // testCompareLatestTwo only verifies that compare runs at all, not
    // that the diff arithmetic is correct.
    static void testCompareDetectsRealDifference() {

        java.util.List<AnalysisResult> two = new java.util.ArrayList<>();

        // earlier observation: smaller lesion, darker color
        two.add(new AnalysisResult(
                "earlier.png", "diffSeries",
                LocalDate.of(2026, 1, 1), LocalTime.of(10, 0),

                100, 30, 0.50,
                80.0, 70.0, 60.0,
                5.0, 5.0, 5.0,

                20, 0, 255, 0, 255, 0, 255,
                "PASS"
        ));

        // later observation: bigger lesion, lighter color
        two.add(new AnalysisResult(
                "later.png", "diffSeries",
                LocalDate.of(2026, 1, 8), LocalTime.of(10, 0),

                150, 40, 0.45,
                120.0, 110.0, 100.0,
                7.0, 7.0, 7.0,

                20, 0, 255, 0, 255, 0, 255,
                "PASS"
        ));

        String text = Comparer.compareLatestTwo(two);

        assertTrue(text.contains("50"),
                "comparison text reports +50 pixel area difference");
        assertTrue(text.contains("larger"),
                "comparison text describes the lesion as having become larger");
        assertTrue(text.contains("lighter"),
                "comparison text describes the lesion as having become lighter");
    }

    // verifies that welford's algorithm produces the same variance
    // values as the two-pass method used in LesionAnalyzer, within numerical
    // tolerance. this justifies the two-pass choice in the report by showing
    // that the alternative implementation is equivalent for the project's
    // input scale.
    static void testWelfordMatchesTwoPass() {
        // generate a deterministic synthetic dataset of RGB samples
        int[] redValues   = {120, 135, 128, 142, 118, 150, 133, 127, 145, 130};
        int[] greenValues = { 80,  92,  85,  88,  78,  95,  89,  83,  90,  87};
        int[] blueValues  = { 50,  65,  58,  60,  52,  70,  63,  55,  67,  61};

        // two-pass method (matching LesionAnalyzer logic)
        int n = redValues.length;
        double sumR = 0, sumG = 0, sumB = 0;
        for (int i = 0; i < n; i++) {
            sumR += redValues[i];
            sumG += greenValues[i];
            sumB += blueValues[i];
        }
        double meanR = sumR / n;
        double meanG = sumG / n;
        double meanB = sumB / n;

        double sumSqR = 0, sumSqG = 0, sumSqB = 0;
        for (int i = 0; i < n; i++) {
            double dr = redValues[i] - meanR;
            double dg = greenValues[i] - meanG;
            double db = blueValues[i] - meanB;
            sumSqR += dr * dr;
            sumSqG += dg * dg;
            sumSqB += db * db;
        }
        double twoPassVarR = sumSqR / n;
        double twoPassVarG = sumSqG / n;
        double twoPassVarB = sumSqB / n;

        // welford's single-pass method
        Statistics.RgbWelford welford = new Statistics.RgbWelford();
        for (int i = 0; i < n; i++) {
            welford.add(redValues[i], greenValues[i], blueValues[i]);
        }
        double welfordVarR = welford.r.getVariance();
        double welfordVarG = welford.g.getVariance();
        double welfordVarB = welford.b.getVariance();

        // compare: results should match within floating-point tolerance
        double tolerance = 1e-9;
        boolean meanMatches = Math.abs(welford.r.getMean() - meanR) < tolerance
                && Math.abs(welford.g.getMean() - meanG) < tolerance
                && Math.abs(welford.b.getMean() - meanB) < tolerance;
        boolean varianceMatches = Math.abs(welfordVarR - twoPassVarR) < tolerance
                && Math.abs(welfordVarG - twoPassVarG) < tolerance
                && Math.abs(welfordVarB - twoPassVarB) < tolerance;

        assertTrue(meanMatches && varianceMatches, "welford matches two-pass variance");
    }
}

