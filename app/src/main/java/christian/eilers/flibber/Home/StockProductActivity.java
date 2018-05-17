package christian.eilers.flibber.Home;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.ArrayList;
import java.util.HashMap;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.StockProduct;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.RecyclerAdapter.TaskInvolvedAdapter;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.*;

public class StockProductActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_product);
        initializeViews();
        initializeVariables();
        if (hasNulls()) {
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        } else loadData();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tv_price = findViewById(R.id.money);
        label_verlauf = findViewById(R.id.label_verlauf);
        rec_involved = findViewById(R.id.recView_beteiligte);
        rec_verlauf = findViewById(R.id.recVerlauf);
        progressBar = findViewById(R.id.progressBar);

        rec_verlauf.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        stockID = getIntent().getStringExtra(STOCKID);
        users = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
    }

    // check for null pointers
    private boolean hasNulls() {
        if (stockID == null || users == null || userID == null || groupID == null) return true;
        else return false;
    }

    // load the StockProduct from database
    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection(GROUPS).document(groupID).collection(STOCK).document(stockID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                progressBar.setVisibility(View.GONE);
                thisProduct = documentSnapshot.toObject(StockProduct.class);
                // PRICE
                tv_price.setAmount(thisProduct.getPrice());
                // NAME as toolbar title
                setSupportActionBar(toolbar);
                getSupportActionBar().setTitle(thisProduct.getName());

                HashMap<String, User> map_involved = new HashMap<>();
                for (String key : users.keySet()) {
                    if (thisProduct.getInvolvedIDs().contains(key))
                        map_involved.put(key, users.get(key));
                }
                int spanCount = 4;
                rec_involved.setHasFixedSize(true);
                rec_involved.setLayoutManager(new GridLayoutManager(StockProductActivity.this, spanCount));

                TaskInvolvedAdapter adapter_involved = new TaskInvolvedAdapter(new ArrayList<>(map_involved.values()));
                rec_involved.setAdapter(adapter_involved);
            }
        });
    }

    // delete this StockProduct and finish the activity
    private void deleteStockProduct() {
        db.collection(GROUPS).document(groupID).collection(STOCK).document(stockID).delete();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stock_product, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                deleteStockProduct();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Toolbar toolbar;
    private TextView label_verlauf;
    private MoneyTextView tv_price;
    private RecyclerView rec_involved, rec_verlauf;
    private ProgressBar progressBar;

    private String userID, groupID, stockID;
    private StockProduct thisProduct;
    private FirebaseFirestore db;
    private HashMap<String, User> users;
}
