package christian.eilers.flibber.Home.Finance;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.blackcat.currencyedittext.CurrencyEditText;

import java.util.ArrayList;

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.RecyclerAdapter.UserSelectionAdapter;

public class QuickTransactionDialog extends Dialog {

    public QuickTransactionDialog(@NonNull Context context, int themeResId, ArrayList<User> userList) {
        super(context, themeResId);
        this.context = context;
        getWindow().setBackgroundDrawableResource(R.color.translucent_black);
        this.userList = userList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_quick_transaction);

        tv_title = findViewById(R.id.tv_title);
        et_price = findViewById(R.id.input_price);
        et_description = findViewById(R.id.input_description);
        btn_save = findViewById(R.id.btn_save);
        btn_cancel = findViewById(R.id.btn_cancel);

    }

    private Context context;
    private ArrayList<User> userList;

    private TextView tv_title;
    private CurrencyEditText et_price;
    private EditText et_description;
    private Button btn_save, btn_cancel;
}
