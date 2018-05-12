package christian.eilers.flibber.Home;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;
import static christian.eilers.flibber.Utils.Strings.*;

public class NewTaskActivity extends AppCompatActivity implements View.OnFocusChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_task);
        initializeViews();
        initializeVariables();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        et_name = findViewById(R.id.input_title);
        et_frequency = findViewById(R.id.input_frequenz);
        switch_order = findViewById(R.id.switch_order);
        progressBar = findViewById(R.id.progressBar);
        tv_beteiligte = findViewById(R.id.tv_count);
        layout_beteiligte = findViewById(R.id.layout_beteiligte);

        et_name.setOnFocusChangeListener(this);
        et_frequency.setOnFocusChangeListener(this);

        setSupportActionBar(toolbar); // Toolbar als Actionbar setzen
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Titel der Actionbar ausblenden
    }

    // Initialize variables
    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        users = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
        if (hasNulls()) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        } else {
            final ArrayList<User> userList = new ArrayList<>(users.values());
            selectedIDs = new ArrayList<>();
            layout_beteiligte.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Dialog dialog = new UserSelectionDialog(NewTaskActivity.this,
                            android.R.style.Theme_Translucent_NoTitleBar, userList);
                    dialog.show();
                }
            });
        }
    }

    // check for null pointers
    private boolean hasNulls() {
        if (users == null || userID == null || groupID == null) return true;
        else return false;
    }

    private void saveTask() {
        String title = et_name.getText().toString().trim();
        String s_frequency = et_frequency.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(s_frequency)) {
            Toast.makeText(this, "Eingaben unvollstädnig...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedIDs.isEmpty()) {
            Toast.makeText(this, "Beteiligte auswählen...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!selectedIDs.contains(userID)) {
            Toast.makeText(this, "Du musst beteiligt sein...", Toast.LENGTH_SHORT).show();
            return;
        }

        long frequency = Long.valueOf(s_frequency);
        boolean hasOrder = switch_order.isChecked();

        DocumentReference doc = db.collection(GROUPS).document(groupID).collection(TASKS).document();

        TaskModel task = new TaskModel(doc.getId(), title, frequency, selectedIDs, hasOrder,
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(frequency)));

        progressBar.setVisibility(View.VISIBLE);
        doc.set(task).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                progressBar.setVisibility(View.GONE);
                finish();
            }
        });
    }

    public void setBeteiligte(ArrayList<String> selectedIDs) {
        this.selectedIDs = selectedIDs;
        if (selectedIDs.size() == 1) tv_beteiligte.setText("1 Person");
        else tv_beteiligte.setText(selectedIDs.size() + " Personen");
    }

    public ArrayList<String> getSelectedIDs() {
        return selectedIDs;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (et_frequency.hasFocus() || et_name.hasFocus()) return;
        if (!hasFocus) {
            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_task, menu);
        MenuItem item_save = menu.findItem(R.id.action_save);
        item_save.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });
        return true;
    }

    private Toolbar toolbar;
    private RelativeLayout layout_beteiligte;
    private TextView tv_beteiligte;
    private EditText et_name, et_frequency;
    private SwitchCompat switch_order;
    private ProgressBar progressBar;

    private String userID, groupID;
    private FirebaseFirestore db;
    private HashMap<String, User> users;
    private ArrayList<String> selectedIDs;

}
