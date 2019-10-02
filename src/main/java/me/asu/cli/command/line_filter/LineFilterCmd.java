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
package me.asu.cli.command.line_filter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import lombok.Data;
import me.asu.cli.command.util.ResourcesFiles;
import me.asu.tui.framework.api.*;
import me.asu.util.Strings;

/**
 * 按行过滤文本
 */
public class LineFilterCmd implements Command {
    private static final String NAMESPACE = "asu";
    private static final String CMD_NAME  = "line-filter";
    private Descriptor descriptor;

    @Override
    public Descriptor getDescriptor() {
        return (descriptor != null) ? descriptor : (descriptor = new DescriptorImpl());
    }

    @Override
    public Object execute(Context ctx) {
        String[] args = (String[]) ctx.getValue(Context.KEY_COMMAND_LINE_ARGS);
        IoConsole c = ctx.getConsole();

        Arguments arguments = null;
        try {
            arguments = parseArguments(args);
        } catch (Exception e) {
            descriptor.printUsage(c);
            return null;
        }
        if (arguments.hasParam("-h") ||
                !arguments.hasRemain() ||
                !arguments.hasParam("-b")) {
            descriptor.printUsage(c);
            return null;
        }

        String delimiter = arguments.getParam("-d");
        Charset encoding = getEncoding(arguments);


        Path inputPath = getInputputPath(arguments);
        Path outputPathIn = Paths.get(inputPath.toString() + ".in");
        Path outputPathNotIn = Paths.get(inputPath.toString() + ".not-in");
        Set<String> baseSet = loadBaseSet(arguments);

        int column = getColumn(arguments);

        c.printf("处理文件： %s%n", inputPath);
        c.printf("字符编码： %s%n", encoding);
        c.printf("行分隔符： %s%n", delimiter);
        c.printf("行： %d%n", column);

        c.printf("%n");
        c.flush();

        List<String> in = new LinkedList<String>();
        List<String> notIn = new LinkedList<String>();


        try(Stream<String> lines = Files.lines(inputPath, encoding);) {
            lines.forEach(line -> {
                String s = pickUpWord(line, column, delimiter);
                if (baseSet.contains(s)) {
                    in.add(line);
                } else {
                    notIn.add(line);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        c.printf("过滤出%d个字词，清理了%d个字词。%n", in.size(), notIn.size());
        c.printf("保存过滤出字词文件到：%s%n", outputPathIn);
        c.printf("保存清理字词文件到：%s%n", outputPathNotIn);

        try(BufferedWriter bufferedWriter = Files.newBufferedWriter(outputPathIn)) {
            in.forEach(w -> write(bufferedWriter, w));
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try(BufferedWriter bufferedWriter = Files.newBufferedWriter(outputPathNotIn)) {
            notIn.forEach(w -> write(bufferedWriter, w));
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    String pickUpWord(String w, int columnIndex, String splitter) {
        if (Strings.isBlank(w)) {
            return "";
        }
        if (Strings.isBlank(splitter)) {
            return w;
        }
        splitter = splitter.trim();
        w = w.trim();
        String[] split = w.split(splitter);
        if (columnIndex >= split.length) {
            columnIndex = split.length - 1;
        }
        if (columnIndex < 0) {
            columnIndex = 0;
        }
        return split[columnIndex];
    }

    private Set<String> loadBaseSet(Arguments arguments) {
        Charset encoding = getEncoding(arguments);
        Charset baseFileEncoding = getBaseEncoding(arguments, encoding);

        String baseFile = arguments.getParam("-b");
        return new HashSet<>(ResourcesFiles.readLinesInResources(baseFile, baseFileEncoding.name()));
    }

    private void write(BufferedWriter bufferedWriter, String w) {
        try {
            bufferedWriter.write(w);
            bufferedWriter.write(System.getProperty("line.separator"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getColumn(Arguments arguments) {
        int column = 0;
        if (arguments.hasParam("-c")) {
            try {
                column = Integer.parseInt(arguments.getParam("-c"));
            } catch (NumberFormatException e) {
               column  = 0;
            }
        }
        return column;
    }

    private Charset getEncoding(Arguments arguments) {
        Charset encoding = StandardCharsets.UTF_8;
        if (arguments.hasParam("-e")) {
           encoding = Charset.forName(arguments.getParam("-e"));
        }
        return encoding;
    }

    private Charset getBaseEncoding(final Arguments arguments, final Charset encoding) {
        Charset e = encoding;
        if (arguments.hasParam("-be")) {
            e = Charset.forName(arguments.getParam("-be"));
        }
        return e;
    }
    private Path getInputputPath(Arguments arguments) {
        return Paths.get(arguments.getRemain().get(0)).toAbsolutePath();
    }



    public Arguments parseArguments(String[] args) {
        Arguments arguments = new Arguments();
        if (args == null || args.length == 0) {
            return arguments;
        }

        for (int i = 0; i < args.length; i++) {
            switch(args[i]) {
                case "-c":
                    if (i+1 >= args.length) {
                        throw new RuntimeException(args[i] + " 需要一个参数");
                    }
                    arguments.setParam(args[i], args[i+1]);
                    i++;
                    break;
                case "-d":
                    if (i+1 >= args.length) {
                        throw new RuntimeException(args[i] + " 需要一个参数");
                    }
                    arguments.setParam(args[i], args[i+1]);
                    i++;
                    break;
                case "-e":
                    if (i+1 >= args.length) {
                        throw new RuntimeException(args[i] + " 需要一个参数");
                    }
                    arguments.setParam(args[i], args[i+1]);
                    i++;
                    break;
                case "-b":
                    if (i+1 >= args.length) {
                        throw new RuntimeException(args[i] + " 需要一个参数");
                    }
                    arguments.setParam(args[i], args[i+1]);
                    i++;
                    break;
                case "-be":
                    if (i+1 >= args.length) {
                        throw new RuntimeException(args[i] + " 需要一个参数");
                    }
                    arguments.setParam(args[i], args[i+1]);
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

    @Data
    private class Word {
        String line;
        int score;
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
            return "按行过滤文本。";
        }

        @Override
        public String getUsage() {
            StringBuilder result = new StringBuilder();
            result.append(Configurator.VALUE_LINE_SEP).append(getName()).append(" [options] <inputFile>")
                  .append(Configurator.VALUE_LINE_SEP);

            return result.toString();
        }

        @Override
        public Map<String, String> getArguments() {
            Map<String, String> result = new TreeMap<>();
            result.put("-c", "按照 n 行匹配，默认值是 0。");
            result.put("-d", "行分隔符号。");
            result.put("-e", "文件字符编码，默认为 UTF-8。");
            result.put("-h", "打印帮助信息。");
            result.put("-b", "比对文件。");
            result.put("-be", "比对文件字符编码。");
            return result;
        }

    }

}
