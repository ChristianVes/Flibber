package christian.eilers.flibber.Home;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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
        label_verlauf = findViewById(R.id.label_verlauf);
        rec_involved = findViewById(R.id.recView_beteiligte);
        rec_verlauf = findViewById(R.id.recVerlauf);
        et_frequenz = findViewById(R.id.input_frequenz);
        progressBar = findViewById(R.id.progressBar);

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
                setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
                getSupportActionBar().setTitle(thisTask.getTitle()); // Titel des Tasks setzen

                users = new HashMap<>();
                for (String key : allUsers.keySet()) {
                    if (thisTask.getInvolvedIDs().contains(key))
                        users.put(key, allUsers.get(key));
                }
                ArrayList<User> userList = new ArrayList<>(users.values());

                int spanCount = 4;

                rec_involved.setHasFixedSize(true);
                rec_involved.setLayoutManager(new GridLayoutManager(TaskActivity.this, spanCount));

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

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (adapter_entries.getItemCount() > 0) label_verlauf.setVisibility(View.VISIBLE);
                else label_verlauf.setVisibility(View.GONE);
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

    public void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void showEditLayout() {
        tv_frequenz.setVisibility(View.GONE);
        et_frequenz.setVisibility(View.VISIBLE);
        et_frequenz.setText(thisTask.getFrequenz() +"");
        // TODO: show soft Keyboard not working
        showSoftKeyboard(et_frequenz);

        menu.clear();
        getMenuInflater().inflate(R.menu.menu_new_task, menu);
        MenuItem item_save = menu.findItem(R.id.action_save);
        item_save.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanging();
            }
        });
    }

    private void showNormalLayout(long frequenz) {
        et_frequenz.setVisibility(View.GONE);
        tv_frequenz.setVisibility(View.VISIBLE);
        tv_frequenz.setText(frequenz +"");

        menu.clear();
        getMenuInflater().inflate(R.menu.menu_task, menu);
    }

    private void saveChanging() {
        String s_frequenz = et_frequenz.getText().toString().trim();
        if (TextUtils.isEmpty(s_frequenz)) {
            Toast.makeText(this, "Frequenz eingeben...", Toast.LENGTH_SHORT).show();
            return;
        }
        final long frequenz = Long.valueOf(s_frequenz);

        HashMap<String, Object> changings = new HashMap<>();
        changings.put("frequenz", frequenz);

        progressBar.setVisibility(View.VISIBLE);
        DocumentReference doc = db.collection(GROUPS).document(groupID).collection(TASKS).document(taskID);
        doc.update(changings).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                progressBar.setVisibility(View.GONE);
                showNormalLayout(frequenz);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_task, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_change:
                showEditLayout();
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
    private TextView tv_frequenz, label_verlauf;
    private RecyclerView rec_involved, rec_verlauf;
    private EditText et_frequenz;
    private ProgressBar progressBar;

    private String userID, groupID, taskID;
    private FirebaseFirestore db;
    private TaskModel thisTask;
    private HashMap<String, User> users;
    private HashMap<String, User> allUsers;
    private TaskBeteiligteAdapter adapter_beteiligte;
    private FirestoreRecyclerAdapter adapter_entries;
    private Menu menu;
}
