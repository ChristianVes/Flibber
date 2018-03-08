package christian.eilers.flibber.ProfilAndWgs;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.Models.Group;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class GroupFragment extends Fragment implements View.OnClickListener{

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_groups, container, false);
        userID = LocalStorage.getUserID(getContext());
        initializeViews();
        loadData();
        return mainView;
    }

    // Initialize views from layout file
    private void initializeViews() {
        recView = mainView.findViewById(R.id.recView);
        fab_new = mainView.findViewById(R.id.fab_new);
        fab_invitations = mainView.findViewById(R.id.fab_invitations);
        placeholder = mainView.findViewById(R.id.placeholder);
        progressBar = mainView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        fab_new.setOnClickListener(this);
        fab_invitations.setOnClickListener(this);
    }

    // Lädt Gruppen des Users aus der Datenbank in den RecyclerView und hält sie up-to-date über einen Listener
    private void loadData() {
        Query groupsQuery = FirebaseFirestore.getInstance()
                .collection(USERS)
                .document(userID)
                .collection(GROUPS)
                .orderBy(TIMESTAMP);

        FirestoreRecyclerOptions<Group> options = new FirestoreRecyclerOptions.Builder<Group>()
                .setQuery(groupsQuery, Group.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Group, GroupFragment.GroupHolder>(options) {

            // Aktualisiere Platzhalter und ProgressBar
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }

            // Bind data from the database to the UI-Object
            @Override
            public void onBindViewHolder(GroupFragment.GroupHolder holder, int position, Group model) {
                holder.v_name.setText(model.getName());
                holder.group = model;
            }

            // Einmalige Zuweisung zum ViewHolder: GroupHolder
            @Override
            public GroupFragment.GroupHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext()).inflate(R.layout.item_wg, group, false);
                return new GroupFragment.GroupHolder(view);
            }
        };

        recView.setLayoutManager(new LinearLayoutManager(getContext()));
        recView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fab_new) {
            GroupCreationFragment frag = new GroupCreationFragment();
            frag.setTargetFragment(GroupFragment.this, GROUP_CREATE_REQUESTCODE);
            frag.show(getFragmentManager(), "wg_erstellen");
        }
        else if(id == R.id.fab_invitations) {
            InvitationsFragment frag = new InvitationsFragment();
            frag.setTargetFragment(GroupFragment.this, INVITATIONS_REQUESTCODE);
            frag.show(getFragmentManager(), "einladungen");
        }
    }

    // Custom ViewHolder for interacting with single items of the RecyclerView
    public class GroupHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView v_name;
        Group group;

        public GroupHolder(View itemView) {
            super(itemView);
            v_name = itemView.findViewById(R.id.wg_name);
            itemView.setOnClickListener(this);
        }

        // Wechsel zur HomeActivity
        @Override
        public void onClick(View view) {
            LocalStorage.setGroupID(getContext(), group.getKey());
            Intent homeIntent = new Intent(getContext(), HomeActivity.class);
            startActivity(homeIntent);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }

    private View mainView;
    private RecyclerView recView;
    private TextView placeholder;
    private FloatingActionButton fab_new, fab_invitations;
    private ProgressBar progressBar;

    private FirestoreRecyclerAdapter adapter;
    private String userID;

    private final int GROUP_CREATE_REQUESTCODE = 1;
    private final int INVITATIONS_REQUESTCODE = 2;
    private final String GROUPS = "wgs";
    private final String USERS = "users";
    private final String TIMESTAMP = "timestamp";
}
