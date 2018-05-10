package christian.eilers.flibber.RecyclerAdapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import de.hdodenhof.circleimageview.CircleImageView;

import static christian.eilers.flibber.Utils.Strings.PROFILE;

public class UserSelectionAdapter extends RecyclerView.Adapter<UserSelectionAdapter.ViewHolder> {

    private ArrayList<User> users;
    private ArrayList<String> involvedIDs;
    private Context context;
    private StorageReference storage = FirebaseStorage.getInstance().getReference().child(PROFILE);

    public UserSelectionAdapter(ArrayList<User> users, ArrayList<String> selectedIDs) {
        this.users = users;
        involvedIDs = selectedIDs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final User user = users.get(position);
        // USERNAME
        holder.tv_username.setText(user.getName());
        // User's PROFILE PICTURE
        if (user.getPicPath() != null)
            GlideApp.with(context)
                    .load(storage.child(user.getPicPath()))
                    .placeholder(R.drawable.profile_placeholder)
                    .dontAnimate()
                    .into(holder.img_profile);
        else Glide.with(context).clear(holder.img_profile);
        // Show/Hide Ring around picture depending if selected
        if (involvedIDs.contains(user.getUserID())) {
            int padding = holder.frameLayout.getPaddingTop(); // Benutzte ein beliebiges
            holder.frameLayout.setBackgroundResource(R.drawable.layerlist_circle);
            holder.frameLayout.setPadding(padding,padding,padding,padding);
        }
        else {
            int padding = holder.frameLayout.getPaddingTop(); // Benutzte ein beliebiges
            holder.frameLayout.setBackgroundColor(Color.TRANSPARENT);
            holder.frameLayout.setPadding(padding,padding,padding,padding);
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public ArrayList<String> getInvolvedIDs() {
        return involvedIDs;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView img_profile;
        TextView tv_username;
        FrameLayout frameLayout;

        public ViewHolder(final View itemView) {
            super(itemView);
            img_profile = itemView.findViewById(R.id.profile_image);
            tv_username = itemView.findViewById(R.id.username);
            frameLayout = itemView.findViewById(R.id.frame_layout);

            // Add/Remove clicked User to the involved ID's
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String userID = users.get(getAdapterPosition()).getUserID();
                    if (involvedIDs.contains(userID)) {
                        int padding = frameLayout.getPaddingTop(); // Benutzte ein beliebiges
                        frameLayout.setBackgroundColor(Color.TRANSPARENT);
                        frameLayout.setPadding(padding,padding,padding,padding);
                        involvedIDs.remove(userID);
                    }
                    else {
                        int padding = frameLayout.getPaddingTop(); // Benutzte ein beliebiges
                        frameLayout.setBackgroundResource(R.drawable.layerlist_circle);
                        frameLayout.setPadding(padding,padding,padding,padding);
                        involvedIDs.add(userID);
                    }
                }
            });
        }
    }
}
