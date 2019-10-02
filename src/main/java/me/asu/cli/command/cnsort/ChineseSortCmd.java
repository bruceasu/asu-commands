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
package me.asu.cli.command.cnsort;

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
import me.asu.tui.framework.api.*;

/**
 * 中文排序
 *
 * @author vvivien
 */
public class ChineseSortCmd implements Command {
    private static final String TYPE_PHRASE = "p";
    private static final String TYPE_SIMPLIFIED = "s";
    private static final String TYPE_TRADITIONAL = "t";

    private static final String NAMESPACE = "asu";
    private static final String CMD_NAME  = "cn-sort";
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
                !arguments.hasRemain()) {
            descriptor.printUsage(c);
            return null;
        }

        ChineseSearcher chineseSearcher = getChineseSearcher(arguments);
        Path outputPath = getOutputPath(arguments);
        Path inputPath = Paths.get(arguments.getRemain().get(0));
        String delimiter = arguments.getParam("-d");
        Charset encoding = getEncoding(arguments);
        int column = getColumn(arguments);

        c.printf("处理文件： %s%n", inputPath);
        c.printf("字符编码： %s%n", encoding);
        c.printf("行分隔符： %s%n", delimiter);
        c.printf("行： %d%n", column);

        c.printf("%n");
        c.flush();

        PriorityQueue<Word> words = new PriorityQueue<Word>(
                Comparator.comparingInt(Word::getScore));
        AtomicInteger count = new AtomicInteger();
        try(Stream<String> lines = Files.lines(inputPath, encoding);) {
            lines.forEach(line -> {
                Word w = new Word();
                w.setLine(line);
                int order = chineseSearcher.searchOrderByColumn(line, column, delimiter);
                w.setScore(order);
                words.add(w);
                int i =                 count.incrementAndGet();
                if (i%10000 == 0) {
                    c.printf("processing %d lines.%n", i);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        c.printf("处理了%d个字词。%n", words.size());
        c.printf("保存文件到：%s%n", outputPath);
        c.flush();
        try(BufferedWriter bufferedWriter = Files.newBufferedWriter(outputPath)) {
            words.forEach(w -> write(bufferedWriter, w));
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    private void write(BufferedWriter bufferedWriter, Word w) {
        try {
            bufferedWriter.write(w.getLine());
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

    private Path getOutputPath(Arguments arguments) {
        String output;
        if (arguments.hasParam("-o")) {
            output = arguments.getParam("-o");
        } else {
            output = arguments.getRemain().get(0) + ".out";
        }
        Path outPath = Paths.get(output).toAbsolutePath();
        if (!Files.exists(outPath.getParent())) {
            try {
                Files.createDirectories(outPath.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return outPath;
    }

    private ChineseSearcher getChineseSearcher(Arguments arguments) {
        ChineseSearcher chineseSearcher;
        String type = null;
        if (arguments.hasParam("-p")) {
            type = TYPE_PHRASE;
        } else if (arguments.hasParam("-s")) {
            type = TYPE_SIMPLIFIED;
        } else if (arguments.hasParam("-t")) {
            type = TYPE_TRADITIONAL;
        }
        if (arguments.hasParam("-type")) {
            type = arguments.getParam("type");
        }

        switch (type) {
            case TYPE_PHRASE:
                chineseSearcher = Orders.getPhrasesSearcher();
                break;
            case TYPE_TRADITIONAL:
                chineseSearcher = Orders.getTraditionChineseSearcher();
                break;
            case TYPE_SIMPLIFIED:
            default:
                chineseSearcher = Orders.getSimplifiedChineseSearcher();
                break;
        }
        return chineseSearcher;
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
                case "-o":
                    if (i+1 >= args.length) {
                        throw new RuntimeException(args[i] + " 需要一个参数");
                    }
                    arguments.setParam(args[i], args[i+1]);
                    i++;
                    break;
                case "-type":
                    if (i+1 >= args.length) {
                        throw new RuntimeException(args[i] + " 需要一个参数");
                    }
                    arguments.setParam(args[i], args[i+1]);
                    i++;
                    break;
                case "-h":
                    arguments.setParam(args[i], "true");
                    break;
                case "-p":
                    arguments.setParam(args[i], "true");
                    break;
                case "-s":
                    arguments.setParam(args[i], "true");
                    break;
                case "-t":
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

    private class DescriptorImpl implements Command.Descriptor {

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
            return "中文字频排序";
        }

        @Override
        public String getUsage() {
            StringBuilder result = new StringBuilder();
            result.append(Configurator.VALUE_LINE_SEP).append(getName()).append(" [选项] <输入文件>")
                  .append(Configurator.VALUE_LINE_SEP);

            return result.toString();
        }

        @Override
        public Map<String, String> getArguments() {
            Map<String, String> result = new TreeMap<>();
            result.put("-c", "按照 n 行排序，默认值是 0。");
            result.put("-d", "行分隔符号。");
            result.put("-e", "文件字符编码，默认为 UTF-8。");
            result.put("-h", "打印帮助信息。");
            result.put("-o", "结果输出文件。");
            result.put("-type", "排序类型, 默认是简体字字频。\n"
                    + "\tp: 简体词组词频\n"
                    + "\ts: 简体字字频\n"
                    + "\tt: 繁体字字频");
            result.put("-p", "等同 -type p.");
            result.put("-s", "等同 -type s.");
            result.put("-t", "等同 -type t.");
            return result;
        }

    }

}
