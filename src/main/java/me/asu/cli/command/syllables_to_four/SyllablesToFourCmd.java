package me.asu.cli.command.syllables_to_four;

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
import me.asu.tui.framework.api.*;
import me.asu.tui.framework.util.CliArguments;
import me.asu.tui.framework.util.CliCmdLineOption;
import me.asu.tui.framework.util.CliCmdLineParser;
import me.asu.util.Strings;

/**
 * 对词汇进行编码
 */
public class SyllablesToFourCmd implements CliCommand {

    private static final String NAMESPACE = "asu";
    private static final String CMD_NAME = "syllables-to-four";
    private Descriptor descriptor;

    @Override
    public Descriptor getDescriptor() {
        return (descriptor != null) ? descriptor : (descriptor = new InnerDescriptor());
    }

    @Override
    public Object execute(CliContext ctx, String[] args) {
        CliConsole console = ctx.getCliConsole();

        ArgumentsParser argumentsParser = new ArgumentsParser(args, console).invoke();
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
            final AtomicInteger count = new AtomicInteger();
            lines.forEach(line -> {
                count.incrementAndGet();
                if(Strings.isBlank(line) || line.charAt(0) == '#') {
                    return;
                }
                String[] split = line.split("\\s+");
                String word = split[0];
                if (word.length() != split.length - 1) {
                    argumentsParser.c.printf("Error syables: %s%n", line);
                    return;
                }
                List<String> keys = new ArrayList<>();
                switch(word.length()){
                    case 1:
                        argumentsParser.c.printf("Error single word phrase: %s%n", line);
                        return;
                    case 2:
                        keys.add(split[1]);
                        keys.add(split[2]);
                        break;
                    case 3:
                        keys.add(split[1].substring(0,1));
                        keys.add(split[2].substring(0,1));
                        keys.add(split[3]);
                        break;
                    default:
                        keys.add(split[1].substring(0,1));
                        keys.add(split[2].substring(0,1));
                        keys.add(split[3].substring(0,1));
                        keys.add(split[split.length-1].substring(0,1));
                        break;
                }
                write(bufferedWriter, keys, split[0]);
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
            String text = String.format("%s\t%s%n", words, join(keys, ""));
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

    private CliArguments parseArguments(String[] args) {
        CliArguments arguments = new CliArguments();
        if (args == null || args.length == 0) {
            return arguments;
        }

        return descriptor.parse(args);
    }

    @Override
    public void plug(CliContext plug) {
        //descriptor = new DescriptorImpl();
     }

    @Override
    public void unplug(CliContext plug) {
        // nothing to do
    }


    private class InnerDescriptor implements Descriptor {

        private CliCmdLineParser parser = new CliCmdLineParser();

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
            return "音节序列转4码字词编码";
        }

        InnerDescriptor()
        {
            CliCmdLineOption opt1 = CliCmdLineOption.builder().build();
            opt1.setShortName("e");
            opt1.setLongName("encoding");
            opt1.setHasArg(true);
            opt1.setDescription("文件字符编码，默认为 UTF-8。");
            CliCmdLineOption opt2 = CliCmdLineOption.builder().build();
            opt2.setShortName("h");
            opt2.setLongName("help");
            opt2.setHasArg(false);
            opt1.setDescription("打印帮助信息。");
            parser.addOption(opt1, opt2);
        }

        @Override
        public CliCmdLineParser getCliCmdLineParser()
        {
            return parser;
        }
    }

    private class ArgumentsParser {

        private boolean error;
        private final String[] args;
        private final CliConsole c;
        CliArguments arguments = null;
        public ArgumentsParser(String[] args, CliConsole c) {
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
            if (arguments.hasParam("h")
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
            return Paths.get(getInputPath().toAbsolutePath().toString() + ".out");
        }


        public boolean hasInputFile() {
            return arguments.getRemain().size() > 0;
        }

        public Charset getEncoding() {
            Charset encoding = StandardCharsets.UTF_8;
            if (arguments.hasParam("e")) {
                encoding = Charset.forName(arguments.getParam("e"));
            }
            return encoding;
        }

        public Path getInputPath() {
            return Paths.get(arguments.getRemain().get(0)).toAbsolutePath();
        }
    }
}
