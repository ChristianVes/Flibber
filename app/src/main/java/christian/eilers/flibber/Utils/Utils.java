package christian.eilers.flibber.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Utils {
    private static String WGKEY, USERID, USERNAME;

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
        SharedPreferences pref_data = c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE);
        WGKEY = pref_data.getString("WGKEY", null);
        USERID = pref_data.getString("USERID", null);
        USERNAME = pref_data.getString("USERNAME", null);
    }

    // Speichere Userdaten lokal auf dem Handy ab
    public static void setLocalData(Context c, String wgKey, String userID, String userName) {
        WGKEY = wgKey;
        USERID = userID;
        USERNAME = userName;
        SharedPreferences pref_data = c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref_data.edit();
        editor.putString("WGKEY", wgKey);
        editor.putString("USERID", userID);
        editor.putString("USERNAME", userName);
        editor.apply();
    }
}
