package christian.eilers.flibber.Profil;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;
import de.hdodenhof.circleimageview.CircleImageView;
import static christian.eilers.flibber.Utils.Strings.*;

public class ProfilFragment extends Fragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_profil, container, false);
        initializeViews();
        initalizeVariables();
        loadData();
        return mainView;
    }

    // Initialize views from layout file
    private void initializeViews() {
        v_name = mainView.findViewById(R.id.name);
        profileImage = mainView.findViewById(R.id.profile_image);
        progressBar = mainView.findViewById(R.id.progressBar);
        profileImage.setOnClickListener(this);
    }

    // Initialize variables
    private void initalizeVariables() {
        userID = LocalStorage.getUserID(getContext());
        userName = LocalStorage.getUsername(getContext());
        picPath = LocalStorage.getPicPath(getContext());
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference().child(PROFILE);
    }

    // Lade Usernamen und Profilbild aus dem Firebase Storage
    private void loadData() {
        v_name.setText(userName);
        if (picPath != null)
            GlideApp.with(getContext())
                    .load(storage.child(picPath))
                    .dontAnimate()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(profileImage);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // To Gallery
        if (id == R.id.profile_image) {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
        }
    }

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
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Crashlytics.logException(result.getError());
            }
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    // Save the Image in the FirebaseStorage and its Reference in the Database
    private void saveImage() {
        progressBar.setVisibility(View.VISIBLE);
        // TODO: randomStringGenerator instead of LastPathSegment
        final String newPicPath = imageUri.getLastPathSegment();
        storage.child(newPicPath).putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Delete old Picture from Storage
                        if (picPath != null)
                            storage.child(picPath).delete();
                        picPath = newPicPath;
                        updateRefs();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imageUri = null;
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Fehler beim Upload!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Update all References to the Profile-Picture of the current user in the database
    private void updateRefs() {
        final Map<String, Object> userData = new HashMap<>();
        userData.put(PICPATH, picPath);
        // Update Reference in the users-collection
        db.collection(USERS).document(userID).update(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Get all groups the user is part of
                        db.collection("users").document(userID).collection(GROUPS).get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot documentSnapshots) {
                                        // Update Picture-References in each group
                                        for (DocumentSnapshot doc : documentSnapshots) {
                                            db.collection(GROUPS).document(doc.getId()).collection(USERS).document(userID).update(userData);
                                        }
                                        LocalStorage.setPicPath(getContext(), picPath);
                                        loadData();
                                        progressBar.setVisibility(View.GONE);
                                    }
                                });
                    }
                });
    }

    private View mainView;
    private TextView v_name;
    private CircleImageView profileImage;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private StorageReference storage;

    private Uri imageUri;
    private String userID, userName, picPath;

    private final int REQUEST_CODE_GALLERY = 0;
}
