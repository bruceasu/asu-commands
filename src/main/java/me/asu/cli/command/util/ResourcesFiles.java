package me.asu.cli.command.util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.*;
import me.asu.util.io.Files;
import me.asu.util.io.Streams;

/**
 * Created by suk on 2019/6/2.
 */
public class ResourcesFiles {

    public static List<String> orders() {
        return ResourcesFiles.readLinesInResources("sort-order.txt");
    }

    public static List<String> ordersT() {
        return ResourcesFiles.readLinesInResources("sort-order-t.txt");
    }

    public static List<String> ordersPhrases() {
        return ResourcesFiles.readLinesInResources("sort-order-phrases.txt");
    }

    public static Map<String, String> loadAsMap(String name) {
        List<String> strings = readLinesInResources(name);
        Map<String, String> map = new HashMap<>();
        for (String line : strings) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] split = line.split("\\s+");
            if (split.length < 2) {
                continue;
            }
            map.put(split[0], split[1]);
        }
        return map;
    }

    public static Map<String, List<String>> loadAsMapList(String name) {
        List<String> strings = readLinesInResources(name);
        Map<String, List<String>> map = toMapList(strings);
        return map;
    }

    public static Map<String, List<String>> loadAsMapList(String name, String encoding) {
        List<String> strings = readLinesInResources(name, encoding);
        Map<String, List<String>> map = toMapList(strings);
        return map;
    }

    public static Map<String, List<String>> toMapList(List<String> strings) {
        Map<String, List<String>> map = new HashMap<>();
        for (String line : strings) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] split = line.split("\\s+");
            if (split.length < 2) {
                continue;
            }
            if (map.containsKey(split[0])) {
                map.get(split[0]).add(split[1]);
            } else {
                List<String> ls = new ArrayList<>();
                ls.add(split[1]);
                map.put(split[0], ls);
            }
        }
        return map;
    }

    public static List<String> readLinesInResources(String name) {

        File file = new File(name);
        if (!file.exists()) {
            file = new File("resources", name);
        }
        if (!file.exists()) {
            String myDIR = OsHelper.getMyDIR();
            file = new File(myDIR, name);
        }
        if (!file.exists()) {
            String myDIR = OsHelper.getMyDIR();
            file = new File(myDIR, "resources" + File.separator + name);
        }
        if (!file.exists()) {
            // try classpath
            InputStream in = ResourcesFiles.class
                    .getClassLoader()
                    .getResourceAsStream(name);
            if (in == null) {
                return Collections.emptyList();
            }
            return Streams.readLinesAndClose(in);
        } else {
            return Files.readLines(file);
        }
    }

    public static List<String> readLinesInResources(String name, String charset) {
        File file = new File(name);
        if (!file.exists()) {
            file = new File("resources", name);
        }
        if (!file.exists()) {
            String myDIR = OsHelper.getMyDIR();
            file = new File(myDIR, name);
        }
        if (!file.exists()) {
            String myDIR = OsHelper.getMyDIR();
            file = new File(myDIR, "resources" + File.separator + name);
        }
        if (!file.exists()) {
            // try classpath
            InputStream in = ResourcesFiles.class
                    .getClassLoader()
                    .getResourceAsStream(name);
            if (in == null) {
                return Collections.emptyList();
            }
            try {
                return Streams.readLinesAndClose(new InputStreamReader(in, charset));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }
        return Files.readLines(file, charset);
    }
}
