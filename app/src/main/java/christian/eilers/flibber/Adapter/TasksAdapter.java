package christian.eilers.flibber.Adapter;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import christian.eilers.flibber.Home.TaskActivity;
import christian.eilers.flibber.Models.TaskEntry;
import christian.eilers.flibber.Models.TaskModel;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;

public class TasksAdapter extends FirestoreRecyclerAdapter<TaskModel, RecyclerView.ViewHolder>{

    private FirebaseFunctions functions;
    private String userID, groupID;
    private HashMap<String, User> users;
    private final int HIDE = 0;
    private final int SHOW = 1;
    private final String TIMESTAMP = "timestamp";
    private final String INVOLVEDIDS = "involvedIDs";
    private final String GROUPS = "groups";
    private final String USERS = "users";
    private final String TASKS = "tasks";
    private final String TASKID = "taskID";
    private final String ENTRIES = "entries";
    private final String POINTS = "points";

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
        functions = FirebaseFunctions.getInstance();
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

        // Click-Listener für Gesamtlayout zur Navigation in Detailansicht
        taskHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(taskHolder.itemView.getContext(), TaskActivity.class);
                i.putExtra(TASKID, getSnapshots().getSnapshot(position).getId());
                taskHolder.itemView.getContext().startActivity(i);
            }
        });

        // Hide User-Order when Task is defined as not-ordered
        if (!model.isOrdered()) taskHolder.layout_order.setVisibility(View.GONE);
        else taskHolder.layout_order.setVisibility(View.VISIBLE);
        // Pass-Button Visibility
        if (nextUser.getUserID().equals(userID) && model.isOrdered()) {
            taskHolder.btn_pass.setVisibility(View.VISIBLE);
            // PASS-Listener
            taskHolder.btn_pass.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Change the current's user position with the user after him
                    ArrayList<String> newOrder = (ArrayList<String>) model.getInvolvedIDs().clone();
                    newOrder.remove(userID);
                    newOrder.add(1, userID);

                    HashMap<String, Object> taskMap = new HashMap<>();
                    taskMap.put(INVOLVEDIDS, newOrder);
                    // UPDATE the involved-List in the Database
                    FirebaseFirestore.getInstance().collection(GROUPS).document(groupID).collection(TASKS)
                            .document(getSnapshots().getSnapshot(position).getId())
                            .update(taskMap);
                }
            });
        }
        else taskHolder.btn_pass.setVisibility(View.GONE);
        // NOTIFICATION-Button Visibility
        if (model.isOrdered()) {
            taskHolder.btn_remind.setVisibility(View.VISIBLE);
            // REMIND-Listener
            taskHolder.btn_remind.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    notifyInvolved(model.getTitle(), model.getInvolvedIDs().get(0));
                }
            });
        }
        else taskHolder.btn_remind.setVisibility(View.GONE);


        // DONE-Listener
        taskHolder.btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleTaskDone(taskHolder, position, model);
            }
        });

    }

    private void notifyInvolved(String taskName, String toUserID) {
        Map<String, Object> data = new HashMap<>();
        data.put("taskName", taskName);
        data.put("userID", toUserID);

        functions.getHttpsCallable("taskNotify").call(data);
    }

    // Handle actions to be done when User has finished/taken care of a task
    private void handleTaskDone(final TaskHolder taskHolder, final int position, final TaskModel model) {
        Toast.makeText(taskHolder.itemView.getContext(), model.getTitle() +" erledigt!", Toast.LENGTH_SHORT).show();
        // Identify the ID of the current task
        String taskID = getSnapshots().getSnapshot(position).getId();
        // Change order of the involved user's
        ArrayList<String> newOrder = (ArrayList<String>) model.getInvolvedIDs().clone();
        newOrder.remove(userID);
        newOrder.add(userID);
        // Update Involved-User's and Timestamp
        final HashMap<String, Object> taskMap = new HashMap<>();
        taskMap.put(TIMESTAMP,
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(model.getFrequenz())));
        taskMap.put(INVOLVEDIDS, newOrder);
        // Create a TaskEntry
        final TaskEntry entry = new TaskEntry(userID);

        final long partialPoints = Math.round((double) model.getPoints() / model.getInvolvedIDs().size());
        final long roundedPoints = partialPoints * model.getInvolvedIDs().size();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference colUsers = db.collection(GROUPS).document(groupID).collection(USERS);
        final DocumentReference docTask = db.collection(GROUPS).document(groupID).collection(TASKS).document(taskID);
        final DocumentReference docEntry = db.collection(GROUPS).document(groupID).collection(TASKS)
                .document(taskID).collection(ENTRIES).document();

        db.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                // Update the invovled User's points
                HashMap<String, Long> map_points = new HashMap<>();
                for (String id : model.getInvolvedIDs()) {
                    // Calculate user's new Points depending on whether he is the current user
                    // (the user who has fulfilled this task)
                    DocumentSnapshot snap_user = transaction.get(colUsers.document(id));
                    if (id.equals(userID))
                        map_points.put(id, snap_user.getLong(POINTS) + roundedPoints - partialPoints);
                    else
                        map_points.put(id, snap_user.getLong(POINTS) - partialPoints);
                }

                for (String id : map_points.keySet()) {
                    transaction.update(colUsers.document(id), POINTS, map_points.get(id));
                }
                // Update the Task-Order & Timestamp
                transaction.update(docTask, taskMap);
                // Save TaskEntry (im Verlauf)
                transaction.set(docEntry, entry);
                return null;
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
