package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class StockActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);
        initializeViews();
        initializeVariables();
    }

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

    private void loadData() {
        isInitialized = true;
        fab.setOnClickListener(StockActivity.this);
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

    // TODO: Adapter--
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private FloatingActionButton fab;
    private Toolbar toolbar;
    private RecyclerView recView;
    private TextView tv_placeholder;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String userID, groupID;
    private HashMap<String, User> users;

    private boolean isInitialized = false;
}
