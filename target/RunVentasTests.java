public class RunVentasTests {
    public static void main(String[] args) throws Exception {
        int passed = 0;
        for (String name : args) {
            Class<?> type = Class.forName(name);
            for (var method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
                    var constructor = type.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    method.setAccessible(true);
                    try {
                        method.invoke(constructor.newInstance());
                        System.out.println("PASS " + method.getName());
                        passed++;
                    } catch (java.lang.reflect.InvocationTargetException ex) {
                        throw new AssertionError(method.getName(), ex.getCause());
                    }
                }
            }
        }
        if (passed == 0) throw new AssertionError("No se ejecutaron pruebas");
        System.out.println(passed + " pruebas correctas");
    }
}
