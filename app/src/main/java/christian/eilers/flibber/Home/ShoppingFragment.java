package christian.eilers.flibber.Home;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter_LifecycleAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

import christian.eilers.flibber.Models.Article;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.GlideApp;
import christian.eilers.flibber.Utils.LocalStorage;

public class ShoppingFragment extends Fragment implements View.OnClickListener, View.OnFocusChangeListener{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mainView = inflater.inflate(R.layout.fragment_shopping, container, false);
        initializeViews();
        userID = LocalStorage.getUserID(getContext());
        groupID = LocalStorage.getGroupID(getContext());
        db = FirebaseFirestore.getInstance();
        loadData();
        return mainView;
    }

    private void initializeViews() {
        toolbar = mainView.findViewById(R.id.toolbar);
        recView = mainView.findViewById(R.id.shoppingList);
        bottomLayout = mainView.findViewById(R.id.bottomLayout);
        et_article = mainView.findViewById(R.id.input_shopping);
        btn_save = mainView.findViewById(R.id.btn_save);
        btn_more = mainView.findViewById(R.id.btn_more);

        et_article.setOnFocusChangeListener(this);

        btn_save.setOnClickListener(this);
        btn_more.setOnClickListener(this);
    }

    private void saveArticle() {
        final String articleName = et_article.getText().toString().trim();
        if (TextUtils.isEmpty(articleName)) return;
        et_article.setText("");

        final Article article = new Article(articleName, userID, true);
        db.collection(GROUPS).document(groupID)
                .collection(USERS).document(userID)
                .collection(SHOPPING).add(article);
    }

    private void deleteArticle(String key) {
        db.collection(GROUPS).document(groupID)
                .collection(USERS).document(userID)
                .collection(SHOPPING).document(key)
                .delete();
    }

    private void loadData() {
        Query shoppingQuery = db.collection(GROUPS).document(groupID)
                .collection(USERS).document(userID)
                .collection(SHOPPING).orderBy(TIMESTAMP, Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Article> options = new FirestoreRecyclerOptions.Builder<Article>()
                .setQuery(shoppingQuery, Article.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Article, ShoppingHolder>(options) {
            @NonNull
            @Override
            public ShoppingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
                return new ShoppingHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final ShoppingHolder holder, int position, @NonNull final Article model) {
                holder.itemView.setVisibility(View.GONE);
                db.collection(GROUPS).document(groupID).collection(USERS).document(model.getUserID()).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                final User articleUser = documentSnapshot.toObject(User.class);
                                holder.tv_article.setText(model.getName());
                                holder.tv_username.setText(articleUser.getName());
                                if (model.getTimestamp() != null)
                                    holder.tv_datum.setText(
                                            DateUtils.getRelativeTimeSpanString(
                                                    model.getTimestamp().getTime(),
                                                    System.currentTimeMillis() + BUFFER,
                                                    DateUtils.MINUTE_IN_MILLIS,
                                                    DateUtils.FORMAT_ABBREV_RELATIVE)
                                    );
                                // Überprüft ob es sich um einen noch nicht an die Database gesendeten Comment handelt
                                boolean offline = getSnapshots().getSnapshot(holder.getAdapterPosition()).getMetadata().hasPendingWrites();
                                if(!offline) holder.itemView.setVisibility(View.VISIBLE);

                                // Öffne Dialog zum Löschen des Einkaufsartikels
                                holder.itemView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        new MaterialDialog.Builder(getContext())
                                                .title(model.getName())
                                                .inputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                                                .input("Preis", null, true, new MaterialDialog.InputCallback() {
                                                    @Override
                                                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                                                    }
                                                })
                                                .positiveText("Eingekauft!")
                                                .negativeText("Abbrechen")
                                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                        deleteArticle(getSnapshots().getSnapshot(holder.getAdapterPosition()).getId());
                                                    }
                                                })
                                                .show();
                                    }
                                });
                            }
                        });
            }
        };


        recView.setLayoutManager(new LinearLayoutManager(getContext()));
        recView.setAdapter(adapter);
    }

    public class ShoppingHolder extends RecyclerView.ViewHolder {
        TextView tv_username, tv_datum, tv_article;
        // CheckBox checkBox;

        public ShoppingHolder(View itemView) {
            super(itemView);
            tv_username = itemView.findViewById(R.id.username);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_article = itemView.findViewById(R.id.article);
            // checkBox = itemView.findViewById(R.id.checkbox);
        }


    }



    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus) {
            InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_save) saveArticle();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }


    private FirebaseFirestore db;
    private FirestoreRecyclerAdapter adapter;
    private String groupID, userID;

    private View mainView;
    private Toolbar toolbar;
    private RecyclerView recView;
    private RelativeLayout bottomLayout;
    private EditText et_article;
    private ImageButton btn_save, btn_more;

    private final String GROUPS = "groups";
    private final String USERS = "users";
    private final String SHOPPING = "shopping";
    private final String TIMESTAMP = "timestamp";
    private final int BUFFER = 10000; // Millisekunden // entspricht 10 Sekunden

}
