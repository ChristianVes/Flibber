package christian.eilers.flibber.Home;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import christian.eilers.flibber.Models.Note;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViews();
        storage = FirebaseStorage.getInstance().getReference();
        userID = LocalStorage.getUserID(getContext());
        groupID = LocalStorage.getGroupID(getContext());
        users = ((HomeActivity) getActivity()).getUsers();
        if(users != null) loadData();
        return mainView;
    }

    private void initializeViews() {
        recView = mainView.findViewById(R.id.recView);
        fab = mainView.findViewById(R.id.fab);
        progressBar = mainView.findViewById(R.id.progressBar);
        placeholder = mainView.findViewById(R.id.placeholder);
        progressBar.setVisibility(View.VISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent_newNote = new Intent(getContext(), NoteCreateActivity.class);
                getActivity().startActivity(intent_newNote);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });
    }

    // Lade Notizen aus der Database und zeige sie in einem Recyclerview an
    private void loadData() {
        // Referenz: Notizen der aktuellen WG
        // nach Erstelldatum soriert
        Query query = FirebaseFirestore.getInstance()
                .collection("wgs")
                .document(groupID)
                .collection("notes")
                .orderBy("timestamp");

        FirestoreRecyclerOptions<Note> options = new FirestoreRecyclerOptions.Builder<Note>()
                .setQuery(query, Note.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Note, HomeFragment.NotesHolder>(options) {

            // Set Placeholder-Visibility and ProgressBar-Visibility
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }

            // Bind data from the database to the UI-Object
            @Override
            public void onBindViewHolder(HomeFragment.NotesHolder holder, int position, Note model) {
                final User user = users.get(model.getUserID());

                if(model.getTitle() == null || model.getTitle().isEmpty())
                    holder.tv_title.setVisibility(View.GONE);
                else {
                    holder.tv_title.setVisibility(View.VISIBLE);
                    holder.tv_title.setText(model.getTitle());
                }

                if(model.getDescription() == null || model.getDescription().isEmpty())
                    holder.tv_description.setVisibility(View.GONE);
                else {
                    holder.tv_description.setVisibility(View.VISIBLE);
                    holder.tv_description.setText(model.getDescription());
                }

                holder.tv_username.setText(user.getName());

                if(model.getTimestamp() != null)
                    holder.tv_datum.setText(
                            DateUtils.getRelativeTimeSpanString(model.getTimestamp().getTime(),
                                    System.currentTimeMillis() + 10000,
                                    DateUtils.MINUTE_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_RELATIVE));


                if(user.getPicPath() != null)
                    GlideApp.with(getContext())
                            .load(storage.child("profile_pictures").child(user.getPicPath()))
                            .dontAnimate()
                            .placeholder(R.drawable.profile_placeholder)
                            .into(holder.img_profile);

                if(model.getImagePath() != null)
                    GlideApp.with(getContext())
                            .load(storage.child("notes").child(model.getImagePath()))
                            .dontAnimate()
                            .into(holder.img_note);
                else {
                    Glide.with(getContext()).clear(holder.img_note);
                }
            }

            // Einmalige Zuweisung zum ViewHolder
            @Override
            public HomeFragment.NotesHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext()).inflate(R.layout.item_notes, group, false);
                return new HomeFragment.NotesHolder(view);
            }
        };

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        adapter.notifyDataSetChanged();
        recView.setLayoutManager(layoutManager);
//        recView.setItemViewCacheSize(20);
//        recView.setDrawingCacheEnabled(true);
        recView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
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

//            itemView.setOnClickListener(this);
        }
    }

    private View mainView;
    private RecyclerView recView;
    private ProgressBar progressBar;
    private TextView placeholder;
    private FirestoreRecyclerAdapter adapter;
    private FloatingActionButton fab;
    private StorageReference storage;

    private String userID, groupID;
    private HashMap<String, User> users;
}
