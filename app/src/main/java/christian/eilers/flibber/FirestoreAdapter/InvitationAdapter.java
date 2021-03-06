package christian.eilers.flibber.FirestoreAdapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.Models.Group;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.ProfileActivity;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

import static christian.eilers.flibber.Utils.Strings.DEVICETOKEN;
import static christian.eilers.flibber.Utils.Strings.EMAIL;
import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.INVITATIONS;
import static christian.eilers.flibber.Utils.Strings.NAME;
import static christian.eilers.flibber.Utils.Strings.PICPATH;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class InvitationAdapter extends FirestoreRecyclerAdapter<Group, InvitationAdapter.InvitationHolder> {

    private Context context;
    private ProfileActivity activity;
    private FirebaseFirestore db;
    private String userID;

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public InvitationAdapter(@NonNull FirestoreRecyclerOptions<Group> options, ProfileActivity activity) {
        super(options);
        this.activity = activity;
        db = FirebaseFirestore.getInstance();
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    // Bind data from the database to the UI-Object
    @Override
    public void onBindViewHolder(final InvitationHolder holder, int position, final Group model) {
        holder.v_name.setText(model.getName());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinWG(model.getKey(), holder.itemView.getContext());
            }
        });
    }

    // Join the Group with the given Group ID
    public void joinWG(final String groupID, final Context context) {
        db.collection(USERS).document(userID).collection(GROUPS).document(groupID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                // Überprüfe ob der User bereits Mitglied in der Gruppe ist
                if (task.getResult().exists()) {
                    Toast.makeText(context, "Already member of this Group", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.collection(GROUPS).document(groupID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        // Überprüfe ob die Gruppe noch existiert
                        if (!document.exists()) {
                            Toast.makeText(context, "Group does not exist", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Add Group-Reference to the user
                        final Group group = document.toObject(Group.class);
                        db.collection(USERS).document(userID).collection(GROUPS).document(group.getKey()).set(group)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // User zur Gruppe hinzufügen
                                    addUserToGroup(group, context);
                                }
                            });
                    }
                });
            }
        });
    }

    // Add current User to the Group
    public void addUserToGroup(final Group group, final Context context) {
        db.collection(USERS).document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()) {
                    Crashlytics.logException(task.getException());
                    return;
                }
                final String username = task.getResult().getString(NAME);
                final String email = task.getResult().getString(EMAIL);
                final String picPath = task.getResult().getString(PICPATH);
                final String deviceToken = task.getResult().getString(DEVICETOKEN);
                final User user = new User(username, email, userID, picPath, deviceToken);
                WriteBatch batch = db.batch();
                DocumentReference ref_user = db.collection(GROUPS).document(group.getKey()).collection(USERS).document(userID);
                DocumentReference ref_user_inv = db.collection(USERS).document(userID).collection(INVITATIONS).document(group.getKey());
                DocumentReference ref_group_inv = db.collection(GROUPS).document(group.getKey()).collection(INVITATIONS).document(userID);
                batch.set(ref_user, user);
                batch.delete(ref_user_inv);
                batch.delete(ref_group_inv);
                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        HashMap<String, Object> map_devicetoken = new HashMap<>();
                        map_devicetoken.put(DEVICETOKEN, deviceToken);
                        LocalStorage.setGroupID(context, group.getKey());
                        LocalStorage.setGroupName(context, group.getName());
                        FirebaseFirestore.getInstance().collection(GROUPS).document(group.getKey())
                                .collection(USERS).document(userID).update(map_devicetoken);
                        // TODO: Hier auf eine Anleitungsseite weiterleiten (gruppen Key bereits im local Storage)
                        Intent homeIntent = new Intent(context, HomeActivity.class);
                        context.startActivity(homeIntent);
                        activity.finish();
                    }
                });
            }
        });
    }

    // Einmalige Zuweisung zum ViewHolder: GroupHolder
    @Override
    public InvitationHolder onCreateViewHolder(ViewGroup group, int i) {
        context = group.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_wg, group, false);
        return new InvitationHolder(view);
    }

    // Custom ViewHolder for Invitations
    public class InvitationHolder extends RecyclerView.ViewHolder {
        TextView v_name;

        public InvitationHolder(View itemView) {
            super(itemView);
            v_name = itemView.findViewById(R.id.wg_name);
        }
    }
}
