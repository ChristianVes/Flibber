package christian.eilers.flibber.FirestoreAdapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import christian.eilers.flibber.Home.HomeActivity;
import christian.eilers.flibber.Models.Group;
import christian.eilers.flibber.ProfileActivity;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

import static christian.eilers.flibber.Utils.Strings.DEVICETOKEN;
import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class GroupAdapter extends FirestoreRecyclerAdapter<Group, GroupAdapter.GroupHolder> {

    private Context context;
    private ProfileActivity activity;
    private StorageReference storage;
    private String userID;

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public GroupAdapter(@NonNull FirestoreRecyclerOptions<Group> options, ProfileActivity activity) {
        super(options);
        this.activity = activity;
        storage = FirebaseStorage.getInstance().getReference().child(GROUPS);
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    // Bind data from the database to the UI-Object
    @Override
    public void onBindViewHolder(final GroupHolder holder, int position, final Group model) {
        // Set group name
        holder.v_name.setText(model.getName());
        // Set group image
        Glide.with(holder.itemView.getContext()).clear(holder.img_group);
        if (model.getPicPath() != null)
            GlideApp.with(holder.itemView.getContext())
                    .load(storage.child(model.getPicPath()))
                    .dontAnimate()
                    .placeholder(R.drawable.placeholder_group)
                    .into(holder.img_group);
        // Open group on click
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save group data in local storage
                LocalStorage.setGroupID(holder.itemView.getContext(), model.getKey());
                LocalStorage.setGroupName(holder.itemView.getContext(), model.getName());
                LocalStorage.setGroupPicPath(holder.itemView.getContext(), model.getPicPath());
                // update device token stored in group reference
                HashMap<String, Object> map_devicetoken = new HashMap<>();
                map_devicetoken.put(DEVICETOKEN, FirebaseInstanceId.getInstance().getToken());
                FirebaseFirestore.getInstance().collection(GROUPS).document(model.getKey())
                        .collection(USERS).document(userID).update(map_devicetoken);
                // start @HomeActivity
                Intent homeIntent = new Intent(context, HomeActivity.class);
                context.startActivity(homeIntent);
                activity.finish();
            }
        });
    }

    // Einmalige Zuweisung zum ViewHolder: GroupHolder
    @Override
    public GroupHolder onCreateViewHolder(ViewGroup group, int i) {
        context = group.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_wg, group, false);
        return new GroupHolder(view);
    }

    // Custom ViewHolder for a Transaction
    public class GroupHolder extends RecyclerView.ViewHolder {
        TextView v_name;
        CircleImageView img_group;

        public GroupHolder(View itemView) {
            super(itemView);
            v_name = itemView.findViewById(R.id.wg_name);
            img_group = itemView.findViewById(R.id.img_group);
        }
    }

}
