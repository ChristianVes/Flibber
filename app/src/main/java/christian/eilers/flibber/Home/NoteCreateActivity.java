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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import christian.eilers.flibber.Models.Note;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class NoteCreateActivity extends AppCompatActivity implements TextView.OnEditorActionListener, View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_create);
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        storage = FirebaseStorage.getInstance().getReference().child("notes");
        db = FirebaseFirestore.getInstance();
        initializeViews();
    }

    // Initialize views from layout file
    private void initializeViews() {
        toolbar = findViewById(R.id.note_toolbar);
        img_notiz = findViewById(R.id.image);
        et_description = findViewById(R.id.input_description);
        et_title = findViewById(R.id.input_title);
        fab = findViewById(R.id.fab);
        fab_delete = findViewById(R.id.fab_delete);
        progressBar = findViewById(R.id.progressBar);

        et_description.setOnEditorActionListener(this);
        et_title.setOnEditorActionListener(this);

        fab.setOnClickListener(this);
        fab_delete.setOnClickListener(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        attachKeyboardListener();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fab) {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
        }
        else if (id == R.id.fab_delete) {
            img_notiz.setImageDrawable(null);
            imageUri = null;
            fab_delete.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send:
                saveNote();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Gallery Intent
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Zuschneiden des Bildes auf 1:1 Ratio
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_GALLERY) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(img_notiz);
            fab_delete.setVisibility(View.VISIBLE);
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

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
            // TODO: Random String Generator
            final String notePicPath = imageUri.getLastPathSegment();
            storage.child(notePicPath).putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                     createDbEntry(notePicPath);
                }
            });
        }
    }

    private void createDbEntry(String notePicPath) {
        Note note = new Note(
                et_title.getText().toString().trim(),
                et_description.getText().toString().trim(),
                userID,
                notePicPath
        );
        db.collection("wgs").document(groupID).collection("notes").document().set(note);
        toHomeActivity();
        progressBar.setVisibility(View.GONE);
    }

    private void toHomeActivity() {
        Intent intent_back = new Intent(NoteCreateActivity.this, HomeActivity.class);
        startActivity(intent_back);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void attachKeyboardListener() {
        // Hide Keyboards on Click outside
        et_title.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) hideKeyboard(v);
            }
        });
        et_description.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) hideKeyboard(v);
            }
        });
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private Toolbar toolbar;
    private ImageView img_notiz;
    private EditText et_title, et_description;
    private FloatingActionButton fab, fab_delete;
    private ProgressBar progressBar;

    private StorageReference storage;
    private FirebaseFirestore db;
    private String userID, groupID;
    private Uri imageUri;

    private static final int REQUEST_CODE_GALLERY = 0;
}
