package christian.eilers.flibber;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.auth.FirebaseAuth;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.ProfilAndWgs.WgsAndProfilActivity;
import christian.eilers.flibber.Utils.Utils;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        auth_listener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser() == null) {
                    Utils.setLocalData(MainActivity.this, null, null, null);

                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(loginIntent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(auth_listener);

        Utils.getLocalData(this);

        if(auth.getCurrentUser() != null && Utils.getUSERID() != null && Utils.getUSERNAME() != null) {
            // Aktueller User besitzt UserID und Usernamen
            if(Utils.getWGKEY() == null) {
                // Wechsel zur Profil&WG Activity
                Intent profilIntent = new Intent(MainActivity.this, WgsAndProfilActivity.class);
                startActivity(profilIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            } else {
                // Wechsel zum Home der aktuellen WG
                Intent homeIntent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(homeIntent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }

        } else {
            // Logge aktuellen User aus
            auth.signOut();
        }
    }

    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener auth_listener;
}
