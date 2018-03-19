package christian.eilers.flibber.Home;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.blackcat.currencyedittext.CurrencyEditText;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import christian.eilers.flibber.Adapter.BeteiligteAdapter;
import christian.eilers.flibber.Adapter.BezahlerAdapter;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

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
        rec_beteiligte = findViewById(R.id.listBeteiligte);
        rec_bezahler = findViewById(R.id.listBezahler);
        rec_beteiligte = findViewById(R.id.listBeteiligte);

        et_price.requestFocus();
        et_price.setLocale(Locale.GERMANY);
        et_price.configureViewForLocale(Locale.GERMANY);

        et_description.setOnFocusChangeListener(this);
        et_article.setOnFocusChangeListener(this);
        et_price.setOnFocusChangeListener(this);

        rec_bezahler.setHasFixedSize(true);
        rec_bezahler.setLayoutManager(new LinearLayoutManager(this));
        rec_beteiligte.setHasFixedSize(true);
        rec_beteiligte.setLayoutManager(new LinearLayoutManager(this));

        setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Titel der Actionbar ausblenden
    }

    // Initialize variables
    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();

        db.collection(GROUPS).document(groupID).collection(USERS).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot documentSnapshots) {
                retrieveUsers(documentSnapshots);
                ArrayList<User> userList = new ArrayList<>(users.values());
                adapter_bezahler = new BezahlerAdapter(userList, userID);
                adapter_beteiligte = new BeteiligteAdapter(userList);
                rec_bezahler.setAdapter(adapter_bezahler);
                rec_beteiligte.setAdapter(adapter_beteiligte);
            }
        });
    }

    // Erzeugt eine Userliste mithilfe eines Snapshots aus der Datenbank
    private void retrieveUsers(QuerySnapshot documentSnapshots) {
        HashMap<String, User> userHashMap = new HashMap<>();
        for(DocumentSnapshot doc : documentSnapshots) {
            User user = doc.toObject(User.class);
            userHashMap.put(user.getUserID(), user);
        }
        users = (HashMap<String, User>) userHashMap.clone();
    }

    // Save the Payment in the database
    private void saveTransaction() {
        int price = ((int) et_price.getRawValue());
        String title = et_article.getText().toString().trim();
        String description = et_description.getText().toString().trim();
        if (price == 0) return;
        if (TextUtils.isEmpty(title)) return;
        // Check if any involved User is selected
        Boolean hasInvolvedUser = false;
        for (Boolean isInvolved : adapter_beteiligte.getInvolvedIDs().values()) {
            if(isInvolved) {
                hasInvolvedUser = true;
                break;
            }
        }
        if(!hasInvolvedUser) return;

        DocumentReference doc = db.collection(GROUPS).document(groupID).collection(FINANCES).document();

        Payment payment = new Payment(
                doc.getId(),
                title,
                description,
                adapter_bezahler.getBezahlerID(),
                userID,
                adapter_beteiligte.getInvolvedIDs(),
                price
        );

        doc.set(payment);
        chargeCosts(payment);

        finish();
    }

    private void chargeCosts(Payment payment) {
        final CollectionReference ref = db.collection(GROUPS).document(groupID).collection(USERS);

        db.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                // DocumentSnapshot doc = transaction.get(ref.document(...));


                return null;
            }
        });
    }

    // Verberge Tastatur, wenn gegebene Views ihren Fokus verlieren
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
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

    private Toolbar toolbar;
    private EditText et_article, et_description;
    private CurrencyEditText et_price;
    private RecyclerView rec_bezahler, rec_beteiligte;

    private final String GROUPS = "groups";
    private final String USERS = "users";
    private final String FINANCES = "finances";
}
