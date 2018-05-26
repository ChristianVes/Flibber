package christian.eilers.flibber.FirestoreAdapter;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import christian.eilers.flibber.Home.NoteActivity;
import christian.eilers.flibber.Models.Note;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import de.hdodenhof.circleimageview.CircleImageView;

import static christian.eilers.flibber.Utils.Strings.BUFFER;
import static christian.eilers.flibber.Utils.Strings.NOTEID;
import static christian.eilers.flibber.Utils.Strings.NOTES;
import static christian.eilers.flibber.Utils.Strings.PROFILE;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class NotesHomeAdapter extends FirestoreRecyclerAdapter<Note, NotesHomeAdapter.NotesHolder> {

    HashMap<String, User> users;
    String groupID;
    StorageReference storage;

    public NotesHomeAdapter(@NonNull FirestoreRecyclerOptions<Note> options, HashMap<String, User> users, String groupID) {
        super(options);
        this.users = users;
        this.groupID = groupID;
        storage = FirebaseStorage.getInstance().getReference();
    }

    @Override
    protected void onBindViewHolder(@NonNull final NotesHolder holder, int position, @NonNull final Note model) {
        User user = users.get(model.getUserID());
        // USERNAME
        holder.tv_username.setText(user.getName());

        // TITEL
        if(model.getTitle() == null || model.getTitle().isEmpty())
            holder.tv_title.setVisibility(View.GONE);
        else {
            holder.tv_title.setVisibility(View.VISIBLE);
            holder.tv_title.setText(model.getTitle());
        }

        // BESCHREIBUNG
        if(model.getDescription() == null || model.getDescription().isEmpty())
            holder.tv_description.setVisibility(View.GONE);
        else {
            holder.tv_description.setVisibility(View.VISIBLE);
            holder.tv_description.setText(model.getDescription());
        }

        // TIMESTAMP (Buffer um "in 0 Minuten"-Anzeige zu vermeiden)
        if(model.getTimestamp() != null)
            holder.tv_datum.setText(
                    DateUtils.getRelativeTimeSpanString(model.getTimestamp().getTime(),
                            System.currentTimeMillis() + BUFFER,
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE));


        // PROFILE PICTURE
        if(user.getPicPath() != null)
            GlideApp.with(holder.itemView.getContext())
                    .load(storage.child(PROFILE).child(user.getPicPath()))
                    .dontAnimate()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(holder.img_profile);

        // NOTE PICTURE ("Clear" zum vermeiden falscher Zuweisungen)
        if(model.getImagePath() != null)
            GlideApp.with(holder.itemView.getContext())
                    .load(storage.child(NOTES).child(groupID).child(model.getImagePath()))
                    .centerInside()
                    .dontAnimate()
                    .into(holder.img_note);
        else Glide.with(holder.itemView.getContext()).clear(holder.img_note);


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(holder.itemView.getContext(), NoteActivity.class);
                i.putExtra(NOTEID, model);
                i.putExtra(USERS, users);
                holder.itemView.getContext().startActivity(i);
            }
        });
    }

    @NonNull
    @Override
    public NotesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notes2, parent, false);
        return new NotesHolder(view);
    }

    // Custom ViewHolder for interacting with single items of the RecyclerView
    public class NotesHolder extends RecyclerView.ViewHolder {
        View itemView;
        CircleImageView img_profile;
        TextView tv_username, tv_title, tv_description, tv_datum;
        ImageView img_note;

        public NotesHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            img_profile = itemView.findViewById(R.id.profile_image);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_description = itemView.findViewById(R.id.description);
            tv_title = itemView.findViewById(R.id.title);
            tv_username = itemView.findViewById(R.id.username);
            img_note = itemView.findViewById(R.id.image);
        }
    }
}
