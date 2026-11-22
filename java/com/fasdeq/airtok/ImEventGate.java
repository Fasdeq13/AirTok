package com.fasdeq.airtok;

final class ImEventGate {
    interface Mode { boolean on(String channel); }
    interface Hit { void accept(String channel); }
    String status = "не подключён";
    ImEventGate(Mode m, Hit h, GhostMode.Notice n) {}
    void install() {}
    String report() { return status; }
}
