package christian.eilers.flibber.FirestoreAdapter;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import christian.eilers.flibber.Home.TaskActivity;
import christian.eilers.flibber.Home.TaskFragment;
import christian.eilers.flibber.Models.NotificationModel;
import christian.eilers.flibber.Models.TaskEntry;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

import static christian.eilers.flibber.Utils.Strings.ENTRIES;
import static christian.eilers.flibber.Utils.Strings.GROUPS;
import static christian.eilers.flibber.Utils.Strings.INVOLVEDIDS;
import static christian.eilers.flibber.Utils.Strings.NOTIFICATIONS;
import static christian.eilers.flibber.Utils.Strings.ONE_DAY;
import static christian.eilers.flibber.Utils.Strings.TASKID;
import static christian.eilers.flibber.Utils.Strings.TASKS;
import static christian.eilers.flibber.Utils.Strings.TIMESTAMP;
import static christian.eilers.flibber.Utils.Strings.USERS;

public class TasksAdapter extends FirestoreRecyclerAdapter<TaskModel, RecyclerView.ViewHolder>{

    private FirebaseFunctions functions;
    private String userID, groupID;
    private HashMap<String, User> users;
    private TaskFragment fragment;

    private final int HIDE = 0;
    private final int SHOW = 1;

    public TasksAdapter(@NonNull FirestoreRecyclerOptions<TaskModel> options, TaskFragment fragment,
                        HashMap<String, User> users, String userID, String groupID) {
        super(options);
        this.fragment = fragment;
        this.users = users;
        this.userID = userID;
        this.groupID = groupID;
    }

    // Create normal/empty Viewholder depending on whether the user is involved in this task
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        functions = FirebaseFunctions.getInstance();
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

        // DATE
        // Relative time span
        taskHolder.tv_datum.setText(DateUtils.getRelativeTimeSpanString(model.getTimestamp().getTime(),
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE));

        // USER-ORDER Layout
        if (!model.isOrdered()) {
            taskHolder.layout_order.setVisibility(View.GONE);
            taskHolder.tv_placeholder.setVisibility(View.VISIBLE);
        }
        else {
            taskHolder.tv_placeholder.setVisibility(View.GONE);
            taskHolder.layout_order.setVisibility(View.VISIBLE);

            // USER ORDER
            User nextUser = users.get(model.getInvolvedIDs().get(0));
            String[] nextUser_names = nextUser.getName().split(" ", 2);
            taskHolder.tv_order_first.setText(nextUser_names[0]);
            // check if one/multiple user are involved in this task
            if (model.getInvolvedIDs().size() == 1) {
                taskHolder.tv_order_second.setText(nextUser_names[0]);
            }
            else {
                User secUser = users.get(model.getInvolvedIDs().get(1));
                String[] secUser_names = secUser.getName().split(" ", 2);
                taskHolder.tv_order_second.setText(secUser_names[0]);
            }
        }

        // OnClick Listener for navigation to detailed view
        taskHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(taskHolder.itemView.getContext(), TaskActivity.class);
                i.putExtra(TASKID, model);
                i.putExtra(USERS, users);
                taskHolder.itemView.getContext().startActivity(i);
            }
        });

        // DONE Listener
        taskHolder.btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskDone(taskHolder, model);
            }
        });

    }

    // Handle actions to be done when User has finished/taken care of a task
    private void taskDone(TaskHolder taskHolder, TaskModel model) {
        // Change order of the involved user's
        ArrayList<String> newOrder = (ArrayList<String>) model.getInvolvedIDs().clone();
        newOrder.remove(userID);
        newOrder.add(userID);
        // Update Involved-User's and Timestamp
        final HashMap<String, Object> taskMap = new HashMap<>();
        taskMap.put(TIMESTAMP, new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(model.getFrequenz())));
        taskMap.put(INVOLVEDIDS, newOrder);
        // Create a TaskEntry
        final TaskEntry entry = new TaskEntry(userID);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference docTask = db.collection(GROUPS).document(groupID).collection(TASKS).document(model.getKey());
        final DocumentReference docEntry = db.collection(GROUPS).document(groupID).collection(TASKS)
                .document(model.getKey()).collection(ENTRIES).document();

        WriteBatch batch = db.batch();
        batch.update(docTask, taskMap);
        batch.set(docEntry, entry);
        String not_description = "\"" + model.getTitle() + "\" erledigt";
        for (String id : model.getInvolvedIDs()) {
            if (id.equals(userID)) continue;
            DocumentReference doc = db.collection(GROUPS).document(groupID).collection(USERS).document(id).collection(NOTIFICATIONS).document();
            NotificationModel not = new NotificationModel(doc.getId(), not_description, TASKS, userID);
            batch.set(doc, not);
        }
        batch.commit();
        Toast.makeText(taskHolder.itemView.getContext(), model.getTitle() +" erledigt!", Toast.LENGTH_SHORT).show();
    }

    // Custom ViewHolder for a Task
    public class TaskHolder extends RecyclerView.ViewHolder {
        TextView tv_title, tv_datum, tv_order_first, tv_order_second, tv_placeholder;
        LinearLayout btn_done;
        LinearLayout layout_order;

        public TaskHolder(View itemView) {
            super(itemView);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_order_first = itemView.findViewById(R.id.order_first);
            tv_order_second = itemView.findViewById(R.id.order_second);
            tv_title = itemView.findViewById(R.id.taskName);
            tv_placeholder = itemView.findViewById(R.id.placeholder);
            btn_done = itemView.findViewById(R.id.btn_done);
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
