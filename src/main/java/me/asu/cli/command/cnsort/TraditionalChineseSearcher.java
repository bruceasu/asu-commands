package me.asu.cli.command.cnsort;

import me.asu.cli.command.util.ResourcesFiles;

/**
 * 繁体
 */
public class TraditionalChineseSearcher extends CommonSearcher {

    public TraditionalChineseSearcher() {
        super(ResourcesFiles.ordersT());
    }
}