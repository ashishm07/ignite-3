package org.apache.ignite.runner;
import java.io.File;
import java.util.*;

public class ClassFinder {
    public static Set<Class<?>> findClasses(String basePackage) throws Exception {
        String projectRoot = System.getProperty("user.dir");
        String path = projectRoot + "/examples/java/build/classes/java/integrationTest/"
                + basePackage.replace('.', '/');
        Set<Class<?>> classes = new HashSet<>();
        findClassesRecursive(new File(path), basePackage, classes);
        return classes;
    }

    private static void findClassesRecursive(File dir, String pkg, Set<Class<?>> classes) throws Exception {
        if (dir == null || !dir.exists()) {
            System.out.println("Path not found: " + dir);
            return;
        }

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                findClassesRecursive(file, pkg + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String clsName = pkg + '.' + file.getName().replace(".class", "");
                classes.add(Class.forName(clsName));
            }
        }
    }
}
