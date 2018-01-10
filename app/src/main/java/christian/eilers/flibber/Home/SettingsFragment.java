package christian.eilers.flibber.Home;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import christian.eilers.flibber.Models.Wg;
import christian.eilers.flibber.ProfilAndWgs.WgsAndProfilActivity;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.Utils;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        btn_invite = view.findViewById(R.id.btn_invite);
        btn_profil = view.findViewById(R.id.btn_profil);

        btn_invite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createDialog();
            }
        });
        btn_profil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Lösche WG Key und wechsel zur WG&Profil Activity
                Utils.setLocalData(getActivity(), null, Utils.getUSERID(), Utils.getUSERNAME());
                Intent profilIntent = new Intent(getContext(), WgsAndProfilActivity.class);
                startActivity(profilIntent);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                getActivity().finish();
            }
        });
        return view;
    }

    private void createDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_invite, null);
        Button btn = v.findViewById(R.id.button_ok);
        eT_email = v.findViewById(R.id.editText_email);
        progressBar = v.findViewById(R.id.progressBar);
        dialog = makeAlertDialog(v);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = eT_email.getText().toString().trim();
                invite(email);
            }
        });
        eT_email.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) {
                    String email = eT_email.getText().toString().trim();
                    invite(email);
                    return true;
                }
                return false;
            }
        });
    }

    // Send an Invitation to the user matching the given E-Mail Adress to join the WG
    private void invite(final String email) {
        // Check if E-Mail is not empty
        if(TextUtils.isEmpty(email)){
            Toast.makeText(getContext(), "Keine E-Mail eingegeben", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }
        // Set Loading Animation
        progressBar.setVisibility(View.VISIBLE);
        eT_email.setVisibility(View.GONE);

        // Check if user with the given email exists
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        // Check if user with the given email exists
                        if(documentSnapshots.isEmpty()) {
                            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            return;
                        }
                        // Get the Document with the matching email (IS ALWAYS A SINGLE DOCUMENT!)
                        for(final DocumentSnapshot docUser : documentSnapshots) {
                            DocumentReference ref_user = docUser.getReference();
                            checkIfMember(ref_user);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Failure", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
    }

    // Check if user already is member of the WG
    private void checkIfMember(final DocumentReference ref_user) {
        ref_user.collection("wgs").document(Utils.getWGKEY()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()) {
                            Toast.makeText(getContext(), "Already member of WG", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            checkIfInvited(ref_user);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Failure", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
    }

    // Check if user already has an invitation
    private void checkIfInvited(final DocumentReference ref_user) {
        ref_user.collection("invitations").document(Utils.getWGKEY()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()) {
                            Toast.makeText(getContext(), "Already has an invitation", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            sendInvitation(ref_user);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Failure", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
    }

    // Create Invitation-Documents in the Users-Collection and WG-Collection
    private void sendInvitation(final DocumentReference ref_user) {
        db.collection("wgs").document(Utils.getWGKEY()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot currentWGSnapshot) {
                        // Create WG-Object of current WG
                        final Wg currentWG = currentWGSnapshot.toObject(Wg.class);
                        // Save a Reference to the invited User in the Invitation-Collection of the WG
                        Map<String,Object> map_user = new HashMap<>();
                        map_user.put("path" , ref_user.getPath());

                        currentWGSnapshot.getReference().collection("invitations").add(map_user)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        // OnSuccesListener gespart...
                                        ref_user.collection("invitations").document(Utils.getWGKEY()).set(currentWG);
                                        Toast.makeText(getContext(), "User invited", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getContext(), "Failure", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Failure", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
    }

    // Make an AlertDialog from a View
    public AlertDialog makeAlertDialog(View v) {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(getContext());
        mBuilder.setView(v);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
        return dialog;
    }

    private Button btn_invite, btn_profil;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Dialog Variables
    private ProgressBar progressBar;
    private AlertDialog dialog;
    private EditText eT_email;
}
