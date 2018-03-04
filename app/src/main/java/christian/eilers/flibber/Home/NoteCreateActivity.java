package christian.eilers.flibber.Home;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.Utils;
import de.hdodenhof.circleimageview.CircleImageView;

public class NoteCreateActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_create);
        Utils.getLocalData(this);
        storage = FirebaseStorage.getInstance().getReference().child("profile_pictures");
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

        if (Utils.getPICPATH() != null)
            GlideApp.with(NoteCreateActivity.this)
                    .load(storage.child(Utils.getPICPATH()))
                    .dontAnimate()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(img_profile);
        tv_username.setText(Utils.getUSERNAME());
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_cancel) {
            Intent intent_back = new Intent(NoteCreateActivity.this, HomeActivity.class);
            startActivity(intent_back);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
        else if (id == R.id.btn_save) {
            saveNote();
        }
    }

    private void saveNote() {
    }

    Button btn_cancel, btn_save;
    CircleImageView img_profile;
    TextView tv_username, tv_datum;
    ImageView img_notiz;
    EditText et_title, et_description;

    StorageReference storage;


}
