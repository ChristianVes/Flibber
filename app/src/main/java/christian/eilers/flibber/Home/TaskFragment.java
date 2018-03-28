package christian.eilers.flibber.Home;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;

import christian.eilers.flibber.Adapter.TasksAdapter;
import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class TaskFragment extends Fragment implements View.OnClickListener{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mainView = inflater.inflate(R.layout.fragment_task, container, false);
        initializeViews();
        initializeVariables();
        if(users != null) {
            loadTasks();
        }
        else {
            Intent main = new Intent(getContext(), MainActivity.class);
            startActivity(main);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
        }
        return mainView;
    }

    private void initializeViews() {
        recView = mainView.findViewById(R.id.recView);
        fab = mainView.findViewById(R.id.fab);
        progressBar = mainView.findViewById(R.id.progressBar);
        toolbar = mainView.findViewById(R.id.toolbar);

        fab.setOnClickListener(this);
    }

    // Initialize variables
    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        userID = LocalStorage.getUserID(getContext());
        groupID = LocalStorage.getGroupID(getContext());
        users = ((HomeActivity) getActivity()).getUsers();
    }

    private void loadTasks() {
        Query query = db.collection(GROUPS).document(groupID)
                .collection(TASKS)
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING); // order by date

        FirestoreRecyclerOptions<TaskModel> options = new FirestoreRecyclerOptions.Builder<TaskModel>()
                .setQuery(query, TaskModel.class)
                .build();

        adapter = new TasksAdapter(options, userID, users);

        recView.setLayoutManager(new LinearLayoutManager(getContext()));
        recView.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fab) {
            Intent newTask = new Intent(getContext(), NewTaskActivity.class);
            getActivity().startActivity(newTask);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    private View mainView;
    private RecyclerView recView;
    private TasksAdapter adapter;
    private FloatingActionButton fab;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private String userID, groupID;
    private HashMap<String, User> users;

    private final String GROUPS = "groups";
    private final String USERS = "users";
    private final String TASKS = "tasks";
    private final String TIMESTAMP = "timestamp";

}
