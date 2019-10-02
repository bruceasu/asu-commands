package me.asu.cli.command.cnsort;

import java.util.Objects;

/**
 * Created by suk on 2019/6/4.
 */
public class Orders {

    /** 词组 */
    private static ChineseSearcher phrasesSearcher;
    private static ChineseSearcher simplifiedChineseSearcher;
    private static ChineseSearcher traditionalChineseSearcher;

    // -------------------------------------------------
    
    public static int searchSimplifiedOrder(String w) {
        Objects.requireNonNull(w);
        ChineseSearcher simplifiedChineseOrder = getSimplifiedChineseSearcher();
        return simplifiedChineseOrder.searchOrder(w);
    }

    public static ChineseSearcher getSimplifiedChineseSearcher() {
        if (simplifiedChineseSearcher == null) {
            simplifiedChineseSearcher = new SimplifiedChineseSearcher();
        }
        return simplifiedChineseSearcher;
    }

    // -------------------------------------------------

    public static int searchTraditionalOrder(String w) {
        Objects.requireNonNull(w);
        ChineseSearcher traditionalChineseOrder = getTraditionChineseSearcher();
        return traditionalChineseOrder.searchOrder(w);
    }

    public static ChineseSearcher getTraditionChineseSearcher() {
        if (traditionalChineseSearcher == null) {
            traditionalChineseSearcher = new TraditionalChineseSearcher();
        }
        return traditionalChineseSearcher;
    }

    // -------------------------------------------------
    public static int searchPhraseOrder(String w) {
        Objects.requireNonNull(w);
        ChineseSearcher traditionalChineseOrder = getTraditionChineseSearcher();
        return traditionalChineseOrder.searchOrder(w);
    }

    public static ChineseSearcher getPhrasesSearcher() {
        if (phrasesSearcher == null) {
            phrasesSearcher = new PhrasesSearcher();
        }
        return phrasesSearcher;
    }
}
