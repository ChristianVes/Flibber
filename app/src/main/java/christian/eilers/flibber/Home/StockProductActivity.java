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
        rec_involved = findViewById(R.id.recView_involved);
        rec_verlauf = findViewById(R.id.recVerlauf);

        rec_involved.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rec_involved.setHasFixedSize(true);
    }

    private void initializeVariables() {
        userID = LocalStorage.getUserID(this);
        groupID = LocalStorage.getGroupID(this);
        db = FirebaseFirestore.getInstance();
        thisProduct = (StockProduct) getIntent().getSerializableExtra(STOCKID);
        users = (HashMap<String, User>) getIntent().getSerializableExtra(USERS);
    }

    // check for null pointers
    private boolean hasNulls() {
        if (thisProduct == null || users == null || userID == null || groupID == null) return true;
        else return false;
    }

    // load the StockProduct from database
    private void loadData() {
        // PRICE
        // tv_price.setAmount(thisProduct.getPrice());
        // NAME as toolbar title
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(thisProduct.getName());

        // Involved users
        ArrayList<User> userList = new ArrayList<>();
        for (String key : thisProduct.getInvolvedIDs()) userList.add(users.get(key));

        TaskInvolvedAdapter adapter = new TaskInvolvedAdapter(userList);
        rec_involved.setAdapter(adapter);
    }

    // delete this StockProduct and finish the activity
    private void deleteStockProduct() {
        for (String id : thisProduct.getInvolvedIDs())
            db.collection(GROUPS).document(groupID).collection(USERS).document(id).collection(STOCK).document(thisProduct.getKey()).delete();
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

    private String userID, groupID;
    private StockProduct thisProduct;
    private FirebaseFirestore db;
    private HashMap<String, User> users;
}
