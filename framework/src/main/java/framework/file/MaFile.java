package framework.file;

public class MaFile {

    private final String fileName;
    private final String type;
    private final byte[] data;

    public MaFile(String fileName, byte[] data) {
        this.fileName = fileName;
        this.type = extensionOf(fileName);
        this.data = data != null ? data : new byte[0];
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    public String getFileName() {
        return fileName;
    }

    public String getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isEmpty() {
        return fileName == null || fileName.isEmpty() || data.length == 0;
    }
}
