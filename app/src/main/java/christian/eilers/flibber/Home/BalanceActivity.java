package christian.eilers.flibber.Home;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.HashMap;

import christian.eilers.flibber.Models.Balancing;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;

public class BalanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance);
    }

    private Toolbar toolbar;
    private RecyclerView recView;
    private MoneyTextView tv_money;

    private String userID, groupID, balanceID;
    private FirebaseFirestore db;
    private Balancing thisBalancing;
    private HashMap<String, User> users;
}
