import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// test runner (printing pass and fail)
public class Tests {

    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) {

        testSaveAndLoad();
        testCompareLatestTwo();

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
                10, 1.0, 2.0, 3.0
        );

        java.io.File saved = ResultStorage.save(r);
        assertTrue(saved != null && saved.exists(), "ResultStorage.save creates a txt file");

        List<AnalysisResult> all = ResultStorage.loadAll();
        assertTrue(all.size() >= 1, "ResultStorage.loadAll returns at least 1 result");
    }

    // this checks compare works when we have at least two results
    static void testCompareLatestTwo() {

        // create 2 fake results and save them
        AnalysisResult r1 = new AnalysisResult(
                "a.png",
                LocalDate.now(),
                LocalTime.now().withSecond(0).withNano(0),
                10, 10, 10, 10
        );

        // small delay so time differs
        AnalysisResult r2 = new AnalysisResult(
                "b.png",
                LocalDate.now(),
                LocalTime.now().withSecond(0).withNano(0).plusSeconds(1),
                20, 20, 20, 20
        );

        ResultStorage.save(r1);
        ResultStorage.save(r2);

        List<AnalysisResult> all = ResultStorage.loadAll();
        String text = Comparer.compareLatestTwo(all);

        assertTrue(text != null && text.length() > 0, "Comparer.compareLatestTwo returns text");
        assertTrue(text.contains("Comparing latest two"), "compare text contains header");
    }
}
