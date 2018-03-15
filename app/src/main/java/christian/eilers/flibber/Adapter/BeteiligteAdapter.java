package christian.eilers.flibber.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
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

public class BeteiligteAdapter extends RecyclerView.Adapter<BeteiligteAdapter.ViewHolder> {

    private final String PROFILE = "profile";
    private ArrayList<User> users;
    private ArrayList<String> selectedUserKeys;
    private Context context;
    private StorageReference storage = FirebaseStorage.getInstance().getReference().child(PROFILE);

    public BeteiligteAdapter(ArrayList<User> users) {
        this.users = users;
        selectedUserKeys = new ArrayList<>();
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
        holder.tv_username.setText(user.getName());
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

    public ArrayList<String> getBeteiligteIDs() {
        return selectedUserKeys;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView img_profile;
        TextView tv_username;

        public ViewHolder(final View itemView) {
            super(itemView);
            img_profile = itemView.findViewById(R.id.profile_image);
            tv_username = itemView.findViewById(R.id.username);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String userID = users.get(getAdapterPosition()).getUserID();
                    CardView cardView = itemView.findViewById(R.id.card);
                    if (selectedUserKeys.contains(userID)) {
                        cardView.setCardBackgroundColor(context.getResources().getColor(R.color.cardview_light_background));
                        selectedUserKeys.remove(userID);
                    }
                    else {
                        cardView.setCardBackgroundColor(context.getResources().getColor(R.color.colorAccent));
                        selectedUserKeys.add(userID);
                    }
                }
            });
        }
    }
}
