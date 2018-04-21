package christian.eilers.flibber;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.iid.FirebaseInstanceId;

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.Profil.ProfilActivity;
import christian.eilers.flibber.Utils.LocalStorage;
import static christian.eilers.flibber.Utils.Strings.*;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener, View.OnFocusChangeListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initializeViews();
        auth = FirebaseAuth.getInstance();
    }

    // Send Email to Reset Password
    private void resetPasswordEmail() {
        final String email = eT_email.getText().toString().trim();
        if(TextUtils.isEmpty(email)){
            Toast.makeText(LoginActivity.this, "Type in your E-Mail Address", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialDialog.Builder(LoginActivity.this)
                .title("Passwort wirklich zurücksetzen?")
                .content("Nach dem Bestätigen erhälst du eine E-Mail, um dein Passwort zu zurückzusetzen.")
                .positiveText("Bestätigen")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        progressBar.setVisibility(View.VISIBLE);
                        auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful())
                                            Toast.makeText(LoginActivity.this, "E-Mail sent.", Toast.LENGTH_SHORT).show();
                                        else
                                            Toast.makeText(LoginActivity.this, "Failed to send E-Mail.", Toast.LENGTH_SHORT).show();
                                        progressBar.setVisibility(View.GONE);
                                    }
                                });
                    }
                })
                .negativeText("Abbrechen")
                .show();
    }

    private void verificationDialog() {
        new MaterialDialog.Builder(LoginActivity.this)
                .title("Dein Account ist noch nicht verifiziert")
                .content("Bestätige deinen Account mithilfe der E-Mail die du erhalten hast.")
                .positiveText("Okay")
                .neutralText("Erneut senden")
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        progressBar.setVisibility(View.VISIBLE);
                        auth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                auth.signOut();
                                if (task.isSuccessful())
                                    Toast.makeText(LoginActivity.this, "E-Mail sent.", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(LoginActivity.this, "Failed to send E-Mail.", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }
                })
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        auth.signOut();
                    }
                })
                .show();
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
                if(task.isSuccessful()) {
                    // TODO: EMail verifizieren wieder einbauen
                    /*if (!auth.getCurrentUser().isEmailVerified()) {
                        progressBar.setVisibility(View.GONE);
                        verificationDialog();
                        return;
                    }*/
                    final String userID = auth.getCurrentUser().getUid();
                    final String deviceToken = FirebaseInstanceId.getInstance().getToken();
                    final FirebaseFirestore db = FirebaseFirestore.getInstance();
                    final DocumentReference ref_user = db.collection("users").document(userID);
                    db.runTransaction(new Transaction.Function<User>() {
                        @Nullable
                        @Override
                        public User apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                            // Read out user-information
                            DocumentSnapshot snapshot_user = transaction.get(ref_user);
                            User user = snapshot_user.toObject(User.class);

                            // Update Devicetoken
                            transaction.update(ref_user, DEVICETOKEN, deviceToken);
                            return user;
                        }
                    }).addOnSuccessListener(new OnSuccessListener<User>() {
                        @Override
                        public void onSuccess(User user) {
                            eT_password.setText("");
                            // Set Local Data
                            LocalStorage.setData(LoginActivity.this, null, userID, user.getName(), user.getPicPath());
                            // Start ProfilActivity
                            progressBar.setVisibility(View.GONE);
                            Intent i_login = new Intent(LoginActivity.this, ProfilActivity.class);
                            i_login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i_login);
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            eT_password.setText("");
                            auth.signOut();
                            Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    });

                } else {
                    eT_password.setText("");
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

        String email = getIntent().getStringExtra(EMAIL);
        String password = getIntent().getStringExtra(PASSWORD);
        if (email != null && password != null) {
            eT_email.setText(email);
            eT_password.setText(password);
        }

        btn_password.setOnClickListener(this);
        btn_login.setOnClickListener(this);
        btn_newAcc.setOnClickListener(this);

        eT_email.setOnEditorActionListener(this);
        eT_password.setOnEditorActionListener(this);

        eT_password.setOnFocusChangeListener(this);
        eT_email.setOnFocusChangeListener(this);
    }

    // Check which Button has been clicked
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_ok) login();
        else if (id == R.id.button_forget) resetPasswordEmail();
        else if (id == R.id.button_newAccount) {
            Intent i_registerActivity = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i_registerActivity);
        }
    }

    // Apply Action on Keyboard Action
    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == EditorInfo.IME_ACTION_GO) {
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

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (eT_email.hasFocus() || eT_password.hasFocus()) return;
        if (!hasFocus) {
            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    // Variablen
    private EditText eT_email, eT_password;
    private Button btn_login, btn_newAcc, btn_password;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
}
