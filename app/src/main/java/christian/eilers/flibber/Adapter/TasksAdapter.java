package christian.eilers.flibber.Adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;

public class TasksAdapter extends FirestoreRecyclerAdapter<TaskModel, RecyclerView.ViewHolder>{

    private String userID, groupID;
    private HashMap<String, User> users;
    private final int HIDE = 0;
    private final int SHOW = 1;
    private final String TIMESTAMP = "timestamp";
    private final String INVOLVEDIDS = "involvedIDs";
    private final String GROUPS = "groups";
    private final String USERS = "users";
    private final String TASKS = "tasks";

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public TasksAdapter(@NonNull FirestoreRecyclerOptions<TaskModel> options,
                        String userID, String groupID, HashMap<String, User> users) {
        super(options);
        this.userID = userID;
        this.groupID = groupID;
        this.users = users;
    }

    // Create normal/empty Viewholder depending on whether the user is involved in this task
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case SHOW: {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
                return new TaskHolder(view);
            }
            default: {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empty, parent, false);
                return new EmptyHolder(view);
            }
        }
    }

    // Determine whether the user is involved in the task at the given adapter-position
    @Override
    public int getItemViewType(int position) {
        boolean isInvolved = getSnapshots().get(position).getInvolvedIDs().contains(userID);
        if (isInvolved) return SHOW;
        return HIDE;
    }

    @Override
    protected void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position, @NonNull final TaskModel model) {
        if (holder.getItemViewType() == HIDE) return; // show nothing if user is not involved

        // specialize the ViewHolder as TransactionHolder to bind the data to the item
        final TaskHolder taskHolder = (TaskHolder) holder;
        // TITLE
        taskHolder.tv_title.setText(model.getTitle());

        // DATUM
        taskHolder.tv_datum.setText(DateUtils.getRelativeTimeSpanString(model.getTimestamp().getTime(),
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE));

        // USER-ORDER
        User nextUser = users.get(model.getInvolvedIDs().get(0));
        taskHolder.tv_order_first.setText(nextUser.getName());
        if (model.getInvolvedIDs().size() == 1) { // case: only one User involved
            taskHolder.tv_order_second.setText(nextUser.getName());
        } else { // case: multiple User involved -> also display the after-next User
            User secUser = users.get(model.getInvolvedIDs().get(1));
            taskHolder.tv_order_second.setText(secUser.getName());
        }

        // Hide User-Order when Task is defined as not-ordered
        if (!model.isOrdered()) taskHolder.layout_order.setVisibility(View.GONE);
        else taskHolder.layout_order.setVisibility(View.VISIBLE);
        // Pass-Button Visibility
        if (nextUser.getUserID().equals(userID)) {
            taskHolder.btn_pass.setVisibility(View.VISIBLE);
            // PASS-Listener
            taskHolder.btn_pass.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ArrayList<String> newOrder = (ArrayList<String>) model.getInvolvedIDs().clone();
                    newOrder.remove(userID);
                    newOrder.add(1, userID);

                    HashMap<String, Object> taskMap = new HashMap<>();
                    taskMap.put(INVOLVEDIDS, newOrder);

                    FirebaseFirestore.getInstance().collection(GROUPS).document(groupID).collection(TASKS)
                            .document(getSnapshots().getSnapshot(position).getId())
                            .update(taskMap);
                }
            });
        }
        else taskHolder.btn_pass.setVisibility(View.GONE);

        // DONE-Listener
        taskHolder.btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> newOrder = (ArrayList<String>) model.getInvolvedIDs().clone();
                newOrder.remove(userID);
                newOrder.add(userID);

                HashMap<String, Object> taskMap = new HashMap<>();
                taskMap.put(TIMESTAMP,
                        new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(model.getFrequenz())));
                taskMap.put(INVOLVEDIDS, newOrder);

                FirebaseFirestore.getInstance().collection(GROUPS).document(groupID).collection(TASKS)
                        .document(getSnapshots().getSnapshot(position).getId())
                        .update(taskMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(taskHolder.itemView.getContext(), model.getTitle() +" erledigt!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // Custom ViewHolder for a Task
    public class TaskHolder extends RecyclerView.ViewHolder {
        TextView tv_title, tv_datum, tv_order_first, tv_order_second;
        ImageButton btn_done, btn_pass, btn_remind;
        LinearLayout layout_order;

        public TaskHolder(View itemView) {
            super(itemView);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_order_first = itemView.findViewById(R.id.order_first);
            tv_order_second = itemView.findViewById(R.id.order_second);
            tv_title = itemView.findViewById(R.id.taskName);
            btn_done = itemView.findViewById(R.id.btn_done);
            btn_pass = itemView.findViewById(R.id.btn_pass);
            btn_remind = itemView.findViewById(R.id.btn_remind);
            layout_order = itemView.findViewById(R.id.layout_order);
        }
    }

    // Just an Empty Viewholder (for tasks the current user is not involved in)
    public class EmptyHolder extends RecyclerView.ViewHolder {

        public EmptyHolder(View itemView) {
            super(itemView);
        }
    }
}
