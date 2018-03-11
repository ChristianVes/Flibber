package christian.eilers.flibber.Home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Comment;
import christian.eilers.flibber.Models.Note;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

/*
Detail Ansicht einer Notiz und ihrer Kommentare
Möglichkeit zum Kommentieren der Notiz
HINWEIS: Um Keyboard auszublenden das Layout eines scrollbaren Layouts focusable/clickable machen
         Hier: MainLayout, Layout unter NestedScrollView, List Item des Recycler Views
 */
public class NoteActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        noteID = getIntent().getExtras().getString(NOTEID);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
        initializeViews();
        if(noteID == null) {
            Intent main = new Intent(this, MainActivity.class);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
        loadNote();
        loadData();
    }

    // Initialize views from layout file
    private void initializeViews() {
        bottomLayout = findViewById(R.id.bottomLayout);
        commentsView = findViewById(R.id.comments);
        img_note = findViewById(R.id.img_note);
        img_profile = findViewById(R.id.profile_image);
        tv_title = findViewById(R.id.title);
        tv_description = findViewById(R.id.description);
        tv_username = findViewById(R.id.username);
        tv_datum = findViewById(R.id.datum);
        input = findViewById(R.id.input_comment);
        btn_send = findViewById(R.id.btn_send);
        btn_more = findViewById(R.id.btn_more);
        et_description = findViewById(R.id.edit_description);
        et_title = findViewById(R.id.edit_title);
        btn_save = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progressBar);

        input.setOnFocusChangeListener(this);
        et_title.setOnFocusChangeListener(this);
        et_description.setOnFocusChangeListener(this);

        btn_send.setOnClickListener(this);
        btn_more.setOnClickListener(this);
        btn_save.setOnClickListener(this);
    }

    // Lade den Inhalt der Notiz und zeige sie an
    private void loadNote() {
        db.collection(GROUPS).document(groupID).collection(NOTES).document(noteID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Note note = documentSnapshot.toObject(Note.class);
                if(note.getUserID().equals(userID)) btn_more.setVisibility(View.VISIBLE);

                // TITEL & BESCHREIBUNG
                if (note.getTitle() != null && !TextUtils.isEmpty(note.getTitle())) {
                    tv_title.setVisibility(View.VISIBLE);
                    tv_title.setText(note.getTitle());
                }
                if (note.getDescription() != null && !TextUtils.isEmpty(note.getDescription())) {
                    tv_description.setVisibility(View.VISIBLE);
                    tv_description.setText(note.getDescription());
                }

                // TIMESTAMP (Buffer um "in 0 Minuten"-Anzeige zu vermeiden)
                if(note.getTimestamp() != null)
                    tv_datum.setText(
                            DateUtils.getRelativeTimeSpanString(note.getTimestamp().getTime(),
                                    System.currentTimeMillis() + BUFFER,
                                    DateUtils.MINUTE_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_RELATIVE));

                // NOTE PICTURE
                if(note.getImagePath() != null)
                    GlideApp.with(NoteActivity.this)
                            .load(storage.child(NOTES).child(note.getImagePath()))
                            .dontAnimate()
                            .into(img_note);
                else Glide.with(NoteActivity.this).clear(img_note);

                // USER-INFORMATION
                loadNoteUser(note.getUserID());
            }
        });
    }

    // Lade User-Informationen des Notes-Erstellers
    private void loadNoteUser(String noteUserID) {
        db.collection(USERS).document(noteUserID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = documentSnapshot.toObject(User.class);
                // USERNAME
                tv_username.setText(user.getName());
                // PROFILE PICTURE
                if (user.getPicPath() != null)
                    GlideApp.with(NoteActivity.this)
                    .load(storage.child(PROFILE).child(user.getPicPath()))
                    .dontAnimate()
                    .into(img_profile);
                else Glide.with(NoteActivity.this).clear(img_profile);

                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // Save the comment specified to the note in the database
    private void saveComment() {
        String commentText = input.getText().toString().trim();
        if(TextUtils.isEmpty(commentText)) return;
        Comment comment = new Comment(commentText, userID);
        db.collection(GROUPS).document(groupID).collection(NOTES).document(noteID).collection(COMMENTS)
                .document().set(comment);
        input.setText("");
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    // load and display the Comments
    /*
    TODO: Beim Erstellen eines Kommentars wird onBindViewHolder 2mal aufgerufen:
    Einmal bevor es zur Datenbank geschickt wird und einmal wenn es online geupdatet wurde
    */
    private void loadData() {
        final Query commentsQuery = FirebaseFirestore.getInstance()
                .collection(GROUPS)
                .document(groupID)
                .collection(NOTES)
                .document(noteID)
                .collection(COMMENTS)
                .orderBy(TIMESTAMP);

        FirestoreRecyclerOptions<Comment> options = new FirestoreRecyclerOptions.Builder<Comment>()
                .setQuery(commentsQuery, Comment.class)
                .build();

        adapter_comments = new FirestoreRecyclerAdapter<Comment, CommentsHolder>(options) {

            @NonNull
            @Override
            public CommentsHolder onCreateViewHolder(@NonNull ViewGroup group, int viewType) {
                View view = LayoutInflater.from(group.getContext()).inflate(R.layout.item_comment, group, false);
                return new CommentsHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final CommentsHolder holder, int position, @NonNull final Comment model) {
                holder.itemView.setVisibility(View.GONE);

                db.collection(USERS).document(model.getUserID()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        final User user = documentSnapshot.toObject(User.class);
                        holder.comment_username.setText(user.getName());

                        // Comment User's Profile Picture
                        if(user.getPicPath() != null)
                            GlideApp.with(NoteActivity.this)
                                    .load(storage.child(PROFILE).child(user.getPicPath()))
                                    .dontAnimate()
                                    .into(holder.comment_img);
                        else Glide.with(NoteActivity.this).clear(holder.comment_img);

                        // Comment text
                        holder.comment_text.setText(model.getDescription());

                        // Comment Timestamp
                        if(model.getTimestamp() != null)
                            holder.comment_datum.setText(
                                    DateUtils.getRelativeTimeSpanString(
                                            model.getTimestamp().getTime(),
                                            System.currentTimeMillis() + BUFFER,
                                            DateUtils.MINUTE_IN_MILLIS,
                                            DateUtils.FORMAT_ABBREV_RELATIVE)
                            );

                        // Überprüft ob es sich um einen noch nicht an die Database gesendeten Comment handelt
                        boolean offline = getSnapshots().getSnapshot(holder.getAdapterPosition()).getMetadata().hasPendingWrites();
                        if(!offline) holder.itemView.setVisibility(View.VISIBLE);
                    }
                });


            }
        };

        commentsView.setLayoutManager(new LinearLayoutManager(NoteActivity.this));
        commentsView.setAdapter(adapter_comments);
    }

    // Custom Viewholder for the Comments of that Note
    public class CommentsHolder extends RecyclerView.ViewHolder {

        View itemView;
        CircleImageView comment_img;
        TextView comment_username, comment_datum, comment_text;

        public CommentsHolder(final View itemView) {
            super(itemView);
            this.itemView = itemView;
            comment_img = itemView.findViewById(R.id.profile_image);
            comment_datum = itemView.findViewById(R.id.datum);
            comment_text = itemView.findViewById(R.id.text);
            comment_username = itemView.findViewById(R.id.username);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_send) {
            saveComment();
        }
        else if (id == R.id.btn_more) {
            PopupMenu menu = new PopupMenu(NoteActivity.this, btn_more);
            menu.getMenuInflater().inflate(R.menu.menu_note, menu.getMenu());
            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    int itemID = item.getItemId();
                    if (itemID == R.id.action_change) {
                        changeLayout();
                    }
                    else if (itemID == R.id.action_delete) {
                        deleteNote();
                    }
                    return true;
                }
            });
            menu.show();
        }
        else if (id == R.id.btn_save) {
            updateNote();
        }
    }

    private void changeLayout() {
        bottomLayout.setVisibility(View.GONE);
        btn_more.setVisibility(View.GONE);
        tv_title.setVisibility(View.GONE);
        tv_description.setVisibility(View.GONE);
        et_title.setText(tv_title.getText().toString());
        et_description.setText(tv_description.getText().toString());
        et_title.setVisibility(View.VISIBLE);
        et_description.setVisibility(View.VISIBLE);
        btn_save.setVisibility(View.VISIBLE);
    }

    private void updateNote() {
        final String title = et_title.getText().toString().trim();
        final String description = et_description.getText().toString().trim();
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(description)) return;
        HashMap<String, Object> map = new HashMap<>();
        map.put(TITLE, title);
        map.put(DESCRIPTION, description);
        // map.put(TIMESTAMP, FieldValue.serverTimestamp());
        progressBar.setVisibility(View.VISIBLE);
        db.collection(GROUPS).document(groupID).collection(NOTES).document(noteID).update(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updateLayout(title, description);
                    }
                });
    }

    private void updateLayout(String title, String description) {
        et_title.setVisibility(View.GONE);
        et_description.setVisibility(View.GONE);
        btn_save.setVisibility(View.GONE);
        tv_title.setText(title);
        tv_description.setText(description);
        bottomLayout.setVisibility(View.VISIBLE);
        btn_more.setVisibility(View.VISIBLE);
        tv_title.setVisibility(View.VISIBLE);
        tv_description.setVisibility(View.VISIBLE);

        progressBar.setVisibility(View.GONE);
    }

    // TODO: Use cloud function (Node.js/Python) to delete document with sub-collection
    // https://firebase.google.com/docs/firestore/manage-data/delete-data?authuser=0
    private void deleteNote() {
        Toast.makeText(this, "Noch nicht möglich...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus) {
            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter_comments != null) adapter_comments.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter_comments != null) adapter_comments.stopListening();
    }

    private RelativeLayout bottomLayout;
    private RecyclerView commentsView;
    private TextView tv_title, tv_description, tv_username, tv_datum;
    private EditText et_title, et_description;
    private ImageView img_note;
    private CircleImageView img_profile;
    private EditText input;
    private ImageButton btn_send, btn_more;
    private Button btn_save;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private StorageReference storage;
    private FirestoreRecyclerAdapter adapter_comments;

    private String noteID, userID, groupID;

    private final String NOTEID = "noteID";
    private final String NOTES = "notes";
    private final String USERS = "users";
    private final String GROUPS = "groups";
    private final String PROFILE = "profile";
    private final String COMMENTS = "comments";
    private final String TIMESTAMP = "timestamp";
    private final String TITLE = "title";
    private final String DESCRIPTION = "description";
    private final int BUFFER = 10000; // Millisekunden // entspricht 10 Sekunden


}
