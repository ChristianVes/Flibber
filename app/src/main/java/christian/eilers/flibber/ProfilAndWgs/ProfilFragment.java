package christian.eilers.flibber.ProfilAndWgs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import christian.eilers.flibber.LoginActivity;
import christian.eilers.flibber.R;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfilFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_profil, container, false);

        btn_logout = mainView.findViewById(R.id.btn_logout);
        v_name = mainView.findViewById(R.id.name);
        profileImage= mainView.findViewById(R.id.profile_image);
        progressBar = mainView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        auth = FirebaseAuth.getInstance();
        userID = auth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();

        loadData();

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                auth.signOut();
                Intent login = new Intent(getActivity(), LoginActivity.class);
                startActivity(login);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                getActivity().finish();
            }
        });
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, 0);
            }
        });

        return mainView;
    }

    // Lade Usernamen und Profilbild aus Database
    private void loadData() {
        db.collection("users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                final String name = task.getResult().getString("name");
                final String picPath = task.getResult().getString("picPath");
                v_name.setText(name);
                if(picPath != null) {
                    Picasso.with(getActivity()).load(picPath).fit().networkPolicy(NetworkPolicy.OFFLINE)
                            .into(profileImage, new Callback() {
                                @Override
                                public void onSuccess() {
                                    progressBar.setVisibility(View.GONE);
                                }

                                @Override
                                public void onError() {
                                    Picasso.with(getActivity()).load(picPath).fit().into(profileImage);
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                } else progressBar.setVisibility(View.GONE);

            }
        });
    }

    // Gallery Intent
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Zuschneiden des Bildes auf 1:1 Ratio
        if (resultCode != Activity.RESULT_CANCELED) {
            if (requestCode == 0) {
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1, 1)
                        .start(getContext(), this);
            }
            else super.onActivityResult(requestCode, resultCode, data);
        }

        // Zurückliefern des zugeschnitten Bildes
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == Activity.RESULT_OK) {
                // Display the chosen Image in the ImageView
                imageUri = result.getUri();
                profileImage.setImageURI(imageUri);
                // Save the chosen Image online
                saveImage();
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Crashlytics.logException(error);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Save the Image in the FirebaseStorage and Firesotre
    private void saveImage() {
        progressBar.setVisibility(View.VISIBLE);
        // Storage
        storage.child(userID).putFile(imageUri)
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
                        imageUri = taskSnapshot.getDownloadUrl();
                        Map<String, Object> userImage = new HashMap<>();
                        userImage.put("picPath", imageUri.toString());
                        // Database
                        db.collection("users").document(userID).update(userImage).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                progressBar.setVisibility(View.GONE);
                            }
                        });
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

    private String userID;
    private Uri imageUri;

}
