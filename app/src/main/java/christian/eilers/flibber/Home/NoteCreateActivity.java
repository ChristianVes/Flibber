package christian.eilers.flibber.Home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import christian.eilers.flibber.Models.Note;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;

public class NoteCreateActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_create);
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        userName = LocalStorage.getUsername(this);
        picPath = LocalStorage.getPicPath(this);
        storage = FirebaseStorage.getInstance().getReference().child("profile_pictures");
        db = FirebaseFirestore.getInstance();
        initializeViews();
    }

    // Initialize views from layout file
    private void initializeViews() {
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_save = findViewById(R.id.btn_save);
        img_profile = findViewById(R.id.profile_image);
        tv_datum = findViewById(R.id.datum);
        tv_username = findViewById(R.id.username);
        img_notiz = findViewById(R.id.image);
        et_description = findViewById(R.id.input_description);
        et_title = findViewById(R.id.input_title);
        btn_cancel.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        tv_username.setText(userName);
        if (picPath != null)
            GlideApp.with(NoteCreateActivity.this)
                    .load(storage.child(picPath))
                    .dontAnimate()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(img_profile);
        tv_datum.setVisibility(View.GONE);
        img_notiz.setVisibility(View.GONE);

        attachKeyboardListener();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_cancel) {
            toHomeActivity();
        }
        else if (id == R.id.btn_save) {
            saveNote();
        }
    }

    private void saveNote() {
        String title = et_title.getText().toString().trim();
        String description = et_description.getText().toString().trim();
        if(title.isEmpty() && description.isEmpty()) {
            Toast.makeText(this, "Titel oder Beschreibung eingeben...", Toast.LENGTH_SHORT).show();
            return;
        }

        Note note = new Note(
                et_title.getText().toString().trim(),
                et_description.getText().toString().trim(),
                userID,
                null
        );
        db.collection("wgs").document(groupID).collection("notes").document().set(note);
        toHomeActivity();
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

    private Button btn_cancel, btn_save;
    private CircleImageView img_profile;
    private TextView tv_username, tv_datum;
    private ImageView img_notiz;
    private EditText et_title, et_description;

    private StorageReference storage;
    private FirebaseFirestore db;
    private String userID, groupID, userName, picPath;

}
