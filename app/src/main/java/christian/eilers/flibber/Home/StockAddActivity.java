package christian.eilers.flibber.Home;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blackcat.currencyedittext.CurrencyEditText;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.StockProduct;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.RecyclerAdapter.UserSelectionAdapter;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.*;

public class StockAddActivity extends AppCompatActivity implements View.OnFocusChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_add);
        initializeViews();
        initializeVariables();
    }

    // Initialize views
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        et_name = findViewById(R.id.input_name);
        et_price = findViewById(R.id.input_price);
        layout_involved = findViewById(R.id.involved);
        tv_count = findViewById(R.id.tv_count);
        progressBar = findViewById(R.id.progressBar);

        et_name.requestFocus();
        et_price.setLocale(Locale.GERMANY);
        et_price.configureViewForLocale(Locale.GERMANY);

        et_name.setOnFocusChangeListener(this);
        et_price.setOnFocusChangeListener(this);

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
            selectedIDs = new ArrayList<>();
            layout_involved.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // open dialog to selected involved users on click
                    Dialog dialog = new UserSelectionDialog(StockAddActivity.this,
                            android.R.style.Theme_Translucent_NoTitleBar);
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

    // save the StockProduct to the database and finish the activity
    private void saveProduct() {
        String name = et_name.getText().toString().trim();
        long price = et_price.getRawValue();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Produktnamen eingeben...", Toast.LENGTH_SHORT).show();
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

        DocumentReference doc = db.collection(GROUPS).document(groupID).collection(STOCK).document();

        StockProduct product = new StockProduct(doc.getId(),
                name,
                price,
                new ArrayList<String>(),
                selectedIDs);

        progressBar.setVisibility(View.VISIBLE);
        doc.set(product).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                progressBar.setVisibility(View.GONE);
                finish();
            }
        });
    }

    // update the selected user's list and it's text view
    public void setInvolved(ArrayList<String> selectedIDs) {
        this.selectedIDs = selectedIDs;
        if (selectedIDs.size() == 1) tv_count.setText("1 Person");
        else tv_count.setText(selectedIDs.size() + " Personen");
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        // TODO: et_price.hasFocus() wieder einbauen und im xml enablen
        if (et_name.hasFocus()) return;
        if (!hasFocus) {
            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stock_add, menu);
        MenuItem item_save = menu.findItem(R.id.action_save);
        item_save.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProduct();
            }
        });
        return true;
    }

    // Dialog to choose the involved users
    private class UserSelectionDialog extends Dialog {

        private Button btn;
        private RecyclerView recView;

        public UserSelectionDialog(@NonNull Context context, int themeResId) {
            super(context, themeResId);
            // set the background to a translucent black
            getWindow().setBackgroundDrawableResource(R.color.translucent_black);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_userlist);
            btn = findViewById(R.id.btn);
            recView = findViewById(R.id.recView);

            recView.setHasFixedSize(true);
            recView.setLayoutManager(new LinearLayoutManager(getContext()));

            final UserSelectionAdapter adapter = new UserSelectionAdapter(
                    new ArrayList<>(users.values()),
                    selectedIDs);
            recView.setAdapter(adapter);

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setInvolved(adapter.getInvolvedIDs());
                    dismiss();
                }
            });

        }
    }


    private Toolbar toolbar;
    private EditText et_name;
    private CurrencyEditText et_price;
    private RelativeLayout layout_involved;
    private TextView tv_count;
    private ProgressBar progressBar;

    private String userID, groupID;
    private FirebaseFirestore db;
    private HashMap<String, User> users;
    private ArrayList<String> selectedIDs;
}
