package christian.eilers.flibber.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

import christian.eilers.flibber.Models.User;

public class Utils {
    private static String WGKEY, USERID, USERNAME, PICPATH;
    private static HashMap<String, User> USERS;
    private static ListenerRegistration LISTENER;

    public static String getWGKEY() {
        return WGKEY;
    }
    public static String getUSERID() {
        return USERID;
    }
    public static String getUSERNAME() {
        return USERNAME;
    }
    public static String getPICPATH() {
        return PICPATH;
    }
    public static HashMap<String, User> getUSERS() {
        return USERS;
    }

    // Übernehme lokale offline Daten vom Handy für die static Variablen
    public static void getLocalData(Context c) {
        SharedPreferences pref_data = c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE);
        WGKEY = pref_data.getString("WGKEY", null);
        USERID = pref_data.getString("USERID", null);
        USERNAME = pref_data.getString("USERNAME", null);
        PICPATH = pref_data.getString("PICPATH", null);
    }

    // Speichere Userdaten lokal auf dem Handy ab
    public static void setLocalData(Context c, String wgKey, String userID, String userName, String picPath) {
        WGKEY = wgKey;
        USERID = userID;
        USERNAME = userName;
        PICPATH = picPath;
        SharedPreferences pref_data = c.getSharedPreferences("USERDATA", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref_data.edit();
        editor.putString("WGKEY", wgKey);
        editor.putString("USERID", userID);
        editor.putString("USERNAME", userName);
        editor.putString("PICPATH", picPath);
        editor.apply();
    }

    public static void addUserListener() {
        if(WGKEY == null) {
            if(USERS != null) USERS.clear();
            return;
        }
        Query query = FirebaseFirestore.getInstance().collection("wgs").document(WGKEY).collection("users");
        LISTENER = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                USERS = new HashMap<>();
                for(DocumentSnapshot doc : documentSnapshots) {
                    User user = doc.toObject(User.class);
                    USERS.put(user.getUserID(), user);
                }
            }
        });
    }

    public static void removeUserListener() {
        if(LISTENER != null) LISTENER.remove();
    }
}
