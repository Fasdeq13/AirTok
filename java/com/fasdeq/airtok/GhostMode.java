package com.fasdeq.airtok;


final class GhostMode {
    interface Flags { boolean on(String key); }
    interface Notice { void log(String message); }
    GhostMode(Flags f, Notice n) {}
    void bootstrap() {}
    void apply() {}
    void stop() {}
    String report() { return "отключен"; }
}
