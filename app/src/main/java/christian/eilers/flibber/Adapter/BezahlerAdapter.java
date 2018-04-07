package christian.eilers.flibber.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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

public class BezahlerAdapter extends RecyclerView.Adapter<BezahlerAdapter.ViewHolder> {

    private ArrayList<User> users;
    private int selectedPosition;
    private CheckBox selectedCheckbox;
    private String userID;
    private Context context;
    private StorageReference storage = FirebaseStorage.getInstance().getReference().child(PROFILE);

    public BezahlerAdapter(ArrayList<User> users, String userID) {
        this.users = users;
        this.userID = userID;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_checkbox2, parent, false);
        context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final User user = users.get(position);
        // USERNAME
        holder.tv_username.setText(user.getName());
        // User's Profile Picture
        if (user.getPicPath() != null)
            GlideApp.with(context)
                    .load(storage.child(user.getPicPath()))
                    .placeholder(R.drawable.profile_placeholder)
                    .dontAnimate()
                    .into(holder.img_profile);
        else Glide.with(context).clear(holder.img_profile);
        // Select/Mark the current User as the payer
        if (userID.equals(user.getUserID())) {
            selectedPosition = position;
            selectedCheckbox = holder.itemView.findViewById(R.id.checkbox);
            selectedCheckbox.setChecked(true);
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public String getBezahlerID() {
        return users.get(selectedPosition).getUserID();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView img_profile;
        TextView tv_username;
        CheckBox checkBox;

        public ViewHolder(final View itemView) {
            super(itemView);
            img_profile = itemView.findViewById(R.id.profile_image);
            tv_username = itemView.findViewById(R.id.username);
            checkBox = itemView.findViewById(R.id.checkbox);

            // Change selected User (PAYER) on itemClick
            // TODO: Change to RadioBox
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedCheckbox.setChecked(false);
                    selectedPosition = getAdapterPosition();
                    selectedCheckbox = itemView.findViewById(R.id.checkbox);
                    selectedCheckbox.setChecked(true);
                }
            });
        }
    }
}
