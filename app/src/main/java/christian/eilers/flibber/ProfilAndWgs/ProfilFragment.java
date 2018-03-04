package christian.eilers.flibber.ProfilAndWgs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import christian.eilers.flibber.LoginActivity;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.Utils;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfilFragment extends Fragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_profil, container, false);
        initializeViews();
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference().child("profile_pictures");
        loadData();
        return mainView;
    }

    // Initialize views from layout file
    private void initializeViews() {
        btn_logout = mainView.findViewById(R.id.btn_logout);
        v_name = mainView.findViewById(R.id.name);
        profileImage = mainView.findViewById(R.id.profile_image);
        progressBar = mainView.findViewById(R.id.progressBar);
        btn_logout.setOnClickListener(this);
        profileImage.setOnClickListener(this);
    }

    // Lade Usernamen und Profilbild aus dem Firebase Storage
    private void loadData() {
        v_name.setText(Utils.getUSERNAME());
        if (Utils.getPICPATH() != null)
            GlideApp.with(getContext())
                    .load(storage.child(Utils.getPICPATH()))
                    .dontAnimate()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(profileImage);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // Log out the current User, delete local-cached data and go to LoginActivity
        if (id == R.id.btn_logout) {
            auth.signOut();
            Utils.setLocalData(getContext(), null, null, null, null);
            Intent login = new Intent(getActivity(), LoginActivity.class);
            startActivity(login);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
        }
        // To Gallery
        else if (id == R.id.profile_image) {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
        }
    }

    // Gallery Intent
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Zuschneiden des Bildes auf 1:1 Ratio
        if (resultCode != Activity.RESULT_CANCELED) {
            if (requestCode == REQUEST_CODE_GALLERY) {
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1, 1)
                        .start(getContext(), this);
            } else super.onActivityResult(requestCode, resultCode, data);
        }

        // Zurückliefern des zugeschnitten Bildes
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == Activity.RESULT_OK) {
                imageUri = result.getUri();
                saveImage();
            }
            // On Error
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Crashlytics.logException(result.getError());
            }
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    // Save the Image in the FirebaseStorage and the Reference in the Database
    private void saveImage() {
        progressBar.setVisibility(View.VISIBLE);
        // TODO: randomStringGenerator instead of LastPathSegment
        final String picPath = imageUri.getLastPathSegment();
        storage.child(picPath).putFile(imageUri)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imageUri = null;
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Fehler beim Upload!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Delete old Picture from Storage
                        if (Utils.getPICPATH() != null)
                            storage.child(Utils.getPICPATH()).delete();
                        updateRefs(picPath);


                        Map<String, Object> profilePic = new HashMap<>();
                        profilePic.put("picPath", picPath);
                        db.collection("users").document(Utils.getUSERID()).update(profilePic).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                // Update Local-cached data
                                Utils.setLocalData(getContext(), Utils.getWGKEY(), Utils.getUSERID(), Utils.getUSERNAME(), picPath);
                                // Load Picture into ImageView
                                loadData();
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }
                });
    }

    // Update Reference to the Profile-Picture in the database
    private void updateRefs(final String picPath) {
        final Map<String, Object> profilePic = new HashMap<>();
        profilePic.put("picPath", picPath);
        // Update Reference in the users-collection
        db.collection("users").document(Utils.getUSERID()).update(profilePic)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Update Local-cached data
                        Utils.setLocalData(getContext(), Utils.getWGKEY(), Utils.getUSERID(), Utils.getUSERNAME(), picPath);
                        // Load Picture into ImageView
                        loadData();
                        progressBar.setVisibility(View.GONE);
                    }
                });

        // Update References in every WG the current user is part of
        db.collection("users").document(Utils.getUSERID()).collection("wgs").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        for (DocumentSnapshot doc : documentSnapshots) {
                            db.collection("wgs").document(doc.getId()).collection("users").document(Utils.getUSERID()).update(profilePic);
                        }
                    }
                });
    }

    private View mainView;
    private Button btn_logout;
    private TextView v_name;
    private CircleImageView profileImage;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private StorageReference storage;
    private Uri imageUri;

    private static final int REQUEST_CODE_GALLERY = 0;
}
