package christian.eilers.flibber.Home.Finance;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import christian.eilers.flibber.RecyclerAdapter.TaskBeteiligteAdapter;
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
        initializeViews();
        initializeVariables();
    }

    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference().child(PROFILE);
        transactionID = getIntent().getStringExtra(TRANSACTIONID);
        allUsers = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
        if(transactionID == null || allUsers == null || groupID == null || userID == null) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        } else loadTransaction();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tv_description = findViewById(R.id.description);
        tv_payer = findViewById(R.id.username);
        tv_username_from = findViewById(R.id.username_from);
        tv_username_to = findViewById(R.id.username_to);
        tv_price = findViewById(R.id.price);
        img_profile_payer = findViewById(R.id.profile_image);
        img_profile_from = findViewById(R.id.profile_image_from);
        img_profile_to = findViewById(R.id.profile_image_to);
        rec_beteiligte = findViewById(R.id.recView_beteiligte);
        progressBar = findViewById(R.id.progressBar);
        layout_normal = findViewById(R.id.layout_normal);
        layout_ueberweisung = findViewById(R.id.layout_ueberweisung);
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
                // PAYER
                User payer = allUsers.get(thisPayment.getPayerID());
                String[] names_payer = payer.getName().split(" ", 2);
                tv_payer.setText(names_payer[0]);
                tv_username_to.setText(names_payer[0]);
                if (payer.getPicPath() != null) {
                    GlideApp.with(TransactionDetailActivity.this)
                            .load(storage.child(payer.getPicPath()))
                            .placeholder(R.drawable.profile_placeholder)
                            .dontAnimate()
                            .into(img_profile_payer);
                    GlideApp.with(TransactionDetailActivity.this)
                            .load(storage.child(payer.getPicPath()))
                            .placeholder(R.drawable.profile_placeholder)
                            .dontAnimate()
                            .into(img_profile_to);
                }

                // INVOLVED
                final HashMap<String, User> involvedUsers = new HashMap<>();
                for (String key : allUsers.keySet()) {
                    if (thisPayment.getInvolvedIDs().contains(key))
                        involvedUsers.put(key, allUsers.get(key));
                }
                ArrayList<User> involvedUserList = new ArrayList<>(involvedUsers.values());

                String[] names_from = involvedUserList.get(0).getName().split(" ", 2);
                tv_username_from.setText(names_from[0]);
                if (involvedUserList.get(0).getPicPath() != null) {
                    GlideApp.with(TransactionDetailActivity.this)
                            .load(storage.child(involvedUserList.get(0).getPicPath()))
                            .placeholder(R.drawable.profile_placeholder)
                            .dontAnimate()
                            .into(img_profile_from);
                }

                int spanCount = 4;

                rec_beteiligte.setHasFixedSize(true);
                rec_beteiligte.setLayoutManager(new GridLayoutManager(TransactionDetailActivity.this, spanCount));

                adapter_beteiligte = new TaskBeteiligteAdapter(involvedUserList);
                rec_beteiligte.setAdapter(adapter_beteiligte);

                if (involvedUserList.size() == 1) {
                    layout_normal.setVisibility(View.GONE);
                    layout_ueberweisung.setVisibility(View.VISIBLE);
                } else {
                    layout_normal.setVisibility(View.VISIBLE);
                    layout_ueberweisung.setVisibility(View.GONE);
                }

                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // Delete the Payment (-> Recalculate Costs)
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_payment, menu);
        // Show Delete Button if User is Creator &&
        // if Payment is less than 24h old
        if (thisPayment != null && userID != null)
            if (thisPayment.getCreatorID().equals(userID)) {
                long timeDiff = System.currentTimeMillis() - thisPayment.getTimestamp().getTime();
                if (timeDiff < TimeUnit.DAYS.toMillis(1))
                    menu.findItem(R.id.action_delete).setVisible(true);
            }

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
    private HashMap<String, User> allUsers;
    private TaskBeteiligteAdapter adapter_beteiligte;

    private Toolbar toolbar;
    private TextView tv_description, tv_payer, tv_username_from, tv_username_to;
    private MoneyTextView tv_price;
    private CircleImageView img_profile_payer, img_profile_from, img_profile_to;
    private RecyclerView rec_beteiligte;
    private ProgressBar progressBar;
    private LinearLayout layout_normal;
    private RelativeLayout layout_ueberweisung;
}
