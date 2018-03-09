package christian.eilers.flibber.Utils;

import android.content.Context;
import android.content.SharedPreferences;

// Klasse zur lokalen Speicherung wichtiger Variablen in SharedPreferences (im lokalen Gerätespeicher)
public class LocalStorage {

    // USER ID
    public static String getUserID(Context c) {
        return c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE).getString("USERID", null);
    }

    // GROUP ID
    public static String getGroupID(Context c) {
        return c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE).getString("GROUPID", null);
    }

    // USER NAME
    public static String getUsername(Context c) {
        return c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE).getString("USERNAME", null);
    }

    // PROFILE PICTURE PATH
    public static String getPicPath(Context c) {
        return c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE).getString("PICPATH", null);
    }


    public static void setData(Context c, String groupID, String userID, String userName, String picPath) {
        SharedPreferences.Editor editor = c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE).edit();
        editor.putString("GROUPID", groupID);
        editor.putString("USERID", userID);
        editor.putString("USERNAME", userName);
        editor.putString("PICPATH", picPath);
        editor.apply();
    }

    public static void setUserID(Context c, String userID) {
        SharedPreferences.Editor editor = c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE).edit();
        editor.putString("USERID", userID);
        editor.apply();
    }

    public static void setGroupID(Context c, String groupID) {
        SharedPreferences.Editor editor = c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE).edit();
        editor.putString("GROUPID", groupID);
        editor.apply();
    }

    public static void setUserName(Context c, String userName) {
        SharedPreferences.Editor editor = c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE).edit();
        editor.putString("USERNAME", userName);
        editor.apply();
    }

    public static void setPicPath(Context c, String picPath) {
        SharedPreferences.Editor editor = c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE).edit();
        editor.putString("PICPATH", picPath);
        editor.apply();
    }
}