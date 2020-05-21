package gcm.core.epi.util.configsplit;

import gcm.util.TimeElapser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A main method utility for joining experiment output directories into a single
 * directory containing concatenated output files with scenario renumbering.
 *
 * @author Shawn Hatch
 */
public class ExperimentJoiner {

    private final Path parentDirectory;

    private final Path targetDirectory;

    private final List<Path> childDirectories = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(ExperimentJoiner.class);

    private ExperimentJoiner(Path parentDirectory, Path targetDirectory) {
        this.parentDirectory = parentDirectory;
        this.targetDirectory = targetDirectory;
    }

    public static void main(String[] args) throws IOException {
        //The directory where the output folders are located
        Path parentDirectory = Paths.get(args[0]);

        //The directory where the joined output files should be written
        Path targetDirectory = Paths.get(args[1]);

        new ExperimentJoiner(parentDirectory, targetDirectory).execute();
    }

    private void clearTargetDirectory() {
        File dir = targetDirectory.toFile();

        for (File file : dir.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }

    private void determineChildDirectories() {
        for (File file : parentDirectory.toFile().listFiles()) {
            if (file.isDirectory()) {
                childDirectories.add(file.toPath());
            }
        }
    }

    private void transferLines(File file, boolean includeHeader) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            try (FileOutputStream fos = new FileOutputStream(targetDirectory.resolve(file.getName()).toFile(), true)) {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

                if (includeHeader) {
                    String line = br.readLine();
                    bw.write(line);
                    bw.newLine();
                } else {
                    br.readLine();
                }

                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    bw.write(line);
                    bw.newLine();
                }
                bw.flush();
            }
        }
    }

    private void execute() throws IOException {
        clearTargetDirectory();
        determineChildDirectories();

        // transfer the contents
        int progressCount = 0;

        boolean includeHeader = true;
        TimeElapser timeElapser = new TimeElapser();
        for (Path path : childDirectories) {
            for (File file : path.toFile().listFiles()) {
                if (file.getName().endsWith(".tsv")) {
                    transferLines(file, includeHeader);
                }
            }
            progressCount++;
            double projectedTimeRemaining = (timeElapser.getElapsedSeconds() / progressCount) * (childDirectories.size() - progressCount);
            includeHeader = false;
            logger.info("Progress: " + progressCount + " of " + childDirectories.size() + " with " + projectedTimeRemaining + " seconds remaining");
        }
    }
}
