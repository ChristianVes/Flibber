package christian.eilers.flibber.Home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Note;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;
import static christian.eilers.flibber.Utils.Strings.*;

public class NoteCreateActivity extends AppCompatActivity implements TextView.OnEditorActionListener, View.OnClickListener, View.OnFocusChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_create);
        initializeViews();
        initializeVariables();
    }

    // Initialize views from layout file
    private void initializeViews() {
        toolbar = findViewById(R.id.note_toolbar);
        img_notiz = findViewById(R.id.image);
        et_description = findViewById(R.id.input_description);
        et_title = findViewById(R.id.input_title);
        fab = findViewById(R.id.fab);
        btn_delete = findViewById(R.id.btn_delete);
        progressBar = findViewById(R.id.progressBar);

        et_description.setOnEditorActionListener(this);
        et_title.setOnEditorActionListener(this);

        et_description.setOnFocusChangeListener(this);
        et_title.setOnFocusChangeListener(this);

        fab.setOnClickListener(this);
        btn_delete.setOnClickListener(this);

        setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Titel der Actionbar ausblenden
    }

    // Initialize variables
    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        if (userID == null || groupID == null) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }
        storage = FirebaseStorage.getInstance().getReference().child(NOTES).child(groupID);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fab) {
            // Intent zur Gallerie
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
        }
        else if (id == R.id.btn_delete) {
            // Notiz-Image entfernen
            img_notiz.setImageDrawable(null);
            imageUri = null;
            btn_delete.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_create, menu);
        MenuItem item_save = menu.findItem(R.id.action_save);
        item_save.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });
        return true;
    }

    // Gewähltes Bild aus der Gallerie in ImageView laden
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_GALLERY) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(img_notiz);
            btn_delete.setVisibility(View.VISIBLE);
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    /*
    Bei drücken des Action-Buttons auf dem Keyboard im Titel-Textfeld wird das
    Beschreibung-Textfeld fokussiert und das Keyboard für dieses Feld geöffnet
     */
    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == EditorInfo.IME_ACTION_NEXT) {
            if (et_title.hasFocus()) {
                et_description.requestFocus();
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(et_description, InputMethodManager.SHOW_IMPLICIT);
                return true;
            }
            return false;
        }
        return false;
    }

    // Verberge Tastatur, wenn gegebene Views ihren Fokus verlieren
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (et_description.hasFocus() || et_title.hasFocus()) return;
        if (!hasFocus) {
            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // Speichere Notiz in Datenbank (und ggf. Bild im Storage) ab
    private void saveNote() {
        String title = et_title.getText().toString().trim();
        String description = et_description.getText().toString().trim();
        if(title.isEmpty() && description.isEmpty()) {
            Toast.makeText(this, "Titel oder Beschreibung eingeben...", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        if(imageUri == null) {
            createDbEntry(null);
        }
        else {
            final String notePicPath = UUID.randomUUID().toString().replaceAll("-","");
            storage.child(notePicPath).putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                     createDbEntry(notePicPath);
                }
            });
        }
    }

    // Erstelle Datenbank-Eintrag für die Notiz
    private void createDbEntry(final String notePicPath) {
        db.collection(GROUPS).document(groupID).collection(USERS).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                ArrayList<String> userList = new ArrayList<>();
                for(DocumentSnapshot doc : queryDocumentSnapshots) {
                    User user = doc.toObject(User.class);
                    userList.add(user.getUserID());
                }
                final DocumentReference noteRef = db.collection(GROUPS).document(groupID).collection(NOTES).document();
                final Note note = new Note(
                        et_title.getText().toString().trim(),
                        et_description.getText().toString().trim(),
                        userID,
                        notePicPath,
                        noteRef.getId(),
                        userList
                );
                noteRef.set(note).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        toNoteActivity(note);
                    }
                });
            }
        });

    }

    // Wechsel zur Home Activity
    private void toNoteActivity(final Note note) {
        // Lade zuerst aktuelle Userliste (für Intent-Extra)
        db.collection(GROUPS).document(groupID).collection(USERS).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                HashMap<String, User> users = new HashMap<>();
                for(DocumentSnapshot doc : queryDocumentSnapshots) {
                    User user = doc.toObject(User.class);
                    users.put(user.getUserID(), user);
                }
                progressBar.setVisibility(View.GONE);
                Intent showNote = new Intent(NoteCreateActivity.this, NoteActivity.class);
                showNote.putExtra(NOTEID, note);
                showNote.putExtra(USERS, users);
                startActivity(showNote);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });

    }


    private Toolbar toolbar;
    private ImageView img_notiz;
    private EditText et_title, et_description;
    private FloatingActionButton fab;
    private ImageButton btn_delete;
    private ProgressBar progressBar;

    private StorageReference storage;
    private FirebaseFirestore db;
    private String userID, groupID;
    private Uri imageUri;

    private final int REQUEST_CODE_GALLERY = 0;


}
