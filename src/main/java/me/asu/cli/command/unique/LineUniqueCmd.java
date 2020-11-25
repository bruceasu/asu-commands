package me.asu.cli.command.unique;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import me.asu.tui.framework.api.CliCommand;
import me.asu.tui.framework.api.CliConsole;
import me.asu.tui.framework.api.CliContext;
import me.asu.tui.framework.util.CliArguments;
import me.asu.tui.framework.util.CliCmdLineOption;
import me.asu.tui.framework.util.CliCmdLineParser;

/**
 * 按行删除重复文本
 */
public class LineUniqueCmd implements CliCommand
{

    private static final String     NAMESPACE = "asu";
    private static final String     CMD_NAME  = "line-unique";
    private              Descriptor descriptor;

    @Override
    public Descriptor getDescriptor()
    {
        return (descriptor != null) ? descriptor : (descriptor = new InnerDescriptor());
    }

    @Override
    public Object execute(CliContext ctx, String[] args)
    {
        CliConsole console = ctx.getCliConsole();

        CliArguments arguments = null;
        try {
            arguments = parseArguments(args);
        } catch (Exception e) {
            descriptor.printUsage(console);
            return null;
        }
        if (arguments.hasParam("h") || !arguments.hasRemain()) {
            descriptor.printUsage(console);
            return null;
        }
        List<String> remain = arguments.getRemain();
        remain.forEach(inputFile -> {
            Path inputPath = Paths.get(inputFile);
            Path outputPath = Paths.get(inputFile + ".tmp");
            try {
                process(console, inputPath, outputPath);
                Files.move(outputPath, inputPath, StandardCopyOption.REPLACE_EXISTING);
                console.printf("处理文件： %s 完成%n", inputPath);
            } catch (IOException e) {
                e.printStackTrace();
                console.printf("处理文件： %s 失败。%n", inputPath);
                console.flush();
            }
        });
        return 0;
    }

    private void process(CliConsole c, Path inputPath, Path outputPath) throws IOException
    {
        c.printf("处理文件： %s%n", inputPath);
        c.flush();
        Set<String> set = new HashSet<>();
        try (Stream<String> lines = Files.lines(inputPath, StandardCharsets.ISO_8859_1);
             BufferedWriter writer = Files.newBufferedWriter(outputPath,
                     StandardCharsets.ISO_8859_1)) {
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

    private void write(BufferedWriter bufferedWriter, String w)
    {
        try {
            bufferedWriter.write(w);
            bufferedWriter.write(System.getProperty("line.separator"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CliArguments parseArguments(String[] args)
    {
        CliArguments arguments = new CliArguments();
        if (args == null || args.length == 0) {
            return arguments;
        }
        arguments = descriptor.getCliCmdLineParser().parse(args);
        return arguments;
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

    private class InnerDescriptor implements Descriptor
    {

        CliCmdLineParser parser = new CliCmdLineParser();

        InnerDescriptor()
        {
            CliCmdLineOption opt = CliCmdLineOption.builder()
                                                   .shortName("h")
                                                   .longName("help")
                                                   .description("Print help message")
                                                   .build();

            parser.addOption(opt);
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
            return "按行删除重复文本。";
        }

    }

}
