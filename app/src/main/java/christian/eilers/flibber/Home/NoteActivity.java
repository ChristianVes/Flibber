package christian.eilers.flibber.Home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
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
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

import static christian.eilers.flibber.Utils.Strings.*;

public class NoteActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        initializeVariables();
        if (hasNulls()) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }
        initializeViews();
        loadNote();
        loadData();
    }

    // Initialize views from layout file
    private void initializeViews() {
        bottomLayout = findViewById(R.id.bottomLayout);
        scrollView = findViewById(R.id.scrollView);
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

    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
        thisNote = (Note) getIntent().getSerializableExtra(NOTEID);
        users = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
    }

    // check for null pointers
    private boolean hasNulls() {
        if (thisNote == null || users == null || userID == null || groupID == null) return true;
        else return false;
    }

    private void loadNote() {
        if(thisNote.getUserID().equals(userID)) btn_more.setVisibility(View.VISIBLE); // Option-Button nur für Ersteller sichtbar machen

        // TITEL & BESCHREIBUNG
        if (thisNote.getTitle() != null && !TextUtils.isEmpty(thisNote.getTitle())) {
            tv_title.setVisibility(View.VISIBLE);
            tv_title.setText(thisNote.getTitle());
        }
        if (thisNote.getDescription() != null && !TextUtils.isEmpty(thisNote.getDescription())) {
            tv_description.setVisibility(View.VISIBLE);
            tv_description.setText(thisNote.getDescription());
        }

        // TIMESTAMP (Buffer um "in 0 Minuten"-Anzeige zu vermeiden)
        if(thisNote.getTimestamp() != null)
            tv_datum.setText(
                    DateUtils.getRelativeTimeSpanString(thisNote.getTimestamp().getTime(),
                            System.currentTimeMillis() + BUFFER,
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE));

        // NOTE PICTURE
        if(thisNote.getImagePath() != null && isValidContextForGlide(NoteActivity.this))
            GlideApp.with(NoteActivity.this)
                    .load(storage.child(NOTES).child(groupID).child(thisNote.getImagePath()))
                    .dontAnimate()
                    .into(img_note);

        // USER-INFORMATION
        final User user = users.get(thisNote.getUserID());
        // USERNAME
        tv_username.setText(user.getName());
        // PROFILE PICTURE
        if (user.getPicPath() != null)
            GlideApp.with(NoteActivity.this)
                    .load(storage.child(PROFILE).child(user.getPicPath()))
                    .dontAnimate()
                    .into(img_profile);
    }

    public boolean isValidContextForGlide(final Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            if (activity.isFinishing()) {
                return false;
            }
        }
        return true;
    }

    // Save the comment specified to the note in the database
    private void saveComment() {
        final String commentText = input.getText().toString().trim();
        if(TextUtils.isEmpty(commentText)) return;

        // save Comment in Database
        final Comment comment = new Comment(commentText, userID);
        final DocumentReference ref_note = db.collection(GROUPS).document(groupID).collection(NOTES).document(thisNote.getKey());
        ref_note.collection(COMMENTS).document().set(comment);

        // Update the commentsCount to +1
        db.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot snapshot = transaction.get(ref_note);
                long newCount = snapshot.getLong(COMMENTSCOUNT) + 1;
                transaction.update(ref_note, COMMENTSCOUNT, newCount);
                return null;
            }
        });
        input.setText(""); // Clear Comments-Input Field
        // Hide Keyboard
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    // load and display the Comments
    private void loadData() {
        final Query commentsQuery = db.collection(GROUPS).document(groupID)
                .collection(NOTES).document(thisNote.getKey())
                .collection(COMMENTS).orderBy(TIMESTAMP);

        final FirestoreRecyclerOptions<Comment> options = new FirestoreRecyclerOptions.Builder<Comment>()
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
                final User user = users.get(model.getUserID());
                // USERNAME
                holder.comment_username.setText(user.getName());

                // TEXT
                holder.comment_text.setText(model.getDescription());

                // TIMESTAMP
                if(model.getTimestamp() != null) {
                    holder.comment_datum.setText(
                            DateUtils.getRelativeTimeSpanString(
                                    model.getTimestamp().getTime(),
                                    System.currentTimeMillis() + BUFFER,
                                    DateUtils.MINUTE_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_RELATIVE)
                    );
                    holder.comment_datum.setVisibility(View.VISIBLE);
                } else {
                    holder.comment_datum.setVisibility(View.GONE);
                }

            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
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

    // Change visibility of layout-objects when user likes to modify the note
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

    // Update the modified note
    private void updateNote() {
        final String title = et_title.getText().toString().trim();
        final String description = et_description.getText().toString().trim();
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(description)) return;

        final HashMap<String, Object> map = new HashMap<>();
        map.put(TITLE, title); // new title
        map.put(DESCRIPTION, description); // new description
        map.put(TIMESTAMP, FieldValue.serverTimestamp()); // new Timestamp

        db.collection(GROUPS).document(groupID).collection(NOTES).document(thisNote.getKey()).update(map);
        updateLayout(title, description);
    }

    // Update Layout after changing the Note
    private void updateLayout(String title, String description) {
        et_title.setVisibility(View.GONE);
        et_description.setVisibility(View.GONE);
        btn_save.setVisibility(View.GONE);
        tv_title.setText(title);
        tv_description.setText(description);
        bottomLayout.setVisibility(View.VISIBLE);
        btn_more.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(title)) tv_title.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(description)) tv_description.setVisibility(View.VISIBLE);
    }

    // delete the note and call a cloud function that deletes all comments, then finish the activity
    private void deleteNote() {
        new MaterialDialog.Builder(NoteActivity.this)
                .title("Notiz wirklich löschen?")
                .positiveText("Löschen")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (thisNote.getImagePath() != null) storage.child(NOTES).child(groupID).child(thisNote.getImagePath()).delete();
                        db.collection(GROUPS).document(groupID).collection(NOTES).document(thisNote.getKey()).delete();
                        finish();
                    }
                })
                .negativeText("Abbrechen")
                .show();
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
    private ScrollView scrollView;
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
    private Note thisNote;
    private HashMap<String, User> users;

    private String userID, groupID;
}
