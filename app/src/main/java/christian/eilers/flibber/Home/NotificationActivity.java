package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;

import christian.eilers.flibber.FirestoreAdapter.NotificationAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.NotificationModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.NOTIFICATIONS;
import static christian.eilers.flibber.Utils.Strings.TIMESTAMP;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class NotificationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        initializeViews();
        initializeVariables();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recView = findViewById(R.id.recVerlauf);
        progressBar = findViewById(R.id.progressBar);
        placeholder = findViewById(R.id.placeholder_notifications);
        // setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
    }

    // Initialize variables
    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        users = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
        if(users == null || groupID == null || userID == null) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
        else loadData();
    }

    // Load all transactions/payments
    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        Query query = db.collection(GROUPS).document(groupID).collection(USERS).document(userID)
                .collection(NOTIFICATIONS).orderBy(TIMESTAMP, Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<NotificationModel> options = new FirestoreRecyclerOptions.Builder<NotificationModel>()
                .setQuery(query, NotificationModel.class)
                .build();

        adapter = new NotificationAdapter(options, users) {
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }
        };

        recView.setLayoutManager(new LinearLayoutManager(NotificationActivity.this));
        //recView.addItemDecoration(new DividerItemDecoration(recView.getContext(), DividerItemDecoration.VERTICAL));
        recView.setAdapter(adapter);
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
    private FirestoreRecyclerAdapter adapter;

    private Toolbar toolbar;
    private RecyclerView recView;
    private ProgressBar progressBar;
    private TextView placeholder;
}
