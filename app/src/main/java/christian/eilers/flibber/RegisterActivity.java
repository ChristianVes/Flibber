package christian.eilers.flibber;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Map;

import christian.eilers.flibber.Profil.ProfilActivity;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.*;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener, View.OnFocusChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initializeViews();
        auth = FirebaseAuth.getInstance();
    }

    // Initialize views from layout file
    private void initializeViews() {
        eT_name = findViewById(R.id.editText_name);
        eT_email = findViewById(R.id.editText_email);
        eT_password = findViewById(R.id.editText_password);
        btn_signup = findViewById(R.id.button_signUp);
        btn_toLogin = findViewById(R.id.button_toLogin);
        progressBar = findViewById(R.id.progressBar);

        btn_signup.setOnClickListener(this);
        btn_toLogin.setOnClickListener(this);

        eT_name.setOnEditorActionListener(this);
        eT_email.setOnEditorActionListener(this);
        eT_password.setOnEditorActionListener(this);

        eT_name.setOnFocusChangeListener(this);
        eT_email.setOnFocusChangeListener(this);
        eT_password.setOnFocusChangeListener(this);
    }

    // Check which Button has been clicked
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_signUp) {
            signUp();
        } else if (id == R.id.button_toLogin) {
            toLogin();
        }
    }

    // Apply Actions for custom Keyboard Keys
    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == EditorInfo.IME_ACTION_GO) {
            signUp();
            return true;
        }
        return false;
    }

    /*
    - Create User Account with E-Mail & Password
    - Send E-Mail Verification
    - Save User-Data in the Database
    - Go back to Login Page
    */
    private void signUp() {
        final String username = eT_name.getText().toString().trim();
        final String email = eT_email.getText().toString().trim();
        final String password = eT_password.getText().toString().trim();
        if (!isValidForm(email, password, username)) return;
        progressBar.setVisibility(View.VISIBLE);

        // Create the user account
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                    eT_password.setText("");
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                // Send E-Mail Verification
                auth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Sending Verification E-Mail failed.",
                                    Toast.LENGTH_SHORT).show();
                            eT_password.setText("");
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        // Save Userdata in the database
                        Map<String, Object> userData = new HashMap<>();
                        userData.put(NAME, username);
                        userData.put(EMAIL, email);
                        FirebaseFirestore.getInstance().collection(USERS).document(auth.getCurrentUser().getUid()).set(userData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // Go back to login page
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(RegisterActivity.this, "Account created. \n " +
                                                "Please verify your E-Mail now to login.",
                                        Toast.LENGTH_SHORT).show();
                                toLogin();
                            }
                        });
                    }
                });
            }
        });
    }

    // Switch to LoginActivity
    private void toLogin() {
        Intent i_loginActivity = new Intent(RegisterActivity.this, LoginActivity.class);
        i_loginActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i_loginActivity);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    // Check if email, password or name is empty
    private boolean isValidForm(String email, String password, String name) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(RegisterActivity.this, "Name required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(RegisterActivity.this, "E-Mail required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(RegisterActivity.this, "Password required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (eT_name.hasFocus() || eT_email.hasFocus() || eT_password.hasFocus()) return;
        if (!hasFocus) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    // Variablen
    private EditText eT_name, eT_email, eT_password;
    private Button btn_signup, btn_toLogin;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
}
