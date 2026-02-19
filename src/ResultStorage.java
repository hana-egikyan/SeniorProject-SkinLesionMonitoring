import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// this is for saving + loading results
public class ResultStorage {

    // tries to create a folder if it does not exist
    // returns true if the folder exists after this (created or already existed)
    private static boolean ensureDirExists(File dir) {
        if (dir == null) return false;

        if (dir.exists()) {
            return dir.isDirectory();
        }

        // mkdirs returns true if it successfully created the directory
        // or false if it failed
        return dir.mkdirs();
    }

    // simple error logging to a text file (no external libraries)
    private static void logError(String message, Exception e) {
        System.err.println(message);
        if (e != null) System.err.println(e.getMessage());

        File logFile = new File("data" + File.separator + "errors.log");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true))) {
            bw.write("[" + java.time.LocalDateTime.now() + "] " + message);
            bw.newLine();

            if (e != null) {
                bw.write("Exception: " + e);
                bw.newLine();
            }

            bw.newLine();
        } catch (IOException ignored) {
            // if logging fails we do not want the program to crash
        }
    }

    private static File buildResultFile(LocalDate date) {
        File dir = new File("data" + File.separator + date.toString());

        // create folder for that day (and check if it worked)
        boolean created = ensureDirExists(dir);
        if (!created) {
            logError("Could not create folder: " + dir.getAbsolutePath(), null);
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
            if (parent != null && !ensureDirExists(parent)) {
                logError("Could not create parent folder: " + parent.getAbsolutePath(), null);
                return null;
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                bw.write(result.toTextBlock());
                bw.newLine();
            }

            return file;

        } catch (Exception e) {
            logError("Failed to save result.", e);
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
                    logError("Failed to read file: " + f.getAbsolutePath(), e);
                    continue;
                }

                AnalysisResult r = AnalysisResult.fromTextBlock(sb.toString());
                if (r != null) results.add(r);
            }
        }

        return results;
    }
}
