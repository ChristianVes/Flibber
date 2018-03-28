package christian.eilers.flibber.Adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.util.HashMap;

import christian.eilers.flibber.Models.Payment;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;

public class TasksAdapter extends FirestoreRecyclerAdapter<TaskModel, TasksAdapter.TaskHolder>{

    private final int BUFFER = 10000; // Millisekunden // entspricht 10 Sekunden
    private String userID;
    private HashMap<String, User> users;

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public TasksAdapter(@NonNull FirestoreRecyclerOptions<TaskModel> options, String userID, HashMap<String, User> users) {
        super(options);
        this.userID = userID;
        this.users = users;
    }

    // Create normal/empty Viewholder depending on whether the user is involved in this transaction
    @NonNull
    @Override
    public TaskHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull final TaskHolder holder, int position, @NonNull final TaskModel model) {

        holder.tv_title.setText(model.getTitle());
    }

    // Custom ViewHolder for a Transaction
    public class TaskHolder extends RecyclerView.ViewHolder {
        TextView tv_title, tv_datum, tv_order;
        Button btn_done, btn_pass, btn_remind;

        public TaskHolder(View itemView) {
            super(itemView);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_order = itemView.findViewById(R.id.order);
            tv_title = itemView.findViewById(R.id.taskName);
            btn_done = itemView.findViewById(R.id.btn_done);
            btn_pass = itemView.findViewById(R.id.btn_pass);
            btn_remind = itemView.findViewById(R.id.btn_remind);
        }
    }
}
