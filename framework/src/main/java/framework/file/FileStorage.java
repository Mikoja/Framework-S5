package framework.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileStorage {

    private final String uploadPath;

    public FileStorage(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public String save(MaFile file) throws IOException {
        Path dir = Paths.get(uploadPath);
        Files.createDirectories(dir);

        String fileName = Paths.get(file.getFileName()).getFileName().toString();
        String base = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            base = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }

        Path target = dir.resolve(fileName);
        for (int i = 1; Files.exists(target); i++) {
            target = dir.resolve(base + "-" + i + extension);
        }
        Files.write(target, file.getData());
        return target.toString();
    }
}
