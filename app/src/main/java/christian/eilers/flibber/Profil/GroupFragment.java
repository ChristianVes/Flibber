package christian.eilers.flibber.Profil;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.Models.Group;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

import static christian.eilers.flibber.Utils.Strings.*;

public class GroupFragment extends Fragment implements View.OnClickListener{

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_groups, container, false);
        userID = LocalStorage.getUserID(getContext());
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference().child(GROUPS);
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
        Query groupsQuery = db
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
            public void onBindViewHolder(GroupHolder holder, int position, final Group model) {

                holder.v_name.setText(model.getName());
                Glide.with(getContext()).clear(holder.img_group);
                GlideApp.with(getContext())
                        .load(storage.child(model.getKey()))
                        .dontAnimate()
                        .placeholder(R.drawable.placeholder_group)
                        .into(holder.img_group);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HashMap<String, Object> map_devicetoken = new HashMap<>();
                        map_devicetoken.put(DEVICETOKEN, FirebaseInstanceId.getInstance().getToken());
                        LocalStorage.setGroupID(getContext(), model.getKey());
                        db.collection(GROUPS).document(model.getKey()).collection(USERS).document(userID)
                                .update(map_devicetoken);
                        Intent homeIntent = new Intent(getContext(), HomeActivity.class);
                        startActivity(homeIntent);
                        getActivity().finish();
                    }
                });
            }

            // Einmalige Zuweisung zum ViewHolder: GroupHolder
            @Override
            public GroupHolder onCreateViewHolder(ViewGroup group, int i) {
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
        if (adapter != null) adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fab_new) {
            groupCreation();
        }
        else if(id == R.id.fab_invitations) {
            inviteDialog();
        }
    }

    public void groupCreation() {
        MaterialDialog.Builder  builder = new MaterialDialog.Builder(getContext())
                .title("Neue Gruppe erstellen")
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                .input("Gruppenname", null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                    }
                })
                .positiveText("Erstellen")
                .negativeText("Abbrechen")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        String groupname = dialog.getInputEditText().getText().toString().trim();
                        if(TextUtils.isEmpty(groupname)) {
                            Toast.makeText(getContext(), "Keinen Gruppennamen eingegeben", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dialog.getInputEditText().setText("");
                        createGroup(groupname);
                    }
                });

        createDialog = builder.build();
        createDialog.show();
    }

    private void createGroup(final String groupname) {
        final DocumentReference reference = db.collection(GROUPS).document();
        final Group group = new Group(groupname, reference.getId());
        reference.set(group);

        // Add Group-Reference to the current user
        db.collection(USERS).document(userID).collection(GROUPS).document(group.getKey()).set(group);

        // Add user to the Group
        db.collection(USERS).document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()) {
                    Crashlytics.logException(task.getException());
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                final String username = task.getResult().getString(NAME);
                final String email = task.getResult().getString(EMAIL);
                final String picPath = task.getResult().getString(PICPATH);
                final String deviceToken = task.getResult().getString(DEVICETOKEN);
                final User user = new User(username, email, userID, picPath, deviceToken);
                db.collection(GROUPS).document(group.getKey()).collection(USERS).document(userID).set(user);
            }
        });
    }

    private void inviteDialog() {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getContext())
                .title("Einladungen")
                .customView(R.layout.dialog_einladungen, true)
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {// adapter -> stopListening
                    }
                });

        invitesDialog = builder.build();
        invitesDialog.show();

        final View mainView = invitesDialog.getCustomView();
        final RecyclerView list_invites = mainView.findViewById(R.id.recView);
        final TextView placeholder = mainView.findViewById(R.id.placeholder);
        final ProgressBar invitesProgressBar = mainView.findViewById(R.id.progressBar);

        invitesProgressBar.setVisibility(View.VISIBLE);

        Query invitesQuery = db
                .collection(USERS)
                .document(userID)
                .collection(INVITATIONS)
                .orderBy(TIMESTAMP);

        FirestoreRecyclerOptions<Group> options = new FirestoreRecyclerOptions.Builder<Group>()
                .setQuery(invitesQuery, Group.class)
                .build();

        FirestoreRecyclerAdapter invitesAdapter = new FirestoreRecyclerAdapter<Group, InvitationHolder>(options) {

            // Aktualisiere Platzhalter und ProgressBar
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                invitesProgressBar.setVisibility(View.GONE);
            }

            // Bind data from the database to the UI-Object
            @Override
            public void onBindViewHolder(InvitationHolder holder, int position, final Group model) {

                holder.v_name.setText(model.getName());
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        joinWG(model.getKey());
                        invitesDialog.dismiss();
                    }
                });
            }

            // Einmalige Zuweisung zum ViewHolder: GroupHolder
            @Override
            public InvitationHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext()).inflate(R.layout.item_wg_invitations, group, false);
                return new GroupFragment.InvitationHolder(view);
            }
        };

        invitesAdapter.startListening();
        list_invites.setLayoutManager(new LinearLayoutManager(getContext()));
        list_invites.setAdapter(invitesAdapter);
    }

    // Custom ViewHolder for interacting with single items of the RecyclerView
    public class GroupHolder extends RecyclerView.ViewHolder{
        TextView v_name;
        CircleImageView img_group;

        public GroupHolder(View itemView) {
            super(itemView);
            v_name = itemView.findViewById(R.id.wg_name);
            img_group = itemView.findViewById(R.id.img_group);
        }
    }

    // Custom ViewHolder for interacting with single items of the RecyclerView
    public class InvitationHolder extends RecyclerView.ViewHolder{
        TextView v_name;

        public InvitationHolder(View itemView) {
            super(itemView);
            v_name = itemView.findViewById(R.id.wg_name);
        }
    }

    // Join the Group with the given Group ID
    private void joinWG(final String groupID) {
        db.collection(USERS).document(userID).collection(GROUPS).document(groupID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                // Überprüfe ob der User bereits Mitglied in der Gruppe ist
                if (task.getResult().exists()) {
                    Toast.makeText(getContext(), "Already member of this Group", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.collection(GROUPS).document(groupID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        // Überprüfe ob die Gruppe noch existiert
                        if (!document.exists()) {
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
                final String deviceToken = task.getResult().getString(DEVICETOKEN);
                final User user = new User(username, email, userID, picPath, deviceToken);
                db.collection(GROUPS).document(groupID).collection(USERS).document(userID).set(user);
                deleteInvitation(groupID);
            }
        });
    }

    // Delete Invitation Documents
    private void deleteInvitation(final String groupID) {
        db.collection(USERS).document(userID).collection(INVITATIONS).document(groupID).delete();
        db.collection(GROUPS).document(groupID).collection(INVITATIONS).document(userID).delete();
    }


    private View mainView;
    private RecyclerView recView;
    private TextView placeholder;
    private FloatingActionButton fab_new, fab_invitations;
    private ProgressBar progressBar;
    private MaterialDialog invitesDialog;
    private MaterialDialog createDialog;

    private FirestoreRecyclerAdapter adapter;
    private FirebaseFirestore db;
    private StorageReference storage;
    private String userID;
}
