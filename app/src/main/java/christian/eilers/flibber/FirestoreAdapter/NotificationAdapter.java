package christian.eilers.flibber.FirestoreAdapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import christian.eilers.flibber.Models.NotificationModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import de.hdodenhof.circleimageview.CircleImageView;

import static christian.eilers.flibber.Utils.Strings.BUFFER;
import static christian.eilers.flibber.Utils.Strings.PROFILE;

public class NotificationAdapter extends FirestoreRecyclerAdapter<NotificationModel, NotificationAdapter.NotificationHolder> {

    HashMap<String, User> users;
    StorageReference storage;

    public NotificationAdapter(@NonNull FirestoreRecyclerOptions<NotificationModel> options, HashMap<String, User> users) {
        super(options);
        this.users = users;
        storage = FirebaseStorage.getInstance().getReference();
    }

    @Override
    protected void onBindViewHolder(@NonNull NotificationHolder holder, int position, @NonNull NotificationModel model) {
        User user = users.get(model.getUserID());
        // USERNAME
        holder.tv_username.setText(user.getName());

        // PROFILE PICTURE
        if(user.getPicPath() != null)
            GlideApp.with(holder.itemView.getContext())
                    .load(storage.child(PROFILE).child(user.getPicPath()))
                    .dontAnimate()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(holder.img_profile);

        // TIMESTAMP (Buffer um "in 0 Minuten"-Anzeige zu vermeiden)
        if(model.getTimestamp() != null)
            holder.tv_date.setText(
                    DateUtils.getRelativeTimeSpanString(model.getTimestamp().getTime(),
                            System.currentTimeMillis() + BUFFER,
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE));

        // DECRIPTION
        holder.tv_description.setText(model.getDescription());
    }

    @NonNull
    @Override
    public NotificationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationHolder(view);
    }

    // Custom ViewHolder for a Task
    public class NotificationHolder extends RecyclerView.ViewHolder {
        CircleImageView img_profile;
        TextView tv_username, tv_description, tv_date;

        public NotificationHolder(View itemView) {
            super(itemView);
            img_profile = itemView.findViewById(R.id.profile_image);
            tv_username = itemView.findViewById(R.id.username);
            tv_description = itemView.findViewById(R.id.text);
            tv_date = itemView.findViewById(R.id.datum);
        }
    }
}
