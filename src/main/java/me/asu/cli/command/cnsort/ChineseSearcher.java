package me.asu.cli.command.cnsort;

import me.asu.util.Strings;

public interface ChineseSearcher {
        int searchOrder(String w);
        default int searchOrderByColumn(String w, int columnIndx) {
            return searchOrderByColumn(w, columnIndx, "\\s+");
        }

        default int searchOrderByColumn(String w, int columnIndex, String splitter) {
            if (Strings.isBlank(w)) {
                return Integer.MAX_VALUE;
            }
            if (Strings.isBlank(splitter)) {
                return searchOrder(w);
            }
            splitter = splitter.trim();
            w = w.trim();
            String[] split = w.split(splitter);
            if (columnIndex >= split.length) {
                columnIndex = split.length - 1;
            }
            return searchOrder(split[columnIndex]);
        }
    }