package christian.eilers.flibber;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.ProfilAndWgs.ProfilActivity;
import christian.eilers.flibber.Utils.LocalStorage;

public class MainActivity extends AppCompatActivity {

    // TODO: verschiedenen Auth Listener in jeder Activity?
    // --> Intent sollte abhängig sein von der Activity, in welcher der User sich ausloggt
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        auth_listener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                // Aktuellen User ausloggen
                if(firebaseAuth.getCurrentUser() == null) {
                    /*setLocalData(MainActivity.this, null, null, null, null);

                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(loginIntent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();*/
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(auth_listener);

        groupID = LocalStorage.getGroupID(this);
        userID = LocalStorage.getUserID(this);

        if(userID != null) {
            if(groupID != null) {
                Intent homeIntent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(homeIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
            else {
                Intent profilIntent = new Intent(MainActivity.this, ProfilActivity.class);
                startActivity(profilIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        }
        else {
            if(auth.getCurrentUser() != null) auth.signOut();

            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
    }

    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener auth_listener;
    private String groupID, userID;
}
