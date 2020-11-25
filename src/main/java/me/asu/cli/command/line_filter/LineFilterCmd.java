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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Data;
import me.asu.cli.command.util.ResourcesFiles;
import me.asu.tui.framework.api.CliCommand;
import me.asu.tui.framework.api.CliConsole;
import me.asu.tui.framework.api.CliContext;
import me.asu.tui.framework.util.CliArguments;
import me.asu.tui.framework.util.CliCmdLineOption;
import me.asu.tui.framework.util.CliCmdLineParser;
import me.asu.util.Strings;

/**
 * 按行过滤文本
 */
public class LineFilterCmd implements CliCommand
{

    private static final String     NAMESPACE  = "asu";
    private static final String     CMD_NAME   = "line-filter";
    private static final Descriptor DESCRIPTOR = new InnerDescriptor();

    @Override
    public Descriptor getDescriptor()
    {
        return DESCRIPTOR;
    }

    @Override
    public Object execute(CliContext ctx, String[] args)
    {
        CliConsole c = ctx.getCliConsole();

        CliArguments arguments = null;
        try {
            arguments = DESCRIPTOR.parse(args);
        } catch (Exception e) {
            DESCRIPTOR.printUsage(c);
            return null;
        }
        if (arguments.hasParam("h") || !arguments.hasRemain() || !arguments.hasParam("b")) {
            DESCRIPTOR.printUsage(c);
            return null;
        }

        String delimiter = arguments.getParam("d");
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

        try (Stream<String> lines = Files.lines(inputPath, encoding);) {
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

        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(outputPathIn)) {
            in.forEach(w -> write(bufferedWriter, w));
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(outputPathNotIn)) {
            notIn.forEach(w -> write(bufferedWriter, w));
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    String pickUpWord(String w, int columnIndex, String splitter)
    {
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

    private Set<String> loadBaseSet(CliArguments arguments)
    {
        Charset encoding = getEncoding(arguments);
        Charset baseFileEncoding = getBaseEncoding(arguments, encoding);

        String baseFile = arguments.getParam("b");
        return new HashSet<>(
                ResourcesFiles.readLinesInResources(baseFile, baseFileEncoding.name()));
    }

    private void write(BufferedWriter bufferedWriter, String w)
    {
        try {
            bufferedWriter.write(w);
            bufferedWriter.write(System.getProperty("line.separator"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getColumn(CliArguments arguments)
    {
        int column = 0;
        if (arguments.hasParam("c")) {
            try {
                column = Integer.parseInt(arguments.getParam("c"));
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

    private Charset getBaseEncoding(final CliArguments arguments, final Charset encoding)
    {
        Charset e = encoding;
        if (arguments.hasParam("be")) {
            e = Charset.forName(arguments.getParam("be"));
        }
        return e;
    }

    private Path getInputputPath(CliArguments arguments)
    {
        return Paths.get(arguments.getRemain().get(0)).toAbsolutePath();
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

    static class InnerDescriptor implements Descriptor
    {

        CliCmdLineParser parser = new CliCmdLineParser();

        InnerDescriptor()
        {
            CliCmdLineOption opt1 = CliCmdLineOption.builder()
                                                    .shortName("c")
                                                    .hasArg(true)
                                                    .description("按照 n 行匹配，默认值是 0。")
                                                    .build();
            CliCmdLineOption opt2 = CliCmdLineOption.builder()
                                                    .shortName("d")
                                                    .hasArg(true)
                                                    .description("行分隔符号。")
                                                    .build();
            CliCmdLineOption opt3 = CliCmdLineOption.builder()
                                                    .shortName("e")
                                                    .hasArg(true)
                                                    .description("文件字符编码，默认为 UTF-8。")
                                                    .build();
            CliCmdLineOption opt4 = CliCmdLineOption.builder()
                                                    .shortName("h")
                                                    .longName("help")
                                                    .description("打印帮助信息。")
                                                    .build();
            CliCmdLineOption opt5 = CliCmdLineOption.builder()
                                                    .shortName("b")
                                                    .hasArg(true)
                                                    .description("比对文件。")
                                                    .build();
            CliCmdLineOption opt6 = CliCmdLineOption.builder()
                                                    .shortName("be")
                                                    .hasArg(true)
                                                    .description("比对文件字符编码。")
                                                    .build();

            parser.addOption(opt1, opt2, opt3, opt4, opt5, opt6);
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
            return "按行过滤文本。";
        }


    }

}
