package christian.eilers.flibber.Home;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.HashMap;

import christian.eilers.flibber.FirestoreAdapter.NoteAdapter2;
import christian.eilers.flibber.FirestoreAdapter.NotificationAdapter;
import christian.eilers.flibber.FirestoreAdapter.TasksAdapter2;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Note;
import christian.eilers.flibber.Models.NotificationModel;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.ProfileActivity;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.NOTES;
import static christian.eilers.flibber.Utils.Strings.NOTIFICATIONS;
import static christian.eilers.flibber.Utils.Strings.ONE_DAY;
import static christian.eilers.flibber.Utils.Strings.ONE_WEEK;
import static christian.eilers.flibber.Utils.Strings.TASKS;
import static christian.eilers.flibber.Utils.Strings.TIMESTAMP;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class HomeFragment2 extends Fragment implements View.OnClickListener{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_home2, container, false);
        initializeViews();
        initializeVariables();
        if (hasNulls()) {
            Intent main = new Intent(getContext(), MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
        } else {
            tv_groupname.setText("Headquarter");
            loadNotes();
            loadTasks();
            loadEvents();
        }

        return mainView;
    }

    // Initialize views from layout file
    private void initializeViews() {
        recView_notes = mainView.findViewById(R.id.recNotes);
        recView_tasks = mainView.findViewById(R.id.recTasks);
        recView_events = mainView.findViewById(R.id.recEvents);
        tv_notes = mainView.findViewById(R.id.label_notes);
        tv_tasks = mainView.findViewById(R.id.label_tasks);
        tv_events =mainView.findViewById(R.id.label_events);
        tv_groupname = mainView.findViewById(R.id.group_name);
        placeholder_notes = mainView.findViewById(R.id.placeholder_notes);
        placeholder_tasks = mainView.findViewById(R.id.placeholder_tasks);
        placeholder_events = mainView.findViewById(R.id.placeholder_events);
        btn_note = mainView.findViewById(R.id.btn_note);
        btn_tasks = mainView.findViewById(R.id.btn_task);
        btn_settings = mainView.findViewById(R.id.btn_settings);
        progressBar = mainView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        btn_note.setOnClickListener(this);
        btn_tasks.setOnClickListener(this);
        btn_settings.setOnClickListener(this);
        tv_notes.setOnClickListener(this);
        tv_tasks.setOnClickListener(this);
        tv_events.setOnClickListener(this);
    }

    // Initialize variables
    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        userID = LocalStorage.getUserID(getContext());
        groupID = LocalStorage.getGroupID(getContext());
        groupname = LocalStorage.getGroupName(getContext());
        users = ((HomeActivity) getActivity()).getUsers();
    }

    // check for null pointers
    private boolean hasNulls() {
        if (users == null || userID == null || groupID == null || groupname == null) return true;
        else return false;
    }

    // load notes from database to the recyclerView including an update listener
    private void loadNotes() {
        Query notesQuery;
        if (users.get(userID).getTimestamp() != null)
            notesQuery = db.collection(GROUPS).document(groupID).collection(NOTES)
                    .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                    .whereGreaterThan(TIMESTAMP, new Date(users.get(userID).getTimestamp().getTime() - 2 * ONE_DAY));

        else
            notesQuery = db.collection(GROUPS).document(groupID).collection(NOTES)
                    .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                    .whereGreaterThan(TIMESTAMP, new Date(System.currentTimeMillis() - 2 * ONE_DAY));

        FirestoreRecyclerOptions<Note> options = new FirestoreRecyclerOptions.Builder<Note>()
                .setQuery(notesQuery, Note.class)
                .build();

        adapter_notes = new NoteAdapter2(options, users, groupID) {
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder_notes.setVisibility(View.VISIBLE);
                else placeholder_notes.setVisibility(View.GONE);
                progressBar.setVisibility(View.INVISIBLE);
            }
        };

        recView_notes.setLayoutManager(new LinearLayoutManager(getContext()));
        recView_notes.setAdapter(adapter_notes);
    }

    private void loadTasks() {
        final Query query = db.collection(GROUPS).document(groupID)
                .collection(TASKS)
                .whereLessThan(TIMESTAMP, new Date(System.currentTimeMillis() + 2 * ONE_DAY))
                .orderBy(TIMESTAMP, Query.Direction.ASCENDING);

        final FirestoreRecyclerOptions<TaskModel> options = new FirestoreRecyclerOptions.Builder<TaskModel>()
                .setQuery(query, TaskModel.class)
                .build();

        adapter_tasks = new TasksAdapter2(options, users, userID, groupID) {
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                int count = 0; // number of visibile items
                for (int i = 0; i < getItemCount(); i++) {
                    if (getItemViewType(i) == 1) count++;
                }
                if (count == 0) placeholder_tasks.setVisibility(View.VISIBLE);
                else placeholder_tasks.setVisibility(View.GONE);
                progressBar.setVisibility(View.INVISIBLE);
            }
        };

        recView_tasks.setLayoutManager(new LinearLayoutManager(getContext()));
        recView_tasks.setAdapter(adapter_tasks);
    }

    private void loadEvents() {
        final Query query = db.collection(GROUPS).document(groupID)
                .collection(USERS).document(userID).collection(NOTIFICATIONS)
                .whereGreaterThan(TIMESTAMP, new Date(System.currentTimeMillis() - ONE_DAY))
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING);

        final FirestoreRecyclerOptions<NotificationModel> options = new FirestoreRecyclerOptions.Builder<NotificationModel>()
                .setQuery(query, NotificationModel.class)
                .build();

        adapter_events = new NotificationAdapter(options, users) {
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder_events.setVisibility(View.VISIBLE);
                else placeholder_events.setVisibility(View.GONE);
            }
        };

        recView_events.setLayoutManager(new LinearLayoutManager(getContext()));
        recView_events.setAdapter(adapter_events);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_note:
                Intent intent_newNote = new Intent(getContext(), NoteCreateActivity.class);
                getActivity().startActivity(intent_newNote);
                break;
            case R.id.label_notes:
                Intent intent_more = new Intent(getContext(), NotesVerlaufActivity.class);
                intent_more.putExtra(USERS, ((HomeActivity) getActivity()).getUsers());
                getActivity().startActivity(intent_more);
                break;
            case R.id.btn_task:
                //TODO
                break;
            case R.id.label_tasks:
                ((HomeActivity) getActivity()).setTabPosition(3);
                break;
            case R.id.label_events:
                //TODO
                break;
            case R.id.btn_settings:
                Intent intent_settings = new Intent(getContext(), SettingsActivity.class);
                getActivity().startActivity(intent_settings);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter_notes != null) adapter_notes.startListening();
        if (adapter_tasks != null) adapter_tasks.startListening();
        if (adapter_events != null) adapter_events.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter_notes != null) adapter_notes.stopListening();
        if (adapter_tasks != null) adapter_tasks.stopListening();
        if (adapter_events != null) adapter_events.stopListening();
        if (!hasNulls()) {
            HashMap<String, Object> timestamp = new HashMap<>();
            timestamp.put(TIMESTAMP, FieldValue.serverTimestamp());
            FirebaseFirestore.getInstance().collection(GROUPS).document(groupID).collection(USERS).document(userID)
                    .update(timestamp);
        }
    }

    private View mainView;
    private RecyclerView recView_notes, recView_tasks, recView_events;
    private TextView tv_notes, tv_tasks, tv_events, tv_groupname;
    private TextView placeholder_notes, placeholder_tasks, placeholder_events;
    private ImageButton btn_note, btn_tasks, btn_settings;
    private ProgressBar progressBar;

    private FirestoreRecyclerAdapter adapter_notes, adapter_tasks, adapter_events;
    private FirebaseFirestore db;
    private String userID, groupID, groupname;
    private HashMap<String, User> users;
}
