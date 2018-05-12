package christian.eilers.flibber.Home;

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

public class UserSelectionDialog extends Dialog {

    public UserSelectionDialog(@NonNull Context context, int themeResId, ArrayList<User> userList, NewTaskActivity activity) {
        super(context, themeResId);
        this.activity = activity;
        this.userList = userList;
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

        final UserSelectionAdapter adapter = new UserSelectionAdapter(userList,
                activity.getSelectedIDs());
        recView.setAdapter(adapter);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.setBeteiligte(adapter.getInvolvedIDs());
                dismiss();
            }
        });

    }

    private NewTaskActivity activity;
    private Button btn;
    private RecyclerView recView;
    private ArrayList<User> userList;
}
