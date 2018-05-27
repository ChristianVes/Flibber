package christian.eilers.flibber.Home.Finance;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;

import christian.eilers.flibber.FirestoreAdapter.VerlaufAdapter;
import christian.eilers.flibber.FirestoreAdapter.VerlaufAllAdapter;
import christian.eilers.flibber.MainActivity;
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
        tv_placeholder = findViewById(R.id.placeholder_verlauf);
        setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
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
        else loadVerlauf();
    }

    // Load all transactions/payments
    private void loadVerlauf() {
        query = db.collection(GROUPS).document(groupID)
                .collection(FINANCES).orderBy(TIMESTAMP, Query.Direction.DESCENDING);   // order by Date

        recyclerOptions = new FirestoreRecyclerOptions.Builder<Payment>()
                .setQuery(query, Payment.class)
                .build();

        adapter = new VerlaufAdapter(recyclerOptions, userID, users) {
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) tv_placeholder.setVisibility(View.VISIBLE);
                else tv_placeholder.setVisibility(View.GONE);
            }
        };
        adapter_all = new VerlaufAllAdapter(recyclerOptions, userID, users);

        recView.setLayoutManager(new LinearLayoutManager(this));
        recView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_finance_verlauf, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_all:
                recView.setAdapter(adapter_all);
                item.setVisible(false);
                menu.findItem(R.id.action_mine).setVisible(true);
                return true;
            case R.id.action_mine:
                recView.setAdapter(adapter);
                item.setVisible(false);
                menu.findItem(R.id.action_all).setVisible(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter_all != null) adapter_all.startListening();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter_all != null) adapter_all.stopListening();
        if (adapter != null) adapter.stopListening();
    }

    private FirebaseFirestore db;
    private String userID, groupID;
    private HashMap<String, User> users;
    private VerlaufAdapter adapter;
    private VerlaufAllAdapter adapter_all;
    private Query query;
    private FirestoreRecyclerOptions<Payment> recyclerOptions;

    private Toolbar toolbar;
    private RecyclerView recView;
    private TextView tv_placeholder;
    private Menu menu;
}
