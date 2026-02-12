import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// this is for saving + loading results
public class ResultStorage {

    private static File buildResultFile(LocalDate date) {
        File dir = new File("data" + File.separator + date.toString());
        if (!dir.exists()) {
            // create folder for that day
            dir.mkdirs();
        }

        String fileName = "result_" + java.time.LocalTime.now().toString().replace(':', '-') + ".txt";
        return new File(dir, fileName);
    }

    // save one analysis result to a new .txt file with a timestamp
    public static File save(AnalysisResult result) {

        if (result == null) return null;

        try {
            File file = buildResultFile(result.date);

            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                bw.write(result.toTextBlock());
                bw.newLine();
            }

            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // load all saved results from the data/ folder and is for comparison
    public static List<AnalysisResult> loadAll() {
        List<AnalysisResult> results = new ArrayList<>();

        File dataDir = new File("data");
        if (!dataDir.exists() || !dataDir.isDirectory()) return results;

        File[] dayDirs = dataDir.listFiles();
        if (dayDirs == null) return results;

        for (File dayDir : dayDirs) {
            if (!dayDir.isDirectory()) continue;

            File[] files = dayDir.listFiles();
            if (files == null) continue;

            for (File f : files) {
                if (!f.isFile()) continue;
                if (!f.getName().toLowerCase().endsWith(".txt")) continue;

                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                AnalysisResult r = AnalysisResult.fromTextBlock(sb.toString());
                if (r != null) results.add(r);
            }
        }

        return results;
    }
}
