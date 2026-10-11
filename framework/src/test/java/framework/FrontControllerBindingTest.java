package framework;

import com.sprint.framework.exception.BindingException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrontControllerBindingTest {

    private final FrontControllerServlet frontController = new FrontControllerServlet();

    @Test
    void remplitUnObjet() throws Exception {
        Employe employe = (Employe) frontController.bindObject(Employe.class,
                request("nom", "Dupont", "age", "30", "salaire", "1500.5"));

        assertEquals("DUPONT", employe.getNom());
        assertEquals(30, employe.getAge());
        assertEquals(1500.5, employe.getSalaire());
    }

    @Test
    void utiliseSetterPuisChampDirect() throws Exception {
        Employe employe = (Employe) frontController.bindObject(Employe.class,
                request("nom", "dupont", "ville", "Paris"));

        assertEquals("DUPONT", employe.getNom());
        assertEquals("Paris", employe.getVille());
    }

    @Test
    void remplissageParDefautSansParametres() throws Exception {
        Employe employe = (Employe) frontController.bindObject(Employe.class, request());

        assertNull(employe.getNom());
        assertNull(employe.getVille());
        assertEquals(0, employe.getAge());
        assertEquals(0.0, employe.getSalaire());
    }

    @Test
    void champNonSimpleIgnore() throws Exception {
        Employe employe = (Employe) frontController.bindObject(Employe.class,
                request("adresse", "quelque chose", "tags", "a,b"));

        assertNull(employe.getAdresse());
        assertNull(employe.getTags());
    }

    @Test
    void champsDeLaHierarchieInclus() throws Exception {
        Employe employe = (Employe) frontController.bindObject(Employe.class,
                request("code", "X42"));

        assertEquals("X42", employe.getCode());
    }

    @Test
    void absenceConstructeurSansArgumentLeveBindingException() {
        assertThrows(BindingException.class,
                () -> frontController.bindObject(SansConstructeur.class, request("nom", "Dupont")));
    }

    private HttpServletRequest request(String... pairs) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            params.put(pairs[i], pairs[i + 1]);
        }
        return (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getParameter".equals(method.getName())) {
                        return params.get((String) args[0]);
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "FakeRequest";
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    return null;
                });
    }

    public static class Parent {
        private String code;

        public String getCode() {
            return code;
        }
    }

    public static class Employe extends Parent {
        private String nom;
        private int age;
        private double salaire;
        private String ville;
        private Adresse adresse;
        private List<String> tags;

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom.toUpperCase();
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public double getSalaire() {
            return salaire;
        }

        public void setSalaire(double salaire) {
            this.salaire = salaire;
        }

        public String getVille() {
            return ville;
        }

        public Adresse getAdresse() {
            return adresse;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    public static class Adresse {
        private String rue;
    }

    public static class SansConstructeur {
        private String nom;

        public SansConstructeur(String nom) {
            this.nom = nom;
        }
    }
}
