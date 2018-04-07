package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

import christian.eilers.flibber.Adapter.TaskBeteiligteAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.TaskEntry;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;
import static christian.eilers.flibber.Utils.Strings.*;

public class TaskActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);
        initializeVariables();
        initializeViews();
        loadTask();
    }

    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        taskID = getIntent().getExtras().getString(TASKID);
        allUsers = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
        if(taskID == null || allUsers == null) {
            Intent main = new Intent(this, MainActivity.class);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }

    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tv_frequenz = findViewById(R.id.frequenz);
        tv_points = findViewById(R.id.points);
        rec_involved = findViewById(R.id.recView_beteiligte);
        rec_verlauf = findViewById(R.id.recVerlauf);
        progressBar = findViewById(R.id.progressBar);

        rec_involved.setHasFixedSize(true);
        rec_involved.setLayoutManager(new LinearLayoutManager(this));
        rec_verlauf.setLayoutManager(new LinearLayoutManager(this));
    }

    // Load the task information from the database
    private void loadTask() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection(GROUPS).document(groupID).collection(TASKS).document(taskID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // get the current Task
                thisTask = documentSnapshot.toObject(TaskModel.class);
                // Task-Frequenz
                tv_frequenz.setText(thisTask.getFrequenz() +"");
                // Task Points (Aufwand)
                tv_points.setText(thisTask.getPoints() +"");
                setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
                getSupportActionBar().setTitle(thisTask.getTitle()); // Titel des Tasks setzen

                users = new HashMap<>();
                for (String key : allUsers.keySet()) {
                    if (thisTask.getInvolvedIDs().contains(key))
                        users.put(key, allUsers.get(key));
                }
                ArrayList<User> userList = new ArrayList<>(users.values());
                adapter_beteiligte = new TaskBeteiligteAdapter(userList);
                rec_involved.setAdapter(adapter_beteiligte);

                loadEntries();
            }
        });
    }

    private void loadEntries() {
        final Query entryQuery = db.collection(GROUPS).document(groupID)
                .collection(TASKS).document(taskID)
                .collection(ENTRIES).orderBy(TIMESTAMP, Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<TaskEntry> options = new FirestoreRecyclerOptions.Builder<TaskEntry>()
                .setQuery(entryQuery, TaskEntry.class)
                .build();

        adapter_entries = new FirestoreRecyclerAdapter<TaskEntry, EntryHolder>(options) {

            @NonNull
            @Override
            public EntryHolder onCreateViewHolder(@NonNull ViewGroup group, int viewType) {
                View view = LayoutInflater.from(group.getContext()).inflate(R.layout.item_verlauf_tasks, group, false);
                return new EntryHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final EntryHolder holder, int position, @NonNull final TaskEntry model) {
                // USERNAME
                holder.username.setText(users.get(model.getUserID()).getName());

                // TIMESTAMP
                if (model.getTimestamp() != null)
                    holder.datum.setText(
                            DateUtils.getRelativeTimeSpanString(
                                    model.getTimestamp().getTime(),
                                    System.currentTimeMillis(),
                                    DateUtils.DAY_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_RELATIVE)
                    );
            }
        };

        rec_verlauf.setAdapter(adapter_entries);
        adapter_entries.startListening();

        progressBar.setVisibility(View.GONE);
    }

    // Custom Viewholder for the Comments of that Note
    public class EntryHolder extends RecyclerView.ViewHolder {

        TextView username, datum;

        public EntryHolder(final View itemView) {
            super(itemView);
            datum = itemView.findViewById(R.id.datum);
            username = itemView.findViewById(R.id.username);
        }
    }

    private void deleteTask() {
        new MaterialDialog.Builder(TaskActivity.this)
                .title("Aufgabe wirklich löschen?")
                .positiveText("Löschen")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        db.collection(GROUPS).document(groupID).collection(TASKS).document(taskID).delete();
                        finish();
                    }
                })
                .negativeText("Abbrechen")
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_task, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_change:
                Toast.makeText(this, "Noch nicht möglich...", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_delete:
                deleteTask();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter_entries != null) adapter_entries.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter_entries != null) adapter_entries.stopListening();
    }

    private Toolbar toolbar;
    private TextView tv_frequenz, tv_points;
    private RecyclerView rec_involved, rec_verlauf;
    private ProgressBar progressBar;

    private String userID, groupID, taskID;
    private FirebaseFirestore db;
    private TaskModel thisTask;
    private HashMap<String, User> users;
    private HashMap<String, User> allUsers;
    private TaskBeteiligteAdapter adapter_beteiligte;
    private FirestoreRecyclerAdapter adapter_entries;
}
