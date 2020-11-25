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
package me.asu.cli.command.code;

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
import me.asu.tui.framework.util.CliArguments;
import me.asu.tui.framework.util.CliCmdLineOption;
import me.asu.tui.framework.util.CliCmdLineParser;
import me.asu.util.Strings;

/**
 * 对词汇进行编码
 */
public class CodeCmd implements CliCommand {

    private static final String NAMESPACE = "asu";
    private static final String     CMD_NAME   = "code";
    private static final Descriptor DESCRIPTOR = new InnerDescriptor();

    @Override
    public Descriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public Object execute(CliContext ctx, String[] args) {
        CliConsole c = ctx.getCliConsole();

        CodeContext codeCtx = new CodeContext();
        ArgumentsParser argumentsParser = new ArgumentsParser(args, c, codeCtx).invoke();
        if (argumentsParser.isError()) {
            return 1;
        }

        processInput(codeCtx);
        return 0;
    }

    private void processInput(CodeContext codeCtx) {
        try (Stream<String> lines = Files.lines(codeCtx.getInputPath(), codeCtx.getEncoding());
             BufferedWriter bufferedWriter = Files.newBufferedWriter(codeCtx.getOutputPath())
        ) {
            lines.forEach(line -> {
                if(Strings.isBlank(line) || line.charAt(0) == '#') {
                    return;
                }
                codeCtx.setPhrase(line);
                final List<String> words = new ArrayList<>();
                final List<String> keys = new ArrayList<>();
                g(words, keys, 0, codeCtx, bufferedWriter);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 递归处理
     * @param keys
     * @param words
     * @param start
     * @param codeContext
     * @param bufferedWriter
     */
    private void g(List<String> keys, List<String> words, int start,
                   CodeContext codeContext, BufferedWriter bufferedWriter) {
        if (start == codeContext.getPhrase().length()) {
            write(bufferedWriter, keys, words);
            return;
        }

        for(int i = start;
                i <  Math.min(codeContext.getPhrase().length(), start + codeContext.getMaxWordLength());
                i++) {
            String w = codeContext.getPhrase().substring(start, i + 1);
            if (codeContext.getWordMap().containsKey(w)) {
                for (String k : codeContext.getWordMap().get(w)) {
                    List<String> newKeys = new ArrayList<>(keys);
                    List<String> newWords = new ArrayList<>(words);

                    newKeys.add(k);
                    newWords.add(w);
                    g(newKeys, newWords, i + 1, codeContext, bufferedWriter);
                }
            }
        }

    }


    private void write(BufferedWriter bufferedWriter,List<String> keys, List<String> words) {
        try {
            String text = String.format("%s\t%s%n", join(words, ""), join(keys, " "));
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


    @Override
    public void plug(CliContext plug) {
        //descriptor = new DescriptorImpl();

    }

    @Override
    public void unplug(CliContext plug) {
        // nothing to do
    }

    @Data
    private class CodeContext {
        final Map<String, List<String>> wordMap = new HashMap<>();
        String phrase;
        int maxWordLength = 0;
        private Charset encoding;
        private Path outputPath;
        private Path inputPath;

        public CodeContext setWordMap(Map<String, List<String>> map) {
            wordMap.clear();
            maxWordLength = 0;
            if (map != null){
                wordMap.putAll(map);
            }
            for(List<String> l : map.values()) {
                if (l.size() > maxWordLength) {
                    maxWordLength = l.size();
                }
            }

            return this;
        }
        public CodeContext setPhrase(String p) {
            this.phrase = p;

            return this;
        }
    }

    static class InnerDescriptor implements Descriptor {
        CliCmdLineParser parser = new CliCmdLineParser();

        public InnerDescriptor()
        {

            CliCmdLineOption opt1 = CliCmdLineOption.builder().shortName("b").hasArg(true).description("单字编码文件，格式为：单字<TAB>编码。").build();
            CliCmdLineOption opt2 = CliCmdLineOption.builder().shortName("be").hasArg(true).description("单字编码文件字符编码。").build();
            CliCmdLineOption opt3 = CliCmdLineOption.builder().shortName("e").hasArg(true).description("文件字符编码，默认为 UTF-8。").build();
            CliCmdLineOption opt4 = CliCmdLineOption.builder().shortName("h").longName("help").description("打印帮助信息。").build();
            CliCmdLineOption opt5 = CliCmdLineOption.builder().shortName("he").description("使用内置的小鹤双拼编码文件").build();
            CliCmdLineOption opt6 = CliCmdLineOption.builder().shortName("py").description("使用内置的汉语全拼编码文件").build();
            CliCmdLineOption opt7 = CliCmdLineOption.builder().shortName("sc").description("使用速成编码文件").build();
            CliCmdLineOption opt8 = CliCmdLineOption.builder().shortName("wb").description("使用五笔编码文件").build();
            CliCmdLineOption opt9 = CliCmdLineOption.builder().shortName("cj").description("使用仓颉编码文件").build();

            parser.addOption(opt1, opt2, opt3, opt4, opt5, opt6, opt7, opt8, opt9);

        }

        @Override
        public CliCmdLineParser getCliCmdLineParser()
        {
            return parser;
        }

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
            return "对词汇进行编码";
        }
    }

    private class ArgumentsParser {

        private boolean error;
        private final String[] args;
        private final CliConsole c;
        private final CodeContext  codeCtx;

        public ArgumentsParser(String[] args, CliConsole c, CodeContext codeCtx) {
            this.args = args;
            this.c = c;
            this.codeCtx =codeCtx;
        }

        boolean isError() {
            return error;
        }



        public CodeContext getCodeCtx() {
            return codeCtx;
        }


        public ArgumentsParser invoke() {
            CliArguments arguments = null;
            try {
                arguments = DESCRIPTOR.parse(args);
            } catch (Exception e) {
                DESCRIPTOR.printUsage(c);
                error = true;
                return this;
            }
            if (arguments.hasParam("h")
                    || !hasMappingFile(arguments)
                    || !hasInputFile(arguments)) {
                DESCRIPTOR.printUsage(c);
                error = true;
                return this;
            }

            String delimiter = arguments.getParam("d");
            codeCtx.setEncoding(getEncoding(arguments));

            codeCtx.setOutputPath(Paths.get("coded.txt"));
            Map<String, List<String>> mapping;
            int column = getColumn(arguments);
            if (arguments.hasParam("b")) {
                String mappingFile = arguments.getParam("b");
                Charset baseFileEncoding = getMapFileEncoding(arguments, codeCtx.getEncoding());
                mapping = ResourcesFiles.loadAsMapList(mappingFile, baseFileEncoding.name());
                c.printf("编码文件： %s%n", mappingFile);
                c.printf("字符编码： %s%n", baseFileEncoding);
            } else if (arguments.hasParam("-py")) {
                mapping = ResourcesFiles.loadAsMapList("gbkpy.txt");
                c.printf("编码文件： 使用内置全拼%n");
            } else if (arguments.hasParam("-he")) {
                mapping = ResourcesFiles.loadAsMapList("he.txt");
                c.printf("编码文件： 使用内置小鹤双拼%n");
            } else if (arguments.hasParam("-wb")) {
                mapping = ResourcesFiles.loadAsMapList("wubi.txt");
                c.printf("编码文件： 使用内置五笔%n");
            } else if (arguments.hasParam("-cj")) {
                mapping = ResourcesFiles.loadAsMapList("cj5-70000.txt");
                c.printf("编码文件： 使用内置仓颉%n");
            } else if (arguments.hasParam("-sc")) {
                mapping = ResourcesFiles.loadAsMapList("sc.txt");
                c.printf("编码文件： 使用内置速成%n");
            } else {
                DESCRIPTOR.printUsage(c);
                error = true;
                return this;
            }

            codeCtx.setWordMap(mapping);
            codeCtx.setInputPath(getInputPath(arguments));
            c.printf("处理文件： %s%n", codeCtx.getInputPath());
            c.printf("字符编码： %s%n", codeCtx.getEncoding());
            c.printf("行分隔符： %s%n", delimiter);
            c.printf("行： %d%n", column);
            c.printf("输出文件： %s%n", codeCtx.getOutputPath());
            c.printf("%n");
            c.flush();
            error = false;
            return this;
        }



        private boolean hasMappingFile(CliArguments arguments) {
            return arguments.hasParam("b") || arguments.hasParam("he") || arguments
                    .hasParam("py") || arguments.hasParam("wb") || arguments.hasParam("cj")
                    || arguments.hasParam("sc");
        }

        private boolean hasInputFile(CliArguments arguments) {
            return arguments.getRemain().size() > 0;
        }

        private Charset getEncoding(CliArguments arguments) {
            Charset encoding = StandardCharsets.UTF_8;
            if (arguments.hasParam("e")) {
                encoding = Charset.forName(arguments.getParam("e"));
            }
            return encoding;
        }

        private int getColumn(CliArguments arguments) {
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

        private Charset getMapFileEncoding(final CliArguments arguments, final Charset encoding) {
            Charset e = encoding;
            if (arguments.hasParam("be")) {
                e = Charset.forName(arguments.getParam("be"));
            }
            return e;
        }

        private Path getInputPath(CliArguments arguments) {
            return Paths.get(arguments.getRemain().get(0)).toAbsolutePath();
        }
    }
}
