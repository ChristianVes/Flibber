package christian.eilers.flibber.Home.Finance;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.RecyclerAdapter.UserSelectionAdapter;

public class InvolvedSelectionDialog extends Dialog {

    public InvolvedSelectionDialog(@NonNull Context context, int themeResId, ArrayList<User> userList) {
        super(context, themeResId);
        this.context = context;
        this.userList = userList;
        // set the background to a translucent black
        getWindow().setBackgroundDrawableResource(R.color.translucent_black);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_userlist);
        btn_save = findViewById(R.id.btn_save);
        btn_all = findViewById(R.id.btn_all);
        btn_all.setVisibility(View.VISIBLE);
        recView = findViewById(R.id.recView);

        recView.setHasFixedSize(true);
        recView.setLayoutManager(new LinearLayoutManager(getContext()));

        final UserSelectionAdapter adapter = new UserSelectionAdapter(
                userList,
                ((TransactionActivity)context).getSelectedIDs());
        recView.setAdapter(adapter);

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((TransactionActivity)context).setInvolved(adapter.getInvolvedIDs());
                dismiss();
            }
        });

        btn_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.selectAll(recView);
            }
        });

    }

    private Context context;
    private Button btn_save, btn_all;
    private RecyclerView recView;
    private ArrayList<User> userList;
}
