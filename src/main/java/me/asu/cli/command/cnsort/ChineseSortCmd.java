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
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.Data;
import me.asu.tui.framework.api.CliCommand;
import me.asu.tui.framework.api.CliConfigurator;
import me.asu.tui.framework.api.CliConsole;
import me.asu.tui.framework.api.CliContext;
import me.asu.tui.framework.util.CliArguments;
import me.asu.tui.framework.util.CliCmdLineOption;
import me.asu.tui.framework.util.CliCmdLineParser;

/**
 * 中文排序
 *
 * @author vvivien
 */
public class ChineseSortCmd implements CliCommand
{

    private static final String TYPE_PHRASE      = "p";
    private static final String TYPE_SIMPLIFIED  = "s";
    private static final String TYPE_TRADITIONAL = "t";

    private static final String     NAMESPACE  = "asu";
    private static final String     CMD_NAME   = "cn-sort";
    private static final Descriptor DESCRIPTOR = new InnerDescriptor();

    @Override
    public Descriptor getDescriptor()
    {
        return DESCRIPTOR;
    }

    @Override
    public Object execute(CliContext ctx,  String[] args)
    {
        CliConsole c = ctx.getCliConsole();

        CliArguments arguments = null;
        try {
            arguments = DESCRIPTOR.parse(args);
        } catch (Exception e) {
            DESCRIPTOR.printUsage(c);
            return null;
        }
        if (arguments.hasParam("h") || !arguments.hasRemain()) {
            DESCRIPTOR.printUsage(c);
            return null;
        }

        ChineseSearcher chineseSearcher = getChineseSearcher(arguments);
        Path outputPath = getOutputPath(arguments);
        Path inputPath = Paths.get(arguments.getRemain().get(0));
        String delimiter = arguments.getParam("d");
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
        try (Stream<String> lines = Files.lines(inputPath, encoding);) {
            lines.forEach(line -> {
                Word w = new Word();
                w.setLine(line);
                int order = chineseSearcher.searchOrderByColumn(line, column, delimiter);
                w.setScore(order);
                words.add(w);
                int i = count.incrementAndGet();
                if (i % 10000 == 0) {
                    c.printf("processing %d lines.%n", i);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        c.printf("处理了%d个字词。%n", words.size());
        c.printf("保存文件到：%s%n", outputPath);
        c.flush();
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(outputPath)) {
            words.forEach(w -> write(bufferedWriter, w));
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    private void write(BufferedWriter bufferedWriter, Word w)
    {
        try {
            bufferedWriter.write(w.getLine());
            bufferedWriter.write(System.getProperty("line.separator"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getColumn(CliArguments arguments)
    {
        int column = 0;
        if (arguments.hasParam("-c")) {
            try {
                column = Integer.parseInt(arguments.getParam("-c"));
            } catch (NumberFormatException e) {
                column = 0;
            }
        }
        return column;
    }

    private Charset getEncoding(CliArguments arguments)
    {
        Charset encoding = StandardCharsets.UTF_8;
        if (arguments.hasParam("e")) {
            encoding = Charset.forName(arguments.getParam("e"));
        }
        return encoding;
    }

    private Path getOutputPath(CliArguments arguments)
    {
        String output;
        if (arguments.hasParam("o")) {
            output = arguments.getParam("o");
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

    private ChineseSearcher getChineseSearcher(CliArguments arguments)
    {
        ChineseSearcher chineseSearcher;
        String type = null;
        if (arguments.hasParam("p")) {
            type = TYPE_PHRASE;
        } else if (arguments.hasParam("s")) {
            type = TYPE_SIMPLIFIED;
        } else if (arguments.hasParam("t")) {
            type = TYPE_TRADITIONAL;
        }
        if (arguments.hasParam("type")) {
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





    @Override
    public void plug(CliContext plug)
    {
        //descriptor = new DescriptorImpl();

    }

    @Override
    public void unplug(CliContext plug)
    {
        // nothing to do
    }

    @Data
    private class Word
    {

        String line;
        int    score;
    }

    static class InnerDescriptor implements CliCommand.Descriptor
    {
        CliCmdLineParser parser = new CliCmdLineParser();

        InnerDescriptor()
        {

            CliCmdLineOption opt1 = CliCmdLineOption.builder().shortName("c").hasArg(true).description("按照 n 行匹配，默认值是 0。").build();
            CliCmdLineOption opt2 = CliCmdLineOption.builder().shortName("d").hasArg(true).description("行分隔符号。").build();
            CliCmdLineOption opt3 = CliCmdLineOption.builder().shortName("e").hasArg(true).description("文件字符编码，默认为 UTF-8。").build();
            CliCmdLineOption opt4 = CliCmdLineOption.builder().shortName("h").longName("help").description("打印帮助信息。").build();
            CliCmdLineOption opt5 = CliCmdLineOption.builder().shortName("o").longName("output").hasArg(true).description("比对结果输出文件。").build();
            CliCmdLineOption opt6 = CliCmdLineOption.builder().shortName("type").hasArg(true).description("排序类型, 默认是简体字字频。\n" + "\tp: 简体词组词频\n" + "\ts: 简体字字频\n" + "\tt: 繁体字字频").build();
            CliCmdLineOption opt7 = CliCmdLineOption.builder().shortName("p").description("等同 -type p。").build();
            CliCmdLineOption opt8 = CliCmdLineOption.builder().shortName("s").description("等同 -type s。").build();
            CliCmdLineOption opt9 = CliCmdLineOption.builder().shortName("t").description("等同 -type t。").build();

            parser.addOption(opt1, opt2, opt3, opt4, opt5, opt6, opt7, opt8, opt9);
        }
        @Override
        public CliCmdLineParser getCliCmdLineParser()
        {
            return parser;
        }

        @Override
        public String getNamespace()
        {
            return NAMESPACE;
        }

        @Override
        public String getName()
        {
            return CMD_NAME;
        }

        @Override
        public String getDescription()
        {
            return "中文字频排序";
        }



    }

}
