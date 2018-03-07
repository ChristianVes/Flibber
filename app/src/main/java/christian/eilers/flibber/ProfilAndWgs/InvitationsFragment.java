package christian.eilers.flibber.ProfilAndWgs;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import christian.eilers.flibber.Models.Group;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class InvitationsFragment extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.dialog_einladungen, container, false);
        initializeViews();
        getDialog().setTitle("Einladungen");
        userID = LocalStorage.getUserID(getContext());
        db = FirebaseFirestore.getInstance();
        loadData();
        return mainView;
    }

    // Initialize views from layout file
    private void initializeViews() {
        recView = mainView.findViewById(R.id.recView);
        placeholder = mainView.findViewById(R.id.placeholder);
        progressBar = mainView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
    }

    // Lädt Einladungen des Users aus der Datenbank in den RecyclerView und hält sie up-to-date über einen Listener
    private void loadData() {
        Query invitesQuery = FirebaseFirestore.getInstance()
                .collection(USERS)
                .document(userID)
                .collection(INVITATIONS)
                .orderBy(TIMESTAMP);

        FirestoreRecyclerOptions<Group> options = new FirestoreRecyclerOptions.Builder<Group>()
                .setQuery(invitesQuery, Group.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Group, InvitationsFragment.GroupHolder>(options) {

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
            public void onBindViewHolder(InvitationsFragment.GroupHolder holder, int position, Group model) {
                holder.group = model;
                holder.v_name.setText(model.getName());
            }

            // Einmalige Zuweisung zum ViewHolder: GroupHolder
            @Override
            public InvitationsFragment.GroupHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext()).inflate(R.layout.item_wg, group, false);
                return new InvitationsFragment.GroupHolder(view);
            }
        };

        adapter.startListening();
        recView.setLayoutManager(new LinearLayoutManager(getContext()));
        recView.setAdapter(adapter);
    }

    // Custom ViewHolder for interacting with single items of the RecyclerView
    public class GroupHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView v_name;
        Group group;

        public GroupHolder(View itemView) {
            super(itemView);
            v_name = itemView.findViewById(R.id.wg_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            joinWG(group.getKey());
        }
    }

    // Join the Group with the given Group ID
    private void joinWG(final String groupID) {
        progressBar.setVisibility(View.VISIBLE);

        db.collection(USERS).document(userID).collection(GROUPS).document(groupID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                // Überprüfe ob der User bereits Mitglied in der Gruppe ist
                if (task.getResult().exists()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Already member of this Group", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.collection(GROUPS).document(groupID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        // Überprüfe ob die Gruppe noch existiert
                        if (!document.exists()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Group does not exist", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Add Group-Reference to the user
                        final Group group = document.toObject(Group.class);
                        db.collection(USERS).document(userID).collection(GROUPS).document(group.getKey()).set(group);
                        // User zur Gruppe hinzufügen
                        addUserToGroup(groupID);
                    }
                });
            }
        });
    }

    // Add current User to the Group
    private void addUserToGroup(final String groupID) {
        db.collection(USERS).document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                final String username = task.getResult().getString(NAME);
                final String email = task.getResult().getString(EMAIL);
                final String picPath = task.getResult().getString(PICPATH);
                final User user = new User(username, email, userID, picPath, 0.0);
                db.collection(GROUPS).document(groupID).collection(USERS).document(userID).set(user);
                deleteInvitation(groupID);
            }
        });
    }

    // Delete Invitation Documents
    private void deleteInvitation(final String groupID) {
        db.collection(USERS).document(userID).collection(INVITATIONS).document(groupID).delete();
        db.collection(GROUPS).document(groupID).collection(INVITATIONS).document(userID).delete();
        Toast.makeText(getContext(), "Erfolgreich beigetreten", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(adapter != null) adapter.stopListening();
    }

    private View mainView;
    private RecyclerView recView;
    private TextView placeholder;
    private ProgressBar progressBar;

    private FirestoreRecyclerAdapter adapter;
    private FirebaseFirestore db;
    private String userID;

    private final String USERS = "users";
    private final String GROUPS = "wgs";
    private final String NAME = "name";
    private final String EMAIL = "email";
    private final String PICPATH = "picPath";
    private final String INVITATIONS = "invitations";
    private final String TIMESTAMP = "timestamp";
}
