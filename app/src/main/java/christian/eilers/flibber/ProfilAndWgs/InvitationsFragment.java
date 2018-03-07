package christian.eilers.flibber.ProfilAndWgs;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.Models.Group;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class InvitationsFragment extends DialogFragment {

    // Instanzierung des Dialogs
    public static InvitationsFragment newInstance() {
        if(thisDialog == null) thisDialog = new InvitationsFragment();
        return thisDialog;
    }

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

    // Lade Liste an WG's des eingeloggten Users aus der Database
    private void loadData() {
        // Referenz: WG's des aktuellen Users
        // nach Einzugsdatum soriert
        Query query = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userID)
                .collection("invitations")
                .orderBy("timestamp");

        FirestoreRecyclerOptions<Group> options = new FirestoreRecyclerOptions.Builder<Group>()
                .setQuery(query, Group.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Group, InvitationsFragment.WgHolder>(options) {

            // Set Placeholder-Visibility and ProgressBar-Visibility
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onBindViewHolder(InvitationsFragment.WgHolder holder, int position, Group model) {
                holder.v_name.setText(model.getName());
                holder.wg = model;
            }

            // Einmalige Zuweisung zum ViewHolder
            @Override
            public InvitationsFragment.WgHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext()).inflate(R.layout.item_wg, group, false);
                return new InvitationsFragment.WgHolder(view);
            }
        };

        adapter.startListening();
        recView.setLayoutManager(new LinearLayoutManager(getContext()));
        recView.setAdapter(adapter);
    }

    // Custom ViewHolder for interacting with single items of the RecyclerView
    public class WgHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView v_name;
        public Group wg;

        public WgHolder(View itemView) {
            super(itemView);
            v_name = itemView.findViewById(R.id.wg_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            joinWG(wg.getKey());
        }
    }

    // Join WG with this specific key (if it exists)
    private void joinWG(final String wgKey) {
        if (TextUtils.isEmpty(wgKey)) {
            Toast.makeText(getContext(), "Keinen Key eingegeben", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(userID).collection("wgs").document(wgKey).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                // Überprüfe ob User bereits Mitglied in der WG ist
                if (task.getResult().exists()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Already member of this WG", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.collection("wgs").document(wgKey).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        // Überprüfe ob WG mit eingegebenem Key existiert
                        if (!document.exists()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "WG does not exist", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // WG zu dem User hinzufügen
                        Group wg = document.toObject(Group.class);
                        db.collection("users").document(userID).collection("wgs").document(wg.getKey()).set(wg);
                        // User zur WG hinzufügen
                        addUserToWg(wgKey);
                    }
                });
            }
        });
    }

    // Add current User to the WG
    private void addUserToWg(final String wgKey) {
        db.collection("users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                String username = task.getResult().getString("name");
                String email = task.getResult().getString("email");
                String picPath = task.getResult().getString("picPath");
                User user = new User(username, email, userID, picPath, 0.0);
                db.collection("wgs").document(wgKey).collection("users").document(userID).set(user);
                deleteInvitation(wgKey);
            }
        });
    }

    // Delete Invitation Documents at Users-Collection and WG-Collection
    private void deleteInvitation(final String wgkey) {
        db.collection("users").document(userID).collection("invitations").document(wgkey).delete();
        db.collection("wgs").document(wgkey).collection("invitations").document(userID).delete();
        Toast.makeText(getContext(), "Erfolgreich beigetreten", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(adapter != null) adapter.stopListening();
    }

    private static InvitationsFragment thisDialog;

    private View mainView;
    private RecyclerView recView;
    private TextView placeholder;
    private ProgressBar progressBar;

    private FirestoreRecyclerAdapter adapter;
    private FirebaseFirestore db;
    private String userID;
}
