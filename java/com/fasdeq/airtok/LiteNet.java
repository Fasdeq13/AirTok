package com.fasdeq.airtok;

public final class LiteNet {

    private static boolean told;

    private LiteNet() {}

    public static boolean drops() {
        if (!Version.LITE) return true;
        if (!told) {
            told = true;
            Diary.note("network: Lite was told to drop requests, and they are sent all the same");
        }
        return false;
    }
}
