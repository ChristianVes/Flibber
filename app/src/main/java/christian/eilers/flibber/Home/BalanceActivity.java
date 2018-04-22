package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;

import christian.eilers.flibber.Adapter.BalanceOffsetAdapter;
import christian.eilers.flibber.Adapter.BalanceUserAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Balancing;
import christian.eilers.flibber.Models.Offset;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;
import static christian.eilers.flibber.Utils.Strings.*;

public class BalanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance);
        initializeViews();
        initializeVariables();
        if (hasNulls()) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
        else loadBalance();
    }

    // Initialize Views
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recView = findViewById(R.id.recView);
        recView_offsets = findViewById(R.id.recView_offsets);
        progressBar = findViewById(R.id.progressBar);
        setSupportActionBar(toolbar);
    }

    // Initialize Variables
    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        balancingID = getIntent().getStringExtra(BALANCING);
        users = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
    }

    // check for null pointers
    private boolean hasNulls() {
        if (balancingID == null ||users == null || userID == null || groupID == null) return false;
        else return true;
    }

    // Load the Balance & possible Offsets
    private void loadBalance() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection(GROUPS).document(groupID).collection(BALANCING).document(balancingID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Balancing thisBalancing = documentSnapshot.toObject(Balancing.class);
                // DATE (ActionBar Title)
                String date = DateFormat.getDateInstance(DateFormat.LONG).format(thisBalancing.getTimestamp());
                getSupportActionBar().setTitle(date);
                // USER-LIST
                final ArrayList<User> list_users = new ArrayList<>();
                for (String key : thisBalancing.getValues().keySet()) {
                    User user = users.get(key);
                    user.setMoney(thisBalancing.getValues().get(key));
                    list_users.add(user);
                }
                // Sorting by money
                Collections.sort(list_users, new Comparator<User>() {
                    @Override
                    public int compare(User user2, User user1) {
                        if (user1.getMoney() < user2.getMoney()) return -1;
                        return 1;
                    }
                });
                // Display user and their balance in a recyclerView
                recView.setHasFixedSize(true);
                recView.setLayoutManager(new LinearLayoutManager(BalanceActivity.this));
                BalanceUserAdapter adapter = new BalanceUserAdapter(list_users);
                recView.setAdapter(adapter);
                progressBar.setVisibility(View.GONE);
            }
        });

        // Load possible offsets from the sub-collection
        // order by name
        db.collection(GROUPS).document(groupID).collection(BALANCING).document(balancingID)
                .collection(ENTRIES).orderBy("fromID").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // retrieve all offset-payments to be payed
                final ArrayList<Offset> list_offsets = new ArrayList<>();
                for (QueryDocumentSnapshot snap : queryDocumentSnapshots) {
                    list_offsets.add(snap.toObject(Offset.class));
                }
                recView_offsets.setHasFixedSize(true);
                recView_offsets.setLayoutManager(new LinearLayoutManager(BalanceActivity.this));
                BalanceOffsetAdapter adapter = new BalanceOffsetAdapter(users, list_offsets);
                recView_offsets.setAdapter(adapter);
            }
        });
    }

    private Toolbar toolbar;
    private RecyclerView recView, recView_offsets;
    private ProgressBar progressBar;

    private String userID, groupID, balancingID;
    private FirebaseFirestore db;
    private HashMap<String, User> users;
}
