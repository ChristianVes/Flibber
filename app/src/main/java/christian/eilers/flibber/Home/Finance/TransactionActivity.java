package christian.eilers.flibber.Home.Finance;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blackcat.currencyedittext.CurrencyEditText;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.*;

public class TransactionActivity extends AppCompatActivity implements View.OnFocusChangeListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);
        initializeViews();
        initializeVariables();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        et_article = findViewById(R.id.input_article);
        et_description = findViewById(R.id.input_description);
        et_price = findViewById(R.id.input_price);
        progressBar = findViewById(R.id.progressBar);
        btn_description = findViewById(R.id.btn_description);
        tv_beteiligte = findViewById(R.id.tv_beteiligte);
        tv_bezahler = findViewById(R.id.tv_bezahler);
        layout_beteiligte = findViewById(R.id.layout_beteiligte);
        layout_bezahler = findViewById(R.id.layout_bezahler);

        et_price.requestFocus();
        et_price.setLocale(Locale.GERMANY);
        et_price.configureViewForLocale(Locale.GERMANY);

        et_description.setOnFocusChangeListener(this);
        et_article.setOnFocusChangeListener(this);
        et_price.setOnFocusChangeListener(this);

        btn_description.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_description.getVisibility() == View.GONE) {
                    et_description.setVisibility(View.VISIBLE);
                    btn_description.setImageResource(R.drawable.ic_keyboard_arrow_up);
                }
                else {
                    et_description.setVisibility(View.GONE);
                    btn_description.setImageResource(R.drawable.ic_keyboard_arrow_down);
                }
            }
        });

        setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Titel der Actionbar ausblenden
    }

    // Initialize variables
    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        users = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
        if(users == null || userID == null || groupID == null) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }

        setBezahler(userID);
        final ArrayList<User> userList = new ArrayList<>(users.values());
        selectedIDs = new ArrayList<>();
        layout_beteiligte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new BeteiligteSelectionDialog(TransactionActivity.this,
                        android.R.style.Theme_Translucent_NoTitleBar, userList);
                dialog.show();
            }
        });
        layout_bezahler.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new BezahlerSelectionDialog(TransactionActivity.this,
                        android.R.style.Theme_Translucent_NoTitleBar, userList);
                dialog.show();
            }
        });
    }

    public void setBezahler(String bezahlerID) {
        this.bezahlerID = bezahlerID;
        if (userID.equals(bezahlerID))
            tv_bezahler.setText("Mir");
        else {
            String[] names = users.get(bezahlerID).getName().split(" ", 2);
            tv_bezahler.setText(names[0]);
        }
    }

    public String getBezahler() { return bezahlerID; }

    public void setBeteiligte(ArrayList<String> selectedIDs) {
        this.selectedIDs = selectedIDs;
        if (selectedIDs.size() == 1) tv_beteiligte.setText("1 Person");
        else tv_beteiligte.setText(selectedIDs.size() + " Personen");
    }

    public ArrayList<String> getSelectedIDs() {
        return selectedIDs;
    }

    // Save the Payment in the database
    private void saveTransaction() {
        long price = et_price.getRawValue();
        String title = et_article.getText().toString().trim();
        String description = et_description.getText().toString().trim();
        if (price <= 0) {
            Toast.makeText(TransactionActivity.this, "Keinen Preis eingegeben...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(TransactionActivity.this, "Keinen Titel eingegeben...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedIDs.isEmpty()) {
            Toast.makeText(TransactionActivity.this, "Keinen Beteiligten angegeben...", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        DocumentReference doc = db.collection(GROUPS).document(groupID).collection(FINANCES).document();

        final Payment payment = new Payment(
                doc.getId(),
                title,
                description,
                bezahlerID,
                userID,
                selectedIDs,
                price
        );

        chargeCosts(payment);
    }

    // Compute costs for each involved user and write everything to the database as Transaction
    private void chargeCosts(final Payment payment) {
        final CollectionReference ref_users = db.collection(GROUPS).document(groupID).collection(USERS);
        final CollectionReference ref_finances = db.collection(GROUPS).document(groupID).collection(FINANCES);

        db.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                HashMap<String, Long> map = new HashMap<>();
                long partialPrice = Math.round((double) payment.getPrice() / payment.getInvolvedIDs().size());
                long totalPriceRounded = partialPrice * payment.getInvolvedIDs().size();

                // Read-Operations
                for (String involvedID : payment.getInvolvedIDs()) {
                    DocumentSnapshot snapshot = transaction.get(ref_users.document(involvedID));
                    // aktueller Beteiligter ist ebenfalls Bezahler
                    if (involvedID.equals(payment.getPayerID())) {
                        map.put(involvedID, snapshot.getLong(MONEY) + totalPriceRounded - partialPrice);
                    }
                    // aktueller Beteiligte ist nicht auch Bezahler
                    else map.put(involvedID, snapshot.getLong(MONEY) - partialPrice);
                }

                // Bezahler Geld verrechnen, falls er noch nicht in Involviert-Schleife gemacht
                if (!map.containsKey(payment.getPayerID())) {
                    DocumentSnapshot snapshot = transaction.get(ref_users.document(payment.getPayerID()));
                    map.put(payment.getPayerID(), snapshot.getLong(MONEY) + totalPriceRounded);
                }

                // Write-Operations
                for (String key : map.keySet()) {
                    transaction.update(ref_users.document(key), MONEY, map.get(key));
                }
                // Speichere Payment in der Finanzen-Collection
                transaction.set(ref_finances.document(payment.getKey()), payment);

                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TransactionActivity.this, "Erfolgreich hinzugefügt!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE);
                Crashlytics.logException(e);
                Toast.makeText(TransactionActivity.this, "Fehler!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Verberge Tastatur, wenn gegebene Views ihren Fokus verlieren
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (et_article.hasFocus() || et_price.hasFocus()) return;
        if (!hasFocus) {
            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_finanzen_eintrag, menu);
        MenuItem item_save = menu.findItem(R.id.action_save);
        item_save.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });
        return true;
    }

    private String userID, groupID, bezahlerID;
    private FirebaseFirestore db;
    private HashMap<String, User> users;
    private ArrayList<String> selectedIDs;

    private Toolbar toolbar;
    private EditText et_article, et_description;
    private CurrencyEditText et_price;
    private ProgressBar progressBar;
    private ImageButton btn_description;
    private TextView tv_bezahler, tv_beteiligte;
    private RelativeLayout layout_bezahler, layout_beteiligte;

}