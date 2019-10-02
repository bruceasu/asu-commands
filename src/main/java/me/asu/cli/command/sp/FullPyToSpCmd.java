/*
 * Copyright 2012 ClamShell-Cli.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.asu.cli.command.sp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.Data;
import me.asu.cli.command.util.ResourcesFiles;
import me.asu.tui.framework.api.*;
import me.asu.util.Strings;

/**
 * 对词汇进行编码
 */
public class FullPyToSpCmd implements Command {

    private static final String NAMESPACE = "asu";
    private static final String CMD_NAME = "full2sp-he";
    private Descriptor descriptor;

    @Override
    public Descriptor getDescriptor() {
        return (descriptor != null) ? descriptor : (descriptor = new DescriptorImpl());
    }

    @Override
    public Object execute(Context ctx) {
        String[] args = (String[]) ctx.getValue(Context.KEY_COMMAND_LINE_ARGS);
        IoConsole c = ctx.getConsole();

        ArgumentsParser argumentsParser = new ArgumentsParser(args, c).invoke();
        if (argumentsParser.isError()) {
            return 1;
        }

        processInput(argumentsParser);
        return 0;
    }

    private void processInput(ArgumentsParser argumentsParser) {
        try (Stream<String> lines = Files.lines(argumentsParser.getInputPath(), argumentsParser.getEncoding());
             BufferedWriter bufferedWriter = Files.newBufferedWriter(argumentsParser.getOutputPath())
        ) {
            Map<String, String> table = makeTable();
            final AtomicInteger count = new AtomicInteger();
            lines.forEach(line -> {
                count.incrementAndGet();
                if(Strings.isBlank(line) || line.charAt(0) == '#') {
                    return;
                }
                List<String> mapping = new ArrayList<>(line.length());
                String[] split = line.split("\\s+");
                for (int i = 1; i < split.length; i++) {
                    if (table.containsKey(split[i])) {
                        mapping.add(table.get(split[i]));
                    } else {
                        System.out.println("split[i] is not exits:  " + split[i]);
                    }
                }
                if (mapping.size() == split.length - 1) {
                    write(bufferedWriter, mapping, split[0]);
                }
                if (count.get() % 1000 == 0) {
                    argumentsParser.c.printf("processing %d lines.%n", count.get());
                }
            });
            bufferedWriter.flush();
            argumentsParser.c.printf("processing %d lines.%n", count.get());
            argumentsParser.c.printf("保存到： %s%n", argumentsParser.getOutputPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(BufferedWriter bufferedWriter, List<String> keys, String words) {
        try {
            String text = String.format("%s\t%s%n", words, join(keys, " "));
            bufferedWriter.write(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String join(Collection<String> coll, String delimiter) {
        StringBuilder b = new StringBuilder();
        for (String s : coll) {
            b.append(s).append(delimiter);
        }
        if (b.length() > 0) {
            b.setLength(b.length() - delimiter.length());
        }
        return b.toString();
    }

    private Arguments parseArguments(String[] args) {
        Arguments arguments = new Arguments();
        if (args == null || args.length == 0) {
            return arguments;
        }

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-e":
                    if (i + 1 >= args.length) {
                        throw new RuntimeException(args[i] + " 需要一个参数");
                    }
                    arguments.setParam(args[i], args[i + 1]);
                    i++;
                    break;
                case "-h":
                    arguments.setParam(args[i], "true");
                    break;
                default:
                    arguments.addRemain(args[i]);
            }
        }
        return arguments;
    }

    @Override
    public void plug(Context plug) {
        //descriptor = new DescriptorImpl();
     }

    @Override
    public void unplug(Context plug) {
        // nothing to do
    }

    private Map<String, String> makeTable() {
        Map<String, String> s = new HashMap<>();
        Map<String, String> w = new HashMap<>();
        Map<String, String> m = new HashMap<>();

        s.put("q", "q");
        s.put("w", "w");
        s.put("r", "r");
        s.put("t", "t");
        s.put("y", "y");
        s.put("sh", "u");
        s.put("ch", "i");
        s.put("p", "p");
        s.put("s", "s");
        s.put("d", "d");
        s.put("f", "f");
        s.put("g", "g");
        s.put("h", "h");
        s.put("j", "j");
        s.put("k", "k");
        s.put("l", "l");
        s.put("z", "z");
        s.put("x", "x");
        s.put("c", "c");
        s.put("zh", "v");
        s.put("b", "b");
        s.put("n", "n");
        s.put("m", "m");

        w.put("iu", "q");
        w.put("ei", "w");
        w.put("e", "e");
        w.put("uan", "r");
        w.put("van", "r");
        w.put("ve", "t");
        w.put("ue", "t");
        w.put("üe", "t");
        w.put("un", "y");
        w.put("vn", "y");
        w.put("u", "u");
        w.put("i", "i");
        w.put("o", "o");
        w.put("uo", "o");
        w.put("ie", "p");

        w.put("a", "a");
        w.put("ong", "s");
        w.put("iong", "s");
        w.put("ai", "d");
        w.put("en", "f");
        w.put("eng", "g");
        w.put("ang", "h");
        w.put("an", "j");
        w.put("ing", "k");
        w.put("uai", "k");
        w.put("uang", "l");
        w.put("iang", "l");

        w.put("ou", "z");
        w.put("ua", "x");
        w.put("ia", "x");
        w.put("ao", "c");
        w.put("v", "v");
        w.put("ui", "v");
        w.put("in", "b");
        w.put("iao", "n");
        w.put("ian", "m");

        s.forEach((k, v) -> {
            w.forEach((x, y) -> {
                m.put(k + x, v + y);
            });
        });

        m.put("ang", "ag");
        m.put("eng", "eg");
        m.put("a", "aa");
        m.put("e", "ee");
        m.put("o", "oo");

        return m;
    }


    private class DescriptorImpl implements Descriptor {

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public String getName() {
            return CMD_NAME;
        }

        @Override
        public String getDescription() {
            return "全拼转双拼编码";
        }

        @Override
        public String getUsage() {
            StringBuilder result = new StringBuilder();
            result.append(Configurator.VALUE_LINE_SEP).append(getName())
                  .append(" [options] <inputFile>").append(Configurator.VALUE_LINE_SEP);

            return result.toString();
        }

        @Override
        public Map<String, String> getArguments() {
            Map<String, String> result = new TreeMap<>();
            result.put("-e", "文件字符编码，默认为 UTF-8。");
            result.put("-h", "打印帮助信息。");
            return result;
        }

    }

    private class ArgumentsParser {

        private boolean error;
        private final String[] args;
        private final IoConsole c;
        Arguments arguments = null;
        public ArgumentsParser(String[] args, IoConsole c) {
            this.args = args;
            this.c = c;
        }

        boolean isError() {
            return error;
        }


        public ArgumentsParser invoke() {

            try {
                arguments = parseArguments(args);
            } catch (Exception e) {
                descriptor.printUsage(c);
                error = true;
                return this;
            }
            if (arguments.hasParam("-h")
                    || !hasInputFile()) {
                descriptor.printUsage(c);
                error = true;
                return this;
            }

            Map<String, List<String>> mapping;

            c.printf("处理文件： %s%n", getInputPath());
            c.printf("字符编码： %s%n", getEncoding());
            c.printf("输出文件： %s%n", getOutputPath());
            c.printf("%n");
            c.flush();
            error = false;
            return this;
        }

        private Path getOutputPath() {
            return Paths.get(getInputPath().toAbsolutePath().toString() + ".sp");
        }


        public boolean hasInputFile() {
            return arguments.getRemain().size() > 0;
        }

        public Charset getEncoding() {
            Charset encoding = StandardCharsets.UTF_8;
            if (arguments.hasParam("-e")) {
                encoding = Charset.forName(arguments.getParam("-e"));
            }
            return encoding;
        }

        public Path getInputPath() {
            return Paths.get(arguments.getRemain().get(0)).toAbsolutePath();
        }
    }
}