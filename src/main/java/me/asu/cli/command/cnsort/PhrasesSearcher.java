package me.asu.cli.command.cnsort;

import java.util.HashMap;
import lombok.Data;
import me.asu.cli.command.util.ResourcesFiles;

@Data
public class PhrasesSearcher extends CommonSearcher {

        public PhrasesSearcher() {
            super(ResourcesFiles.ordersPhrases());
        }

        protected void init() {
            orderMap = new HashMap<>();
            for (int i = 0; i < orders.size(); i++) {
                String s = orders.get(i);
                String[] split = s.split("\t");
                if (split.length > 1) {
                    orderMap.put(split[0], Integer.valueOf(split[1]));
                } else {
                    orderMap.put(split[0], i * 10);
                }
            }
        }
    }