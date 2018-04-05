package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.fabiomsr.moneytextview.MoneyTextView;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import christian.eilers.flibber.Adapter.TaskBeteiligteAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;
import static christian.eilers.flibber.Utils.Strings.*;

public class TransactionDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);
        initializeVariables();
        initializeViews();
        loadTransaction();
    }

    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference().child(PROFILE);
        transactionID = getIntent().getExtras().getString(TRANSACTIONID);
        if(transactionID == null) {
            Intent main = new Intent(this, MainActivity.class);
            startActivity(main);
            finish();
        }

    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tv_description = findViewById(R.id.description);
        tv_payer = findViewById(R.id.payer_username);
        tv_price = findViewById(R.id.price);
        img_profile_payer = findViewById(R.id.payer_profile_image);
        rec_beteiligte = findViewById(R.id.recView_beteiligte);
        progressBar = findViewById(R.id.progressBar);
        label_price = findViewById(R.id.label_price);
        label_payer = findViewById(R.id.label_payer);
        label_involved = findViewById(R.id.label_beteiligte);

        final boolean isUeberweisung = getIntent().getExtras().getBoolean("isUeberweisung", false);
        if (isUeberweisung) {
            label_price.setText("Betrag:");
            label_payer.setText("An:");
            label_involved.setText("Von:");
        }

        rec_beteiligte.setHasFixedSize(true);
        rec_beteiligte.setLayoutManager(new LinearLayoutManager(this));
    }

    // Load the task information from the database
    private void loadTransaction() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection(GROUPS).document(groupID).collection(FINANCES).document(transactionID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // get the current Payment
                thisPayment = documentSnapshot.toObject(Payment.class);
                // DESCRIPTION
                if(thisPayment.getDescription().isEmpty()) tv_description.setVisibility(View.GONE);
                else tv_description.setText(thisPayment.getDescription());
                // PRICE
                tv_price.setAmount(thisPayment.getPrice());
                // TITLE
                setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
                getSupportActionBar().setTitle(thisPayment.getTitle()); // Titel des Tasks setzen

                // Lade Beteiligte User-Liste (in Reihenfolge)
                db.collection(GROUPS).document(groupID).collection(USERS).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        retrieveUsers(documentSnapshots);
                        ArrayList<User> userList = new ArrayList<>(users.values());
                        adapter_beteiligte = new TaskBeteiligteAdapter(userList);
                        rec_beteiligte.setAdapter(adapter_beteiligte);
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    // Erzeugt eine Userliste aller Beteiligter mithilfe eines Snapshots aus der Datenbank
    private void retrieveUsers(@NotNull QuerySnapshot documentSnapshots) {
        HashMap<String, User> userHashMap = new HashMap<>();
        for(DocumentSnapshot doc : documentSnapshots) {
            final User user = doc.toObject(User.class);
            if (thisPayment.getInvolvedIDs().contains(user.getUserID()))
                userHashMap.put(user.getUserID(), user);

            if (thisPayment.getPayerID().equals(user.getUserID())) {
                tv_payer.setText(user.getName());
                if (user.getPicPath() != null) {
                    try{
                        GlideApp.with(TransactionDetailActivity.this)
                                .load(storage.child(user.getPicPath()))
                                .placeholder(R.drawable.profile_placeholder)
                                .dontAnimate()
                                .into(img_profile_payer);
                    } catch (IllegalArgumentException e) {
                        Crashlytics.logException(e);
                    }
                }

            }
        }
        users = (HashMap<String, User>) userHashMap.clone();
    }

    // Delete the Payment (-> Recalculate Costs)
    // TODO: Nur löschen in den ersten 24h erlauben
    private void deletePayment() {
        // Nur dem Ersteller das Löschen erlauben
        if (thisPayment == null) return;
        if (!thisPayment.getCreatorID().equals(userID)) {
            Toast.makeText(TransactionDetailActivity.this, "Nicht berechtigt...", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        long timeDiff = System.currentTimeMillis() - thisPayment.getTimestamp().getTime();
        if (timeDiff > TimeUnit.DAYS.toMillis(1)) {
            Toast.makeText(TransactionDetailActivity.this, "Nur 24h möglich...", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        new MaterialDialog.Builder(TransactionDetailActivity.this)
                .title("Zahlung wirklich löschen?")
                .positiveText("Löschen")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        deleteTransaction();
                    }
                })
                .negativeText("Abbrechen")
                .show();
    }

    private void deleteTransaction() {
        final CollectionReference ref_users = db.collection(GROUPS).document(groupID).collection(USERS);
        final CollectionReference ref_finances = db.collection(GROUPS).document(groupID).collection(FINANCES);

        progressBar.setVisibility(View.VISIBLE);

        db.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                final DocumentSnapshot snap_payment = transaction.get(ref_finances.document(transactionID));
                final Payment payment = snap_payment.toObject(Payment.class);
                // Return if payment is already deleted
                if (payment.isDeleted()) return null;

                HashMap<String, Long> map = new HashMap<>();
                long partialPrice = Math.round((double) payment.getPrice() / payment.getInvolvedIDs().size());
                long totalPriceRounded = partialPrice * payment.getInvolvedIDs().size();

                // Read-Operations
                for (String involvedID : payment.getInvolvedIDs()) {
                    DocumentSnapshot snapshot = transaction.get(ref_users.document(involvedID));
                    // aktueller Beteiligter ist ebenfalls Bezahler
                    if (involvedID.equals(payment.getPayerID())) {
                        map.put(involvedID, snapshot.getLong(MONEY) - totalPriceRounded + partialPrice);
                    }
                    // aktueller Beteiligte ist nicht auch Bezahler
                    else map.put(involvedID, snapshot.getLong(MONEY) + partialPrice);
                }

                // Bezahler Geld verrechnen, falls er noch nicht in Involviert-Schleife gemacht
                if (!map.containsKey(payment.getPayerID())) {
                    DocumentSnapshot snapshot = transaction.get(ref_users.document(payment.getPayerID()));
                    map.put(payment.getPayerID(), snapshot.getLong(MONEY) - totalPriceRounded);
                }

                // Write-Operations
                for (String key : map.keySet()) {
                    transaction.update(ref_users.document(key), MONEY, map.get(key));
                }

                // Set Payment as DELETED (-> is not showing anymore)
                HashMap<String, Object> mapDeleted = new HashMap<>();
                mapDeleted.put("deleted", true);
                transaction.update(ref_finances.document(transactionID), mapDeleted);

                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TransactionDetailActivity.this, "Löschen erfolgreich...", Toast.LENGTH_SHORT)
                        .show();
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_payment, menu);
        // Show Delete Button if User is Creator
        if (thisPayment != null && userID != null)
            if (thisPayment.getCreatorID().equals(userID))
                menu.findItem(R.id.action_delete).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                deletePayment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String userID, groupID, transactionID;
    private FirebaseFirestore db;
    private StorageReference storage;
    private Payment thisPayment;
    private HashMap<String, User> users;
    private TaskBeteiligteAdapter adapter_beteiligte;
    private Menu menu;

    private Toolbar toolbar;
    private TextView tv_description, tv_payer;
    private TextView label_price, label_payer, label_involved;
    private MoneyTextView tv_price;
    private CircleImageView img_profile_payer;
    private RecyclerView rec_beteiligte;
    private ProgressBar progressBar;
}
