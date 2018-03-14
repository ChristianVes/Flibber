package christian.eilers.flibber.Home;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Note;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViews();
        initializeVariables();
        if(users != null) loadData();
        else {
            Intent main = new Intent(getContext(), MainActivity.class);
            startActivity(main);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
        }
        return mainView;
    }

    // Initialize views from layout file
    private void initializeViews() {
        recView = mainView.findViewById(R.id.recView);
        refreshLayout = mainView.findViewById(R.id.refreshLayout);
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
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    // Initialize variables
    private void initializeVariables() {
        storage = FirebaseStorage.getInstance().getReference();
        groupID = LocalStorage.getGroupID(getContext());
        users = ((HomeActivity) getActivity()).getUsers();
    }

    // Lädt Notizen aus der Datenbank in den RecyclerView und hält sie up-to-date über einen Listener
    private void loadData() {
        Query notesQuery = FirebaseFirestore.getInstance()
                .collection(GROUPS)
                .document(groupID)
                .collection(NOTES)
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Note> options = new FirestoreRecyclerOptions.Builder<Note>()
                .setQuery(notesQuery, Note.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Note, HomeFragment.NotesHolder>(options) {

            // Aktualisiere Platzhalter und ProgressBar
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                refreshLayout.setRefreshing(false);
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }

            // Bind data from the database to the UI-Object
            @Override
            public void onBindViewHolder(final HomeFragment.NotesHolder holder, int position, final Note model) {
                User user = users.get(model.getUserID());
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
                    GlideApp.with(getContext())
                            .load(storage.child(PROFILE).child(user.getPicPath()))
                            .dontAnimate()
                            .placeholder(R.drawable.profile_placeholder)
                            .into(holder.img_profile);

                // NOTE PICTURE ("Clear" zum vermeiden falscher Zuweisungen)
                if(model.getImagePath() != null)
                    GlideApp.with(getContext())
                            .load(storage.child(NOTES).child(model.getImagePath()))
                            .dontAnimate()
                            .into(holder.img_note);
                else {
                    Glide.with(getContext()).clear(holder.img_note);
                }

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent i = new Intent(getContext(), NoteActivity.class);
                        i.putExtra(NOTEID, model.getKey());
                        startActivity(i);
                        getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    }
                });


                holder.tv_comments.setText("...");
                getSnapshots().getSnapshot(position).getReference().collection(COMMENTS).get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot documentSnapshots) {
                                holder.tv_comments.setText(documentSnapshots.size()+"");
                            }
                        });
            }

            // Einmalige Zuweisung zum ViewHolder: NotesHolder
            @Override
            public HomeFragment.NotesHolder onCreateViewHolder(ViewGroup group, int i) {
                View view = LayoutInflater.from(group.getContext()).inflate(R.layout.item_notes, group, false);
                return new HomeFragment.NotesHolder(view);
            }
        };

        recView.setLayoutManager(new LinearLayoutManager(getContext()));
        recView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(adapter != null) adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(adapter != null) adapter.stopListening();
    }

    // Custom ViewHolder for interacting with single items of the RecyclerView
    public class NotesHolder extends RecyclerView.ViewHolder {
        View itemView;
        CircleImageView img_profile;
        TextView tv_username, tv_title, tv_description, tv_datum, tv_comments;
        ImageView img_note;

        public NotesHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            img_profile = itemView.findViewById(R.id.profile_image);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_description = itemView.findViewById(R.id.description);
            tv_title = itemView.findViewById(R.id.title);
            tv_username = itemView.findViewById(R.id.username);
            tv_comments = itemView.findViewById(R.id.comment_count);
            img_note = itemView.findViewById(R.id.image);
        }
    }

    private View mainView;
    private RecyclerView recView;
    private SwipeRefreshLayout refreshLayout;
    private ProgressBar progressBar;
    private TextView placeholder;
    private FirestoreRecyclerAdapter adapter;
    private FloatingActionButton fab;
    private StorageReference storage;

    private String groupID;
    private HashMap<String, User> users;

    private final String GROUPS = "groups";
    private final String NOTES = "notes";
    private final String TIMESTAMP = "timestamp";
    private final String PROFILE = "profile";
    private final String NOTEID = "noteID";
    private final String COMMENTS = "comments";
    private final int BUFFER = 10000; // Millisekunden // entspricht 10 Sekunden
}
