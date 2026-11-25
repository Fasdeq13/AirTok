package com.fasdeq.airtok;




final class TabCount {

    private TabCount() {}

    
    static final int PLENTY = 100;

    
    static int of(CharSequence text) {
        if (text == null) return 0;
        String said = text.toString().trim();
        if (said.endsWith("+")) {
            return said.length() > 1 && digits(said.substring(0, said.length() - 1)) ? PLENTY : 0;
        }
        if (said.length() > 3 || !digits(said)) return 0;
        return Integer.parseInt(said);
    }

    private static boolean digits(String text) {
        if (text.length() == 0) return false;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < '0' || text.charAt(i) > '9') return false;
        }
        return true;
    }

    






    static int remembered(int dismissed, int now) {
        if (dismissed <= 0 || now <= 0 || now > dismissed) return 0;
        return now;
    }
}
