package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.fabiomsr.moneytextview.MoneyTextView;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import christian.eilers.flibber.Adapter.TaskBeteiligteAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

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
                if (user.getPicPath() != null)
                    GlideApp.with(TransactionDetailActivity.this)
                            .load(storage.child(user.getPicPath()))
                            .placeholder(R.drawable.profile_placeholder)
                            .dontAnimate()
                            .into(img_profile_payer);
            }
        }
        users = (HashMap<String, User>) userHashMap.clone();
    }


    private final String TRANSACTIONID = "transactionID";
    private final String USERS = "users";
    private final String GROUPS = "groups";
    private final String FINANCES = "finances";
    private final String PROFILE = "profile";

    private String userID, groupID, transactionID;
    private FirebaseFirestore db;
    private StorageReference storage;
    private Payment thisPayment;
    private HashMap<String, User> users;
    private TaskBeteiligteAdapter adapter_beteiligte;

    private Toolbar toolbar;
    private TextView tv_description, tv_payer;
    private MoneyTextView tv_price;
    private CircleImageView img_profile_payer;
    private RecyclerView rec_beteiligte;
    private ProgressBar progressBar;
}
