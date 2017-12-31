package christian.eilers.flibber;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

public class WgSelectorActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wg_selector);
        initializeViews();
        auth = FirebaseAuth.getInstance();
    }

    // Initialize views from layout file
    private void initializeViews() {
        btn_back = findViewById(R.id.btn_exit);
        btn_new = findViewById(R.id.btn_new);
        btn_join = findViewById(R.id.btn_join);
        recView = findViewById(R.id.recView);
        placeholder = findViewById(R.id.placeholder);

        btn_back.setOnClickListener(this);
        btn_new.setOnClickListener(this);
        btn_join.setOnClickListener(this);
    }

    // Check which Button has been clicked
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_exit) {
            onBackPressed();
        } else if (id == R.id.btn_join) {
            joinWG();
        } else if (id == R.id.btn_new) {
            createWG();
        }
    }

    // Open Dialog to join an existing WG
    private void joinWG() {
    }

    // Open Dialog to create a new WG
    private void createWG() {
    }

    @Override
    public void onBackPressed() {
        auth.signOut();
        Intent i = new Intent(WgSelectorActivity.this, LoginActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private ImageButton btn_back;
    private RecyclerView recView;
    private TextView placeholder;
    private Button btn_new, btn_join;
    private FirebaseAuth auth;

}
