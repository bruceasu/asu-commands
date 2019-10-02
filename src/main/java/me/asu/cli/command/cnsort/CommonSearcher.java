package me.asu.cli.command.cnsort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class CommonSearcher implements ChineseSearcher {

        public List<String> orders;
        public Map<String, Integer> orderMap;

        public CommonSearcher(List<String> list) {
            this.orders = list;
            if (list != null) {
                init();
            }
        }


        @Override
        public int searchOrder(String w) {
            Integer integer = orderMap.get(w);
            if (integer == null) {
                return Integer.MAX_VALUE;
            }
            return integer;
        }

        protected void init() {
            orderMap = new HashMap<>();
            for (int i = 0; i < orders.size(); i++) {
                orderMap.put(orders.get(i), i);
            }
        }
    }