package christian.eilers.flibber.ProfilAndWgs;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.Models.Group;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class GroupCreationFragment extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.dialog_wg_erstellen, container, false);
        initializeViews();
        getDialog().setTitle("Neue WG erstellen");
        db = FirebaseFirestore.getInstance();
        userID = LocalStorage.getUserID(getContext());
        btn_erstellen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createWG();
            }
        });
        btn_abbrechen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });
        return mainView;
    }

    private void createWG() {
        final String groupName = eT_groupName.getText().toString().trim();
        if(TextUtils.isEmpty(groupName)){
            Toast.makeText(getContext(), "Keinen Gruppen-Namen eingegeben", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create new Group
        progressBar.setVisibility(View.VISIBLE);
        final DocumentReference reference = db.collection(GROUPS).document();
        final Group group = new Group(groupName, reference.getId(), null);
        reference.set(group);

        // Add Group-Reference to the current user
        db.collection(USERS).document(userID).collection(GROUPS).document(group.getKey()).set(group);

        // Add user to the Group
        db.collection(USERS).document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()) {
                    Crashlytics.logException(task.getException());
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                final String username = task.getResult().getString(NAME);
                final String email = task.getResult().getString(EMAIL);
                final String picPath = task.getResult().getString(PICPATH);
                final User user = new User(username, email, userID, picPath, 0.0);
                db.collection(GROUPS).document(group.getKey()).collection(USERS).document(userID).set(user);
                progressBar.setVisibility(View.GONE);
                eT_groupName.setText("");
                getDialog().dismiss();
            }
        });
    }

    // Initialize views from layout file
    private void initializeViews() {
        image = mainView.findViewById(R.id.image);
        eT_groupName = mainView.findViewById(R.id.editText_name);
        btn_erstellen = mainView.findViewById(R.id.btn_erstellen);
        btn_abbrechen = mainView.findViewById(R.id.btn_abbrechen);
        progressBar = mainView.findViewById(R.id.progressBar);
    }


    private View mainView;
    private ImageView image;
    private EditText eT_groupName;
    private Button btn_erstellen, btn_abbrechen;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String userID;

    private final String GROUPS = "wgs";
    private final String USERS = "users";
    private final String NAME = "name";
    private final String EMAIL = "email";
    private final String PICPATH = "picPath";

}
