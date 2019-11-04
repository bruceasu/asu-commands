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
package me.asu.cli.command.unique;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;
import lombok.Data;
import me.asu.cli.command.util.ResourcesFiles;
import me.asu.tui.framework.api.*;
import me.asu.util.Strings;

/**
 * 按行删除重复文本
 */
public class LineUniqueCmd implements Command {
    private static final String NAMESPACE = "asu";
    private static final String CMD_NAME  = "line-unique";
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
        if (arguments.hasParam("-h")|| !arguments.hasRemain() ) {
            descriptor.printUsage(c);
            return null;
        }
        List<String> remain = arguments.getRemain();
        remain.forEach(inputFile -> {
            Path inputPath = Paths.get(inputFile);
            Path outputPath = Paths.get(inputFile + ".tmp");
            try {
                process(c, inputPath, outputPath);
                Files.move(outputPath, inputPath, StandardCopyOption.REPLACE_EXISTING);
                c.printf("处理文件： %s 完成%n", inputPath);
            } catch (IOException e) {
                e.printStackTrace();
                c.printf("处理文件： %s 失败。%n", inputPath);
                c.flush();
            }
        });
        return 0;
    }

    private void process(IoConsole c, Path inputPath, Path outputPath) throws IOException{
        c.printf("处理文件： %s%n", inputPath);
        c.flush();
        Set<String> set = new HashSet<>();
        try (Stream<String> lines = Files.lines(inputPath, StandardCharsets.ISO_8859_1);
             BufferedWriter writer = Files.newBufferedWriter(outputPath,  StandardCharsets.ISO_8859_1)) {
            set.clear();
            lines.forEach(line -> {
                if (!set.contains(line)) {
                    set.add(line);
                    write(writer, line);
                }
            });
            writer.flush();
        }
    }

    private void write(BufferedWriter bufferedWriter, String w) {
        try {
            bufferedWriter.write(w);
            bufferedWriter.write(System.getProperty("line.separator"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Arguments parseArguments(String[] args) {
        Arguments arguments = new Arguments();
        if (args == null || args.length == 0) {
            return arguments;
        }

        for (int i = 0; i < args.length; i++) {
            switch(args[i]) {
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
            return "按行删除重复文本。";
        }

        @Override
        public String getUsage() {
            StringBuilder result = new StringBuilder();
            result.append(Configurator.VALUE_LINE_SEP).append(getName()).append(" <inputFile>")
                  .append(Configurator.VALUE_LINE_SEP);

            return result.toString();
        }

        @Override
        public Map<String, String> getArguments() {
            Map<String, String> result = new TreeMap<>();
            result.put("-h", "打印帮助信息。");
            return result;
        }

    }

}
