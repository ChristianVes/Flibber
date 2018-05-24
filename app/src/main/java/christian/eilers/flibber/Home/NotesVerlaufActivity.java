package christian.eilers.flibber.Home;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;

import christian.eilers.flibber.FirestoreAdapter.NoteAdapter;
import christian.eilers.flibber.FirestoreAdapter.VerlaufAdapter;
import christian.eilers.flibber.FirestoreAdapter.VerlaufAllAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Note;
import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.FINANCES;
import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.NOTES;
import static christian.eilers.flibber.Utils.Strings.TIMESTAMP;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class NotesVerlaufActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verlauf_notes);
        initializeViews();
        initializeVariables();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recView = findViewById(R.id.recVerlauf);
        progressBar = findViewById(R.id.progressBar);
        placeholder = findViewById(R.id.placeholder_notes);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent_newNote = new Intent(NotesVerlaufActivity.this, NoteCreateActivity.class);
                startActivity(intent_newNote);
            }
        });
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
        Query notesQuery = db.collection(GROUPS).document(groupID).collection(NOTES)
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Note> options = new FirestoreRecyclerOptions.Builder<Note>()
                .setQuery(notesQuery, Note.class)
                .build();

        adapter = new NoteAdapter(options, users, groupID) {
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }
        };

        recView.setLayoutManager(new LinearLayoutManager(NotesVerlaufActivity.this));
        recView.addItemDecoration(new DividerItemDecoration(recView.getContext(), DividerItemDecoration.VERTICAL));
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
    private FloatingActionButton fab;
}
