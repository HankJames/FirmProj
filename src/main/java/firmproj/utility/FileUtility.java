package firmproj.utility;

import firmproj.main.ApkContext;
import firmproj.main.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class FileUtility {
    private static final Logger LOGGER = LogManager.getLogger(FileUtility.class);

    public static File getOutputFile(String outputPath) {
        File outputFile = new File(outputPath);
        if (outputFile.isDirectory()) {
            outputPath +=  "/" + ApkContext.getInstance().getPackageName() + ".txt";
            outputFile = new File(outputPath);
        }
        return outputFile;
    }

    public static List<String> getApksFromFile(String path) {
        List<String> result = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = "";
            while (line != null) {
                line = reader.readLine();
                result.add(line);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return result;
    }

    public static void initDirs(String outputPath) {
        File tmp = new File(outputPath);
        if (!tmp.exists()) {
            //LOGGER.info("creating tmp directory");
            if (!outputPath.endsWith(".json")) {
                tmp.mkdir();
            }
        }
        tmp = new File(Config.LOGDIR);
        if (!tmp.exists())
            tmp.mkdir();
    }


}
