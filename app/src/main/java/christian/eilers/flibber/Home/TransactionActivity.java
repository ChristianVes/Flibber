package christian.eilers.flibber.Home;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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

import christian.eilers.flibber.Adapter.BeteiligteAdapter;
import christian.eilers.flibber.Adapter.BezahlerAdapter;
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
        rec_beteiligte = findViewById(R.id.recView_beteiligte);
        rec_bezahler = findViewById(R.id.recView_bezahler);
        progressBar = findViewById(R.id.progressBar);
        scrollView = findViewById(R.id.scrollView);
        layout_expand = findViewById(R.id.layout_expand);
        layout_detailed = findViewById(R.id.layout_detailed);
        btn_expand = findViewById(R.id.btn_expand);

        et_price.requestFocus();
        et_price.setLocale(Locale.GERMANY);
        et_price.configureViewForLocale(Locale.GERMANY);

        et_description.setOnFocusChangeListener(this);
        et_article.setOnFocusChangeListener(this);
        et_price.setOnFocusChangeListener(this);

        layout_expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layout_detailed.getVisibility() == View.GONE) {
                    layout_detailed.setVisibility(View.VISIBLE);
                    btn_expand.setImageResource(R.drawable.ic_keyboard_arrow_down);
                }
                else {
                    layout_detailed.setVisibility(View.GONE);
                    btn_expand.setImageResource(R.drawable.ic_keyboard_arrow_up);
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
        if(users == null) {
            Intent main = new Intent(this, MainActivity.class);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }

        int spanCount = 4;
        ArrayList<User> userList = new ArrayList<>(users.values());
        if (userList.size() <= 3) {
            spanCount = userList.size();
        }
        if (userList.size() == 5 || userList.size() == 6) spanCount = 3;

        layoutManagerBeteiligte = new GridLayoutManager(this, spanCount);
        layoutManagerBezahler = new GridLayoutManager(this, spanCount);

        rec_bezahler.setHasFixedSize(true);
        rec_bezahler.setLayoutManager(layoutManagerBezahler);

        rec_beteiligte.setHasFixedSize(true);
        rec_beteiligte.setLayoutManager(layoutManagerBeteiligte);

        adapter_bezahler = new BezahlerAdapter(userList, userID);
        adapter_beteiligte = new BeteiligteAdapter(userList);
        rec_bezahler.setAdapter(adapter_bezahler);
        rec_beteiligte.setAdapter(adapter_beteiligte);
    }

    // Save the Payment in the database
    private void saveTransaction() {
        long price = et_price.getRawValue();
        String title = et_article.getText().toString().trim();
        String description = et_description.getText().toString().trim();
        if (price <= 0) return;
        if (TextUtils.isEmpty(title)) return;
        if (adapter_beteiligte.getInvolvedIDs().isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);

        DocumentReference doc = db.collection(GROUPS).document(groupID).collection(FINANCES).document();

        final Payment payment = new Payment(
                doc.getId(),
                title,
                description,
                adapter_bezahler.getBezahlerID(),
                userID,
                adapter_beteiligte.getInvolvedIDs(),
                price
        );

        // doc.set(payment); auf Transaction ausgelagert
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveTransaction();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String userID, groupID;
    private FirebaseFirestore db;
    private HashMap<String, User> users;
    private BezahlerAdapter adapter_bezahler;
    private BeteiligteAdapter adapter_beteiligte;
    private GridLayoutManager layoutManagerBeteiligte, layoutManagerBezahler;

    private Toolbar toolbar;
    private EditText et_article, et_description;
    private CurrencyEditText et_price;
    private RecyclerView rec_bezahler, rec_beteiligte;
    private NestedScrollView scrollView;
    private ProgressBar progressBar;
    private RelativeLayout layout_expand;
    private LinearLayout layout_detailed;
    private ImageButton btn_expand;

}
