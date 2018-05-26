package christian.eilers.flibber.Home;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.NotificationModel;
import christian.eilers.flibber.Models.TaskEntry;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.RecyclerAdapter.TaskInvolvedAdapter;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.ENTRIES;
import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.INVOLVEDIDS;
import static christian.eilers.flibber.Utils.Strings.NOTIFICATIONS;
import static christian.eilers.flibber.Utils.Strings.TASKID;
import static christian.eilers.flibber.Utils.Strings.TASKS;
import static christian.eilers.flibber.Utils.Strings.TIMESTAMP;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class TaskActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);
        initializeViews();
        initializeVariables();
        if (hasNulls()) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        } else loadTask();
    }

    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        functions = FirebaseFunctions.getInstance();
        thisTask = (TaskModel) getIntent().getSerializableExtra(TASKID);
        users = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
    }

    // check for null pointers
    private boolean hasNulls() {
        if (thisTask == null || users == null || userID == null || groupID == null) return true;
        else return false;
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tv_frequency = findViewById(R.id.tv_frequency);
        tv_ordered = findViewById(R.id.tv_order);
        placeholder = findViewById(R.id.placeholder);
        rec_involved = findViewById(R.id.recView_involved);
        rec_verlauf = findViewById(R.id.recView_verlauf);
        progressBar = findViewById(R.id.progressBar);

        rec_verlauf.setLayoutManager(new LinearLayoutManager(this));
        rec_verlauf.addItemDecoration(new DividerItemDecoration(rec_verlauf.getContext(), DividerItemDecoration.VERTICAL));
        rec_involved.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rec_involved.setHasFixedSize(true);
    }

    // Load the task information from the database
    private void loadTask() {
        // Frequency
        if (thisTask.getFrequenz() == 1) tv_frequency.setText("Täglich");
        else tv_frequency.setText(thisTask.getFrequenz() +" Tage");
        // Order
        if (thisTask.isOrdered()) tv_ordered.setText("Ja");
        else tv_ordered.setText("Nein");
        // Title
        setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
        getSupportActionBar().setTitle(thisTask.getTitle()); // Titel des Tasks setzen

        // Involved users
        ArrayList<User> userList = new ArrayList<>();
        for (String key : thisTask.getInvolvedIDs()) userList.add(users.get(key));

        adapter_involved = new TaskInvolvedAdapter(userList);
        rec_involved.setAdapter(adapter_involved);

        loadEntries();

        // Listener for this task
        db.collection(GROUPS).document(groupID).collection(TASKS).document(thisTask.getKey()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null || !documentSnapshot.exists()) return;
                thisTask = documentSnapshot.toObject(TaskModel.class);
                // Frequency
                if (thisTask.getFrequenz() == 1) tv_frequency.setText("Täglich");
                else tv_frequency.setText(thisTask.getFrequenz() +" Tage");
                // Involved users
                ArrayList<User> userList = new ArrayList<>();
                for (String key : thisTask.getInvolvedIDs()) userList.add(users.get(key));
                adapter_involved = new TaskInvolvedAdapter(userList);
                rec_involved.setAdapter(adapter_involved);
            }
        });
    }

    private void loadEntries() {
        final Query entryQuery = db.collection(GROUPS).document(groupID)
                .collection(TASKS).document(thisTask.getKey())
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
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
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
                        db.collection(GROUPS).document(groupID).collection(TASKS).document(thisTask.getKey()).delete();
                        finish();
                    }
                })
                .negativeText("Abbrechen")
                .show();
    }

    private void changeFrequency() {
        new MaterialDialog.Builder(this)
                .title("Frequenz ändern")
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .input("Frequenz in Tagen", null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                    }
                })
                .positiveText("Speichern")
                .negativeText("Abbrechen")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction which) {
                        final String s_frequency = dialog.getInputEditText().getText().toString().trim();
                        if(TextUtils.isEmpty(s_frequency)) {
                            dialog.dismiss();
                            return;
                        }
                        final long frequency = Long.valueOf(s_frequency);

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("frequenz", frequency);

                        DocumentReference doc_task = db.collection(GROUPS).document(groupID).collection(TASKS).document(thisTask.getKey());

                        WriteBatch batch = db.batch();
                        String not_description = "Frequenz von \"" + thisTask.getTitle() + "\" geändert";
                        for (String id : thisTask.getInvolvedIDs()) {
                            if (id.equals(userID)) continue;
                            DocumentReference doc = db.collection(GROUPS).document(groupID).collection(USERS).document(id).collection(NOTIFICATIONS).document();
                            NotificationModel not = new NotificationModel(doc.getId(), not_description, TASKS, userID);
                            batch.set(doc, not);
                        }
                        batch.update(doc_task, map);
                        batch.commit();
                        dialog.dismiss();
                    }
                })
                .show();
    }

    // Notifiy the first User in the Involved User's List of the current Task
    private void remindNotification() {
        Map<String, Object> data = new HashMap<>();
        data.put("taskName", thisTask.getTitle());
        data.put("groupID", groupID);

        if (thisTask.isOrdered()) {
            data.put("userID", thisTask.getInvolvedIDs().get(0));
            // Calls the Http Function which makes the Notification
            functions.getHttpsCallable("taskNotify").call(data);
        } else {
            data.put("involvedIDs", thisTask.getInvolvedIDs());
            // Calls the Http Function which makes the Notification
            functions.getHttpsCallable("taskNotifyAll").call(data);
        }
        Toast.makeText(TaskActivity.this, "Benachrichtigung gesendet!", Toast.LENGTH_SHORT)
                .show();
    }

    // Skip the current User and put him on the second Position in the involved User's List
    private void skip() {
        // check state to avoid null pointer (should never be the case)
        if (thisTask.getInvolvedIDs().size() == 1 || !thisTask.isOrdered() || !thisTask.getInvolvedIDs().get(0).equals(userID)) {
            Toast.makeText(this, "Nicht möglich!", Toast.LENGTH_SHORT).show();
            return;
        }
        menu.findItem(R.id.action_skip).setVisible(false);
        // Notify the second user
        final String toUserID = thisTask.getInvolvedIDs().get(1);
        final Map<String, Object> data = new HashMap<>();
        data.put("taskName", thisTask.getTitle());
        data.put("groupID", groupID);
        data.put("userID", toUserID);

        // Calls the Http Function which makes the Notification
        // functions.getHttpsCallable("taskSkipped").call(data);

        // Change the current's user position with the user after him
        ArrayList<String> newOrder = (ArrayList<String>) thisTask.getInvolvedIDs().clone();
        newOrder.remove(userID);
        newOrder.add(1, userID);

        final HashMap<String, Object> taskMap = new HashMap<>();
        taskMap.put(INVOLVEDIDS, newOrder);

        final DocumentReference doc_task = FirebaseFirestore.getInstance().collection(GROUPS).document(groupID).collection(TASKS).document(thisTask.getKey());
        final DocumentReference doc_not = db.collection(GROUPS).document(groupID).collection(USERS).document(toUserID).collection(NOTIFICATIONS).document();

        final String not_description = "hat \"" + thisTask.getTitle() + "\" an dich weitergegeben";
        final NotificationModel not = new NotificationModel(doc_not.getId(), not_description, TASKS, userID);

        WriteBatch batch = db.batch();
        batch.update(doc_task, taskMap);
        batch.set(doc_not, not);
        batch.commit();
    }

    private void datePicker() {
        DialogFragment dateDialog = DatePickerFragment.newInstance(groupID, thisTask);
        dateDialog.show(getSupportFragmentManager(), "datePicker");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_task, menu);
        // Skip - Visibility
        if (thisTask.isOrdered() && thisTask.getInvolvedIDs().get(0).equals(userID) && thisTask.getInvolvedIDs().size() > 1)
            menu.findItem(R.id.action_skip).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_skip:
                skip();
                menu.findItem(R.id.action_skip).setVisible(false);
                return true;
            case R.id.action_remind:
                remindNotification();
                return true;
            case R.id.action_date:
                datePicker();
                return true;
            case R.id.action_change:
                changeFrequency();
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
    private TextView tv_frequency, tv_ordered, placeholder;
    private RecyclerView rec_involved, rec_verlauf;
    private ProgressBar progressBar;

    private String userID, groupID;
    private FirebaseFirestore db;
    private FirebaseFunctions functions;
    private TaskModel thisTask;
    private HashMap<String, User> users;
    private TaskInvolvedAdapter adapter_involved;
    private FirestoreRecyclerAdapter adapter_entries;
    private Menu menu;
}
