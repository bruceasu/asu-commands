package me.asu.cli.command.cnsort;

import me.asu.cli.command.util.ResourcesFiles;

/** 简体 */
public class SimplifiedChineseSearcher extends CommonSearcher {
        public SimplifiedChineseSearcher() {
           super(ResourcesFiles.orders());
        }
        @Override
        public int searchOrder(String w) {
            Integer integer = orderMap.get(w);
            if (integer == null) {
                return Integer.MAX_VALUE;
            }
            return integer;
        }
    }