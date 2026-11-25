package com.fasdeq.airtok;

public final class GhostMode {

    public interface Notice {
        void log(String message);
    }

    public interface Flags {
        boolean on(String key);
    }

    public GhostMode() {}

    public GhostMode(Flags flags, Notice notice) {}

    public void bootstrap() {}

    public void apply() {}

    public void close() {}

    public String report() {
        return Ghost.report();
    }
}
