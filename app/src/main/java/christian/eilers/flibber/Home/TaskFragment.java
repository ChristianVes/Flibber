package christian.eilers.flibber.Home;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;

import christian.eilers.flibber.MainActivity;
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
            // TODO: LOAD
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

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fab) {
            Intent newTask = new Intent(getContext(), NewTaskActivity.class);
            getActivity().startActivity(newTask);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }

    private View mainView;
    private RecyclerView recView;
    private FloatingActionButton fab;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private String userID, groupID;
    private HashMap<String, User> users;

}
