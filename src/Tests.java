import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


// test runner (printing pass and fail)
public class Tests {

    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) {

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

    // this checks that save creates files and loadAll returns something
    static void testSaveAndLoad() {

        AnalysisResult r = new AnalysisResult(
                "dummy.png",
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

}