package christian.eilers.flibber;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{

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
        eT_name = (EditText) findViewById(R.id.editText_name);
        eT_email = (EditText) findViewById(R.id.editText_email);
        eT_password = (EditText) findViewById(R.id.editText_password);
        btn_signup = (Button) findViewById(R.id.button_signUp);
        btn_toLogin = (Button) findViewById(R.id.button_toLogin);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        btn_signup.setOnClickListener(this);
        btn_toLogin.setOnClickListener(this);
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

    // Sign Up new user with e-amil and password
    private void signUp() {
        String name = eT_name.getText().toString();
        String email = eT_email.getText().toString();
        String password = eT_password.getText().toString();
        if (!validateForm(email, password, name)) return;
        progressBar.setVisibility(View.VISIBLE);
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    sendEmailVerification();
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
        //TODO: Fade animation
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
    private boolean validateForm(String email, String password, String name) {
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

    // Variablen
    private EditText eT_name, eT_email, eT_password;
    private Button btn_signup, btn_toLogin;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
}
