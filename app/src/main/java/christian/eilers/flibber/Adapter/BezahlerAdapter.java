package christian.eilers.flibber.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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

import static christian.eilers.flibber.Utils.Strings.*;

/*
PADDING FIX zum Nachschlagen:
https://stackoverflow.com/questions/5890379/setbackgroundresource-discards-my-xml-layout-attributes?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
 */

public class BezahlerAdapter extends RecyclerView.Adapter<BezahlerAdapter.ViewHolder> {

    private ArrayList<User> users;
    private int selectedPosition;
    private FrameLayout selectedLayout;
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
                .inflate(R.layout.item_transaction_user, parent, false);
        context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final User user = users.get(position);
        // USERNAME
        String[] names = user.getName().split(" ", 2);
        holder.tv_username.setText(names[0]);
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
            selectedLayout = holder.frameLayout;
            int padding = selectedLayout.getPaddingTop(); // Benutzte ein beliebiges
            selectedLayout.setBackgroundResource(R.drawable.layerlist_circle);
            selectedLayout.setPadding(padding,padding,padding,padding);
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

    public String getBezahlerID() {
        return users.get(selectedPosition).getUserID();
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

            // Change selected User (PAYER) on itemClick
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int padding = selectedLayout.getPaddingTop(); // Benutzte ein beliebiges
                    selectedLayout.setBackgroundColor(Color.TRANSPARENT);
                    selectedLayout.setPadding(padding,padding,padding,padding);
                    selectedPosition = getAdapterPosition();
                    selectedLayout = frameLayout;
                    selectedLayout.setBackgroundResource(R.drawable.layerlist_circle);
                    selectedLayout.setPadding(padding,padding,padding,padding);
                }
            });
        }
    }
}
