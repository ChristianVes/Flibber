package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

import christian.eilers.flibber.FirestoreAdapter.StockAdapter;
import christian.eilers.flibber.FirestoreAdapter.TasksAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.StockProduct;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.NAME;
import static christian.eilers.flibber.Utils.Strings.STOCK;
import static christian.eilers.flibber.Utils.Strings.TASKS;
import static christian.eilers.flibber.Utils.Strings.TIMESTAMP;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class StockActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);
        initializeViews();
        initializeVariables();
    }

    // Initialize views
    private void initializeViews() {
        fab = findViewById(R.id.fab);
        toolbar = findViewById(R.id.toolbar);
        recView = findViewById(R.id.recView);
        tv_placeholder = findViewById(R.id.placeholder);
        progressBar = findViewById(R.id.progressBar);
    }

    // Initialize variables
    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        userID = LocalStorage.getUserID(StockActivity.this);
        groupID = LocalStorage.getGroupID(StockActivity.this);
        isInitialized = false;
        if (hasNulls()) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        // user list real time updated
        // data loaded after first time retrieving users
        db.collection(GROUPS).document(groupID).collection(USERS).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    Intent main = new Intent(StockActivity.this, MainActivity.class);
                    main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(main);
                    finish();
                    return;
                }
                retrieveUsers(queryDocumentSnapshots);
                progressBar.setVisibility(View.GONE);
                if (isInitialized) return;
                loadData();
            }
        });
    }

    // load & update the stock items
    private void loadData() {
        isInitialized = true;
        // activate button because user-map is needed to open @StockAddActivity
        fab.setOnClickListener(StockActivity.this);

        final Query query = db.collection(GROUPS).document(groupID)
                .collection(STOCK)
                .orderBy(NAME, Query.Direction.ASCENDING);  // sort after name alphabetically

        final FirestoreRecyclerOptions<StockProduct> options = new FirestoreRecyclerOptions.Builder<StockProduct>()
                .setQuery(query, StockProduct.class)
                .build();

        adapter = new StockAdapter(options, StockActivity.this, userID, groupID, users) {
            @Override
            public void onDataChanged() {
                if (getItemCount() == 0) tv_placeholder.setVisibility(View.VISIBLE);
                else tv_placeholder.setVisibility(View.GONE);
                super.onDataChanged();
            }
        };

        recView.setLayoutManager(new LinearLayoutManager(this));
        recView.setAdapter(adapter);
        recView.setNestedScrollingEnabled(false);
        adapter.startListening();
    }

    // produces a map of users for given snapshots
    private void retrieveUsers(QuerySnapshot documentSnapshots) {
        HashMap<String, User> refreshedUsers = new HashMap<>();
        for(DocumentSnapshot doc : documentSnapshots) {
            User user = doc.toObject(User.class);
            refreshedUsers.put(user.getUserID(), user);
        }
        users = (HashMap<String, User>) refreshedUsers.clone();
    }

    public HashMap<String, User> getUsers() {
        return users;
    }

    // check for null pointers
    private boolean hasNulls() {
        if (userID == null || groupID == null) return true;
        else return false;
    }

    @Override
    public void onClick(View view) {
        // Intent @StockAddActivity
        if (view.getId() == R.id.fab) {
            Intent newTask = new Intent(StockActivity.this, StockAddActivity.class);
            newTask.putExtra(USERS, users);
            startActivity(newTask);
        }
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

    private FloatingActionButton fab;
    private Toolbar toolbar;
    private RecyclerView recView;
    private TextView tv_placeholder;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String userID, groupID;
    private HashMap<String, User> users;
    private StockAdapter adapter;

    private boolean isInitialized = false;
}
