// this class checks if the output looks valid and stable enough
// it helps us define "acceptance criteria" for the feature
public class AcceptanceCriteria {

    public static String validate(boolean[][] selected,
                                  int imageW,
                                  int imageH,
                                  AnalysisResult result,
                                  int selectedCount) {

        // 1) must have a selection mask
        if (selected == null) return "FAIL: selection mask is null";

        // 2) bounded growth: mask should match the image size
        if (selected.length != imageH) return "FAIL: mask height mismatch";
        for (int y = 0; y < imageH; y++) {
            if (selected[y] == null || selected[y].length != imageW) {
                return "FAIL: mask width mismatch at row " + y;
            }
        }

        // 3) non-zero segmentation (region must exist)
        if (selectedCount <= 0) return "FAIL: no pixels selected";
        if (result == null) return "FAIL: result is null";
        if (result.lesionPixelCount <= 0) return "FAIL: lesionPixelCount is 0";

        // 4) "bounded" growth: should not explode to most of the image
        // (this number is a simple safety rule, you can tune it later)
        int maxAllowed = (int)(imageW * imageH * 0.80);
        if (selectedCount > maxAllowed) return "FAIL: region too large (possible leak)";

        // 5) metrics should be valid (not NaN / not crazy)
        if (Double.isNaN(result.avgR) || Double.isNaN(result.avgG) || Double.isNaN(result.avgB)) {
            return "FAIL: NaN averages";
        }
        if (result.avgR < 0 || result.avgR > 255) return "FAIL: avgR out of range";
        if (result.avgG < 0 || result.avgG > 255) return "FAIL: avgG out of range";
        if (result.avgB < 0 || result.avgB > 255) return "FAIL: avgB out of range";

        return "PASS";
    }
}
