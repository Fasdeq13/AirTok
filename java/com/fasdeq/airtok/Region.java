package com.fasdeq.airtok;

import android.content.Context;
import android.telephony.TelephonyManager;



















public final class Region {

    private Region() {}

    public static String getSimCountryIso(TelephonyManager tm) {
        String answer = !AirTok.active()
                ? (tm == null ? "" : tm.getSimCountryIso())
                : AirTok.current()[AirTok.ISO];
        return Plugins.region("sim_country", answer);
    }

    public static String getNetworkCountryIso(TelephonyManager tm) {
        String answer = !AirTok.active()
                ? (tm == null ? "" : tm.getNetworkCountryIso())
                : AirTok.current()[AirTok.ISO];
        return Plugins.region("network_country", answer);
    }

    public static String getSimOperator(TelephonyManager tm) {
        String answer = !AirTok.active()
                ? (tm == null ? "" : tm.getSimOperator())
                : AirTok.current()[AirTok.MCCMNC];
        return Plugins.region("sim_operator", answer);
    }

    public static String getNetworkOperator(TelephonyManager tm) {
        String answer = !AirTok.active()
                ? (tm == null ? "" : tm.getNetworkOperator())
                : AirTok.current()[AirTok.MCCMNC];
        return Plugins.region("network_operator", answer);
    }

    public static String getSimOperatorName(TelephonyManager tm) {
        String answer = !AirTok.active()
                ? (tm == null ? "" : tm.getSimOperatorName())
                : AirTok.current()[AirTok.CARRIER];
        return Plugins.region("sim_operator_name", answer);
    }

    public static String getNetworkOperatorName(TelephonyManager tm) {
        String answer = !AirTok.active()
                ? (tm == null ? "" : tm.getNetworkOperatorName())
                : AirTok.current()[AirTok.CARRIER];
        return Plugins.region("network_operator_name", answer);
    }

    







    private static boolean realCard(TelephonyManager tm) {
        if (tm == null) return false;
        try {
            if (tm.hasIccCard()) return true;
            return tm.getSimState() == TelephonyManager.SIM_STATE_READY;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int getSimState(TelephonyManager tm) {
        if (!AirTok.active() || realCard(tm)) {
            return tm == null ? TelephonyManager.SIM_STATE_UNKNOWN : tm.getSimState();
        }
        return TelephonyManager.SIM_STATE_READY;
    }

    public static int getSimState(TelephonyManager tm, int slot) {
        if (!AirTok.active() || realCard(tm)) {
            return tm == null ? TelephonyManager.SIM_STATE_UNKNOWN : tm.getSimState(slot);
        }
        return slot == 0 ? TelephonyManager.SIM_STATE_READY : TelephonyManager.SIM_STATE_UNKNOWN;
    }

    public static boolean hasIccCard(TelephonyManager tm) {
        if (!AirTok.active() || realCard(tm)) return tm != null && tm.hasIccCard();
        return true;
    }

    
    public static boolean isNetworkRoaming(TelephonyManager tm) {
        if (!AirTok.active()) return tm != null && tm.isNetworkRoaming();
        return false;
    }

    

    







    public static int getPhoneCount(TelephonyManager tm) {
        if (!AirTok.active() || realCard(tm)) return tm == null ? 0 : tm.getPhoneCount();
        return 1;
    }

    public static int getActiveModemCount(TelephonyManager tm) {
        if (!AirTok.active() || realCard(tm)) {
            return tm == null ? 0 : tm.getActiveModemCount();
        }
        return 1;
    }

    
    public static int getPhoneType(TelephonyManager tm) {
        if (!AirTok.active() || realCard(tm)) {
            return tm == null ? TelephonyManager.PHONE_TYPE_NONE : tm.getPhoneType();
        }
        return TelephonyManager.PHONE_TYPE_GSM;
    }

    public static int getActiveSubscriptionInfoCount(
            android.telephony.SubscriptionManager subs) {
        if (!AirTok.active()) return subs == null ? 0 : subs.getActiveSubscriptionInfoCount();
        return 1;
    }

    






    public static int getDefaultDataSubscriptionId() {
        if (!AirTok.active()) {
            return android.telephony.SubscriptionManager.getDefaultDataSubscriptionId();
        }
        return 1;
    }

    public static int getDefaultVoiceSubscriptionId() {
        if (!AirTok.active()) {
            return android.telephony.SubscriptionManager.getDefaultVoiceSubscriptionId();
        }
        return 1;
    }

    public static int getDefaultSmsSubscriptionId() {
        if (!AirTok.active()) {
            return android.telephony.SubscriptionManager.getDefaultSmsSubscriptionId();
        }
        return 1;
    }

    public static int getActiveDataSubscriptionId() {
        if (!AirTok.active()) {
            return android.telephony.SubscriptionManager.getActiveDataSubscriptionId();
        }
        return 1;
    }

    




    public static int getSimCarrierId(TelephonyManager tm) {
        if (!AirTok.active()) return tm == null ? -1 : tm.getSimCarrierId();
        return TelephonyManager.UNKNOWN_CARRIER_ID;
    }
}
