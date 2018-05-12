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
import android.widget.Toast;

import java.util.ArrayList;

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.RecyclerAdapter.PayerSelectionAdapter;

public class PayerSelectionDialog extends Dialog {

    public PayerSelectionDialog(@NonNull Context context, int themeResId, ArrayList<User> userList) {
        super(context, themeResId);
        this.context = context;
        getWindow().setBackgroundDrawableResource(R.color.translucent_black);
        this.userList = userList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_userlist);
        btn = findViewById(R.id.btn);
        recView = findViewById(R.id.recView);
        recView.setHasFixedSize(true);
        recView.setLayoutManager(new LinearLayoutManager(getContext()));
        final PayerSelectionAdapter adapter = new PayerSelectionAdapter(userList,
                ((TransactionActivity)context).getBezahler());
        recView.setAdapter(adapter);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof Activity)
                    ((TransactionActivity)context).setBezahler(adapter.getPayerID());
                else
                    Toast.makeText(getContext(), "Fehler aufgetreten", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });

    }

    private Context context;
    private Button btn;
    private RecyclerView recView;
    private ArrayList<User> userList;
}
