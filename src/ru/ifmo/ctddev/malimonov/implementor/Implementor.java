package ru.ifmo.ctddev.malimonov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Created by heat_wave on 03.03.15.
 */

public class Implementor implements JarImpler{

    /**
     * {@link Set} with full implementations of given methods
     */
    static Set<String> implementedMethods = new HashSet<String>();

    /**
     * {@link String} containing the file's directory
     */
    String fileDir;

    /**
     * Returns default return value for given method
     *
     * @param method an instance of {@link java.lang.reflect.Method}
     * @return default return value for <code>method</code>
     */
    public static String getDefaultReturnValue(Method method) {
        if (method.getReturnType().isPrimitive()) {
            if (method.getReturnType().equals(boolean.class)) {
                return " false";
            } else if (method.getReturnType().equals(void.class)) {
                return "";
            } else {
                return " 0";
            }
        }
        return " null";
    }

    /**
     * Converts Method to String that contains full description of the method with modifiers, return type and parameters
     *
     * @param method an instance of {@link java.lang.reflect.Method}
     * @return description of <code>method</code>
     */
    public static String getMethod(Method method) {
        if (!method.getReturnType().isPrimitive()) {
            String aux = method.getReturnType().getName();
        }
        return modifiersToString(method.getModifiers()) + method.getReturnType().getCanonicalName() + " " +
            method.getName() + "("  + getParameters(method) +  ") {return" + getDefaultReturnValue(method) + ";}";
    }

    /**
     * Returns a string containing all the parameters of a given method
     *
     * @param method an instance of {@link java.lang.reflect.Method}
     * @return {@link java.lang.String} with all its parameters
     */
    public static String getParameters(Method method) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Parameter parameter : method.getParameters()) {
            stringBuilder.append(getParameter(parameter)).append(", ");
        }
        if (stringBuilder.length() >= 2) {
            stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        }
        return stringBuilder.toString();
    }

    /**
     * Converts parameter to a string which contains all of the parameter's modifiers
     *
     * @param parameter an instance of {@link java.lang.reflect.Parameter}
     * @return full description of <code>parameter</code>
     */
    public static String getParameter(Parameter parameter) {
        return modifiersToString(parameter.getModifiers()) + parameter.getType().getCanonicalName() + " " + parameter.getName();
    }

    /**
     * Converts modifier constant to {@link java.lang.String}, except for Abstract and Transient modifiers
     *
     * @param modifiers a {@link java.lang.reflect.Modifier} constant
     * @return String <code>result</code> according to <code>modifiers</code>
     */
    public static String modifiersToString(int modifiers) {
        String result = Modifier.toString(modifiers & ((Modifier.ABSTRACT | Modifier.TRANSIENT) ^ Integer.MAX_VALUE));
        if (result.length() > 0) {
            result += " ";
        }
        return result;
    }

    /**
     * Implements an interface with a given token
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException describing the error
     */
    @Override
    public void implement(Class<?> token, File root) throws ImplerException {

        try {

            String pack;
            if (token.getPackage() == null) {

                pack = "";
                throw new ImplerException("Null package");
            }
            else {
                pack = "package " + token.getPackage().getName() + ";";
            }
            String sep = File.separator;
            fileDir = root.getAbsolutePath() + sep + token.getPackage().getName().replace(".", sep) + sep;

            if (!new File(fileDir).exists()) {
                if (!new File(fileDir).mkdirs()) {
                    throw new ImplerException("Wrong root, failed to create file.");
                }
            }

            PrintWriter out = new PrintWriter(new File(fileDir + token.getSimpleName() + "Impl.java"));

            Method[] methods = token.getMethods();
            String classLine = "public class " + token.getSimpleName() + "Impl implements " + token.getSimpleName() + " {";

            for (Method method : methods) {
                implementedMethods.add(getMethod(method));
            }

            out.println(pack);

            out.println(classLine);

            out.println();

            for (String method : implementedMethods) {
                out.println("    " + method);
            }

            out.println();
            out.print("}");

            out.flush();
        }
        catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Implements a class with given token in a jar form
     *
     * @param token type token to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
     * @throws ImplerException describing the error
     */
    public void implementJar(Class<?> token, File jarFile) throws ImplerException {
        if (token == null) {
            throw new NullPointerException("token is null!");
        }
        if (jarFile == null) {
            throw new NullPointerException("jarFile is null!");
        }

        File workingDir = new File(".");
        try {
            workingDir = Files.createTempDirectory("ImplTemp").toFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        implement(token, workingDir);
        String path;
        if (token.getPackage() != null) {
            path = token.getPackage().getName().replace(".", File.separator) + File.separator;
        } else {
            path = File.separator;
        }
        String name;
        if (token.getPackage() != null) {
            name = token.getPackage().getName() + ".";
        } else {
            name = ".";
        }
        int mam = compile(workingDir, workingDir.getAbsolutePath() +  File.separator + path + token.getSimpleName() + "Impl.java");
        System.out.println("# " + mam + "\n" + jarFile.getName() + "\n");
        createJar(name + token.getSimpleName() + "Impl", jarFile.getAbsolutePath(), path + token.getSimpleName() + "Impl.class", workingDir.getAbsolutePath());
    }

    /**
     * Compiles target file
     *
     * @param root file path
     * @param file file name
     * @return the exit code given by the compiler
     */
    private int compile(final File root, String file) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final List<String> args = new ArrayList<>();
        args.add(file);
        args.add("-cp");
        args.add(root.getPath() + File.pathSeparator + System.getProperty("java.class.path"));
        return compiler.run(null, null, null, args.toArray(new String[args.size()]));
    }

    /**
     * Creates a jar archive
     *
     * @param fullName full name
     * @param jarName archive name
     * @param filePath file path
     * @param workingDir working directory
     */

    private static void createJar(String fullName, String jarName, String filePath, String workingDir) {
        System.out.println(fullName + "\n" + jarName + "\n" + filePath);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        //manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, fullName);
        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarName), manifest);
             FileInputStream fileInputStream = new FileInputStream(workingDir + File.separator + filePath)) {

            jarOutputStream.putNextEntry(new ZipEntry(filePath));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fileInputStream.read(buffer)) > 0) {
                jarOutputStream.write(buffer, 0, length);
            }
            jarOutputStream.closeEntry();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * An entry point necessary to compile our jar archive
     *
     * @param args default args of any main
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            throw new IllegalArgumentException("Not enough arguments!");
        }
        Class<?> clazz;
        try {
            clazz = Class.forName(args[0]);
            (new Implementor()).implementJar(clazz, new File("./out.jar"));
        } catch (ClassNotFoundException e) {
            System.err.println(e.toString());
        } catch (ImplerException e) {
            e.printStackTrace();
        }
    }

}
