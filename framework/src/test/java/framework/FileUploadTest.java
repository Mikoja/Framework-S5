package framework;

import com.sprint.framework.exception.BindingException;
import framework.annotations.RequestParam;
import framework.config.FrameworkProperties;
import framework.file.FileStorage;
import framework.file.MaFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUploadTest {

    private final FrontControllerServlet frontController = new FrontControllerServlet();

    @Test
    void lieUnFichierParametre() throws Exception {
        Method method = getClass().getMethod("actionFichier", MaFile.class);
        byte[] data = "bonjour".getBytes();

        Object[] args = frontController.resolveArguments(method,
                multipartRequest(filePart("fichier", "photo.png", data)));

        MaFile file = (MaFile) args[0];
        assertEquals("photo.png", file.getFileName());
        assertEquals("png", file.getType());
        assertArrayEquals(data, file.getData());
        assertFalse(file.isEmpty());
    }

    @Test
    void fichierAbsentLeveBindingException() throws Exception {
        Method method = getClass().getMethod("actionFichier", MaFile.class);

        assertThrows(BindingException.class,
                () -> frontController.resolveArguments(method, multipartRequest()));
    }

    @Test
    void fichierOptionnelNullSiAbsent() throws Exception {
        Method method = getClass().getMethod("actionFichierOptionnel", MaFile.class);

        Object[] args = frontController.resolveArguments(method, multipartRequest());

        assertNull(args[0]);
    }

    @Test
    void requeteNonMultipartNeVoitPasDeFichier() throws Exception {
        Method method = getClass().getMethod("actionFichierOptionnel", MaFile.class);

        Object[] args = frontController.resolveArguments(method, formRequest());

        assertNull(args[0]);
    }

    @Test
    void champFichierDansObjetLie() throws Exception {
        byte[] data = "contenu".getBytes();

        ObjetFichier objet = (ObjetFichier) frontController.bindObject(ObjetFichier.class,
                multipartRequest(filePart("fichier", "cv.pdf", data)));

        assertEquals("cv.pdf", objet.getFichier().getFileName());
        assertEquals("pdf", objet.getFichier().getType());
    }

    @Test
    void champFichierAbsentLaisseNull() throws Exception {
        ObjetFichier objet = (ObjetFichier) frontController.bindObject(ObjetFichier.class, multipartRequest());

        assertNull(objet.getFichier());
    }

    @Test
    void ecritUnFichierSurDisque(@TempDir Path tempDir) throws Exception {
        FileStorage storage = new FileStorage(tempDir.toString());

        String stored = storage.save(new MaFile("note.txt", "contenu".getBytes()));

        assertEquals("contenu", Files.readString(Path.of(stored)));
    }

    @Test
    void neEcrasePasUnFichierExistant(@TempDir Path tempDir) throws Exception {
        FileStorage storage = new FileStorage(tempDir.toString());
        storage.save(new MaFile("note.txt", "un".getBytes()));

        String second = storage.save(new MaFile("note.txt", "deux".getBytes()));

        assertNotEquals("note.txt", Path.of(second).getFileName().toString());
        assertEquals("un", Files.readString(tempDir.resolve("note.txt")));
        assertEquals("deux", Files.readString(Path.of(second)));
    }

    @Test
    void chargeLeFichierDeConfiguration() throws Exception {
        FrameworkProperties props = FrameworkProperties.load("upload-test.properties");

        assertEquals("target/test-uploads", props.get("upload.path"));
        assertNull(props.get("inconnu"));
        assertEquals("defaut", props.get("inconnu", "defaut"));
    }

    @Test
    void developpeLesVariablesSysteme() throws Exception {
        FrameworkProperties props = FrameworkProperties.load("upload-test.properties");

        assertEquals(System.getProperty("java.io.tmpdir") + "/uploads", props.get("upload.tmp"));
    }

    @Test
    void configurationIntrouvableLeveIo() {
        assertThrows(java.io.IOException.class, () -> FrameworkProperties.load("absent.properties"));
    }

    @Test
    void maFileExtraitLExtension() {
        MaFile file = new MaFile("archive.TAR.GZ", "x".getBytes());

        assertEquals("gz", file.getType());
        assertTrue(new MaFile("sansextension", new byte[0]).isEmpty());
    }

    public void actionFichier(MaFile fichier) {
    }

    public void actionFichierOptionnel(@RequestParam(value = "fichier", required = false) MaFile fichier) {
    }

    public static class ObjetFichier {
        private MaFile fichier;

        public MaFile getFichier() {
            return fichier;
        }
    }

    private HttpServletRequest multipartRequest(Part... parts) {
        List<Part> partList = List.of(parts);
        return (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getContentType".equals(method.getName())) {
                        return "multipart/form-data; boundary=abc";
                    }
                    if ("getParts".equals(method.getName())) {
                        return partList;
                    }
                    return defaultValue(proxy, method, args);
                });
    }

    private HttpServletRequest formRequest() {
        Map<String, String> params = new HashMap<>();
        return (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getContentType".equals(method.getName())) {
                        return "application/x-www-form-urlencoded";
                    }
                    if ("getParameter".equals(method.getName())) {
                        return params.get((String) args[0]);
                    }
                    return defaultValue(proxy, method, args);
                });
    }

    private Part filePart(String name, String fileName, byte[] data) {
        return (Part) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Part.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return name;
                    }
                    if ("getSubmittedFileName".equals(method.getName())) {
                        return fileName;
                    }
                    if ("getInputStream".equals(method.getName())) {
                        return new java.io.ByteArrayInputStream(data);
                    }
                    return defaultValue(proxy, method, args);
                });
    }

    private Object defaultValue(Object proxy, Method method, Object[] args) {
        if ("hashCode".equals(method.getName())) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(method.getName())) {
            return proxy == args[0];
        }
        if ("toString".equals(method.getName())) {
            return "Fake";
        }
        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        return null;
    }
}
