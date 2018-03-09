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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import christian.eilers.flibber.Profil.ProfilActivity;
import christian.eilers.flibber.Utils.LocalStorage;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initializeViews();
        hideKeyboard_onClickOutside();
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
        if (i == EditorInfo.IME_ACTION_NEXT) {
            if(eT_name.hasFocus()) showSoftKeyboard(eT_email);
            else if(eT_email.hasFocus()) showSoftKeyboard(eT_password);
            return true;
        } else if (i == EditorInfo.IME_ACTION_GO) {
            signUp();
            return true;
        }
        return false;
    }

    // Sign Up new user with e-amil and password
    private void signUp() {
        final String username = eT_name.getText().toString().trim();
        final String email = eT_email.getText().toString().trim();
        final String password = eT_password.getText().toString().trim();
        if (!isValidForm(email, password, username)) return;
        progressBar.setVisibility(View.VISIBLE);
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    final String userID = auth.getCurrentUser().getUid();
                    // TODO: E-Mail verification wieder einbauen
                    // sendEmailVerification();

                    LocalStorage.setData(RegisterActivity.this, null, userID, username, null);
                    // Speichere Username in DB
                    Map<String, Object> userData = new HashMap<>();
                    userData.put(NAME, username);
                    userData.put(EMAIL, email);
                    FirebaseFirestore.getInstance().collection(USERS).document(userID).set(userData);
                    // Wechsel zum WG-Selector
                    Intent i_wgSelector = new Intent(RegisterActivity.this, ProfilActivity.class);
                    startActivity(i_wgSelector);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                    eT_password.setText("");
                }
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // Switch to LoginActivity
    private void toLogin() {
        Intent i_loginActivity = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(i_loginActivity);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    // Send email verification
    private void sendEmailVerification() {
        final FirebaseUser user = auth.getCurrentUser();
        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this,
                                    "Verification email sent to " + user.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Failed to send verification email.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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

    // Show Keyboard an focus View v
    public void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // Attach Listener to EditTexts, on Click outside hide the Keyboard
    private void hideKeyboard_onClickOutside() {
        eT_name.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        eT_email.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        eT_password.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });
    }

    // Hide Keyboard
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // Variablen
    private EditText eT_name, eT_email, eT_password;
    private Button btn_signup, btn_toLogin;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    private final String USERS = "users";
    private final String NAME = "name";
    private final String EMAIL = "email";
}
