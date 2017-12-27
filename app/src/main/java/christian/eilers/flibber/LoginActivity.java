package christian.eilers.flibber;

import android.app.Activity;
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

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        eT_email = (EditText) findViewById(R.id.editText_email);
        eT_password = (EditText) findViewById(R.id.editText_password);
        btn_login = (Button) findViewById(R.id.button_ok);
        btn_password = (Button) findViewById(R.id.button_forget);
        btn_newAcc = (Button) findViewById(R.id.button_newAccount);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        // Hide Keyboard on click outside
        hideKeyboard_onClickOutside();
    }

    // Check which Button has been clicked
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_ok) {
            login();
        } else if (id == R.id.button_forget) {
            forgetPassword();
        } else if (id == R.id.button_newAccount) {
            createAccount();
        }
    }

    // Switches to a XXX to create a new Account
    private void createAccount() {
        //TODO
    }

    // Switches to XXX to set a new password
    private void forgetPassword() {
        //TODO
    }

    // Login the user with e-mail and password
    private void login() {
        String email = eT_email.getText().toString();
        String password = eT_password.getText().toString();
        if (!validateForm(email, password)) return;
        progressBar.setVisibility(View.VISIBLE);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    //TODO
                } else {
                    Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    eT_password.setText("");
                }
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // Check if email or password are empty
    private boolean validateForm(String email, String password) {
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

    private EditText eT_email, eT_password;
    private Button btn_login, btn_newAcc, btn_password;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
}
