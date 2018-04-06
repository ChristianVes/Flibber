package christian.eilers.flibber.Home;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

import christian.eilers.flibber.Adapter.VerlaufAdapter;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.*;

public class VerlaufActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verlauf);
        initializeViews();
        initializeVariables();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recView = findViewById(R.id.recVerlauf);
        progressBar = findViewById(R.id.progressBar);
    }

    // Initialize variables
    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);

        progressBar.setVisibility(View.VISIBLE);
        Query usersQuery = db.collection(GROUPS).document(groupID).collection(USERS);
        usersQuery.addSnapshotListener(VerlaufActivity.this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                // Toast.makeText(VerlaufActivity.this, "Update", Toast.LENGTH_SHORT).show();
                retrieveUsers(documentSnapshots);
                progressBar.setVisibility(View.GONE);
                if (adapter == null) loadVerlauf();
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

    // Load all transactions/payments
    private void loadVerlauf() {
        Query query = db.collection(GROUPS).document(groupID)
                .collection(FINANCES).orderBy(TIMESTAMP, Query.Direction.DESCENDING);   // order by Date

        FirestoreRecyclerOptions<Payment> options = new FirestoreRecyclerOptions.Builder<Payment>()
                .setQuery(query, Payment.class)
                .build();

        adapter = new VerlaufAdapter(options, userID, users);

        recView.setLayoutManager(new LinearLayoutManager(this));
        recView.setAdapter(adapter);
        recView.setNestedScrollingEnabled(false);
        adapter.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    private FirebaseFirestore db;
    private String userID, groupID;
    private HashMap<String, User> users;
    private VerlaufAdapter adapter;

    private Toolbar toolbar;
    private RecyclerView recView;
    private ProgressBar progressBar;
}
