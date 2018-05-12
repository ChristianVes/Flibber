package christian.eilers.flibber.RecyclerAdapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import de.hdodenhof.circleimageview.CircleImageView;

import static christian.eilers.flibber.Utils.Strings.*;

public class TaskInvolvedAdapter extends RecyclerView.Adapter<TaskInvolvedAdapter.ViewHolder> {

    private ArrayList<User> users;  // List containing all users
    private Context context;
    private StorageReference storage = FirebaseStorage.getInstance().getReference().child(PROFILE);

    public TaskInvolvedAdapter(ArrayList<User> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction_user, parent, false);
        context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final User user = users.get(position);
        // USERNAME
        String[] names = user.getName().split(" ", 2);
        holder.tv_username.setText(names[0]);
        // User's PROFILE PICTURE
        if (user.getPicPath() != null)
            GlideApp.with(context)
                    .load(storage.child(user.getPicPath()))
                    .placeholder(R.drawable.profile_placeholder)
                    .dontAnimate()
                    .into(holder.img_profile);
        else Glide.with(context).clear(holder.img_profile);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView img_profile;
        TextView tv_username;

        public ViewHolder(final View itemView) {
            super(itemView);
            img_profile = itemView.findViewById(R.id.profile_image);
            tv_username = itemView.findViewById(R.id.username);

        }
    }
}
