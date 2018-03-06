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

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.ProfilAndWgs.ProfilActivity;
import christian.eilers.flibber.Utils.LocalStorage;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initializeViews();
        hideKeyboard_onClickOutside();
        auth = FirebaseAuth.getInstance();
    }

    // Switches to RegisterActivity to create a new Account
    private void toRegisterActivity() {
        Intent i_registerActivity = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(i_registerActivity);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    // Send Email to Reset Password
    private void resetPasswordEmail() {
        String email = eT_email.getText().toString();
        if(TextUtils.isEmpty(email)){
            Toast.makeText(LoginActivity.this, "E-Mail required.", Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO: Dialog zum Bestätigen anzeigen
        progressBar.setVisibility(View.VISIBLE);
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "E-Mail to reset password sent.", Toast.LENGTH_SHORT).show();
                        } else
                            Toast.makeText(LoginActivity.this, "Failed to send E-Mail.", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    // Login the user with e-mail and password
    private void login() {
        final String email = eT_email.getText().toString().trim();
        final String password = eT_password.getText().toString().trim();
        if (!isValidForm(email, password)) return;
        progressBar.setVisibility(View.VISIBLE);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                eT_password.setText("");
                if(task.isSuccessful()) {
                    // TODO: check if user is EmailVerified durch -> auth.getCurrentUser().isEmailVerified();
                    final String userID = auth.getCurrentUser().getUid();
                    FirebaseFirestore.getInstance().collection("users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            progressBar.setVisibility(View.GONE);
                            if(!task.isSuccessful()) {
                                Crashlytics.logException(task.getException());
                                return;
                            }
                            final User mUser = task.getResult().toObject(User.class);
                            LocalStorage.setData(LoginActivity.this, null, userID, mUser.getName(), mUser.getPicPath());

                            Intent i_wgSelector = new Intent(LoginActivity.this, ProfilActivity.class);
                            startActivity(i_wgSelector);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            // TODO: Alle Activies im Backstack löschen
                            finish();
                        }
                    });

                } else {
                    Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    // Initialize views from layout file
    private void initializeViews() {
        eT_email = findViewById(R.id.editText_email);
        eT_password = findViewById(R.id.editText_password);
        btn_login = findViewById(R.id.button_ok);
        btn_password = findViewById(R.id.button_forget);
        btn_newAcc = findViewById(R.id.button_newAccount);
        progressBar = findViewById(R.id.progressBar);

        btn_password.setOnClickListener(this);
        btn_login.setOnClickListener(this);
        btn_newAcc.setOnClickListener(this);

        eT_email.setOnEditorActionListener(this);
        eT_password.setOnEditorActionListener(this);
    }

    // Check which Button has been clicked
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_ok) {
            login();
        } else if (id == R.id.button_forget) {
            resetPasswordEmail();
        } else if (id == R.id.button_newAccount) {
            toRegisterActivity();
        }
    }

    // Apply Action on Keyboard Action
    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == EditorInfo.IME_ACTION_NEXT) {
            showSoftKeyboard(eT_password);
            return true;
        } else if (i == EditorInfo.IME_ACTION_GO) {
            login();
            return true;
        }
        return false;
    }

    // Check if email or password are empty
    private boolean isValidForm(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(LoginActivity.this, "E-Mail required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Password required", Toast.LENGTH_SHORT).show();
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
    private EditText eT_email, eT_password;
    private Button btn_login, btn_newAcc, btn_password;
    private ProgressBar progressBar;
    private FirebaseAuth auth;


}
