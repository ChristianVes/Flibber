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

    // Instanzierung des Dialogs
    public static GroupCreationFragment newInstance() {
        if(thisDialog == null) thisDialog =  new GroupCreationFragment();
        return thisDialog;
    }

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
        final String wgName = eT_wgName.getText().toString().trim();
        if(TextUtils.isEmpty(wgName)){
            Toast.makeText(getContext(), "Keinen Namen eingegeben", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        // Create new WG-Document
        final DocumentReference ref_wg = db.collection("wgs").document();
        final Group wg = new Group(wgName, ref_wg.getId(), null);
        ref_wg.set(wg);

        // Add WG to the current user's WG-Collection
        db.collection("users").document(userID).collection("wgs").document(wg.getKey()).set(wg);
        // Add user to the WG
        db.collection("users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()) {
                    Crashlytics.logException(task.getException());
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                String username = task.getResult().getString("name");
                String email = task.getResult().getString("email");
                String picPath = task.getResult().getString("picPath");
                User user = new User(username, email, userID, picPath, 0.0);
                db.collection("wgs").document(wg.getKey()).collection("users").document(userID).set(user);
                progressBar.setVisibility(View.GONE);
                eT_wgName.setText("");
                getDialog().dismiss();
            }
        });
    }

    // Initialize views from layout file
    private void initializeViews() {
        image = mainView.findViewById(R.id.image);
        eT_wgName = mainView.findViewById(R.id.editText_name);
        btn_erstellen = mainView.findViewById(R.id.btn_erstellen);
        btn_abbrechen = mainView.findViewById(R.id.btn_abbrechen);
        progressBar = mainView.findViewById(R.id.progressBar);
    }


    private static GroupCreationFragment thisDialog;

    private View mainView;
    private ImageView image;
    private EditText eT_wgName;
    private Button btn_erstellen, btn_abbrechen;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String userID;
}
