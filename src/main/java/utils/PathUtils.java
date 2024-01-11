package utils;

import java.io.File;
import java.io.FileFilter;

public class PathUtils {

    public static String removeFileExtension(String fileName) {
        if (fileName == null) return fileName;

        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 1)
            return fileName;
        else
            return fileName.substring(0,dotIndex);
    }

    public static File findFileInFolderTree(String fileName, File rootFolder) {
        if (rootFolder.isDirectory()) {
            File targetFile = new File(rootFolder.getPath() + File.separator + fileName);
            if (targetFile.exists()) return targetFile;
            File[] files = rootFolder.listFiles();
            if (files == null) return null;
            for (File file : files) {
                targetFile = findFileInFolderTree(fileName, file);
                if (targetFile != null) return targetFile;
            }
        }
        return null;
    }
}
