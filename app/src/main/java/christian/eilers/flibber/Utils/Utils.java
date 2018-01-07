package christian.eilers.flibber.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Utils {
    private static String WGKEY, USERID, USERNAME;
    public static String s_WGKEY = "WGKEY";
    public static String s_USERID = "USERID";
    public static String s_USERNAME = "USERNAME";
    public static String s_USERDATA = "USERDATA";

    public static String getWGKEY() {
        return WGKEY;
    }
    public static String getUSERID() {
        return USERID;
    }
    public static String getUSERNAME() {
        return USERNAME;
    }

    // Übernehme lokale offline Daten vom Handy für die static Variablen
    public static void getLocalData(Context c) {
        SharedPreferences pref_data = c.getSharedPreferences(s_USERDATA, Context.MODE_PRIVATE);
        WGKEY = pref_data.getString(s_WGKEY, null);
        USERID = pref_data.getString(s_USERID, null);
        USERNAME = pref_data.getString(s_USERNAME, null);
    }

    // Speichere Userdaten lokal auf dem Handy ab
    public static void setLocalData(Context c, String wgKey, String userID, String userName, String deviceToken) {
        SharedPreferences pref_data = c.getSharedPreferences(s_USERDATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref_data.edit();
        editor.putString(s_WGKEY, wgKey);
        editor.putString(s_USERID, userID);
        editor.putString(s_USERNAME, userName);
        editor.apply();
    }

    // Überprüfe auf null-Elemente
    public static boolean checkForNulls() {
        if(WGKEY == null)    return true;
        if(USERID == null)   return true;
        if(USERNAME == null) return true;

        return false;
    }
}
