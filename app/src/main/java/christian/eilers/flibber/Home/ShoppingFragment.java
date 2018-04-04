package christian.eilers.flibber.Home;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Article;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;
import static christian.eilers.flibber.Utils.Strings.*;

public class ShoppingFragment extends Fragment implements View.OnClickListener, View.OnFocusChangeListener{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mainView = inflater.inflate(R.layout.fragment_shopping, container, false);
        initializeViews();
        // Initialize Variables
        userID = LocalStorage.getUserID(getContext());
        groupID = LocalStorage.getGroupID(getContext());
        users = ((HomeActivity) getActivity()).getUsers();
        db = FirebaseFirestore.getInstance();
        ref_shopping = db.collection(GROUPS).document(groupID).collection(USERS).document(userID).collection(SHOPPING);
        // Load Shopping-List if User-List exists
        if (users != null) loadData();
        else {
            Intent main = new Intent(getContext(), MainActivity.class);
            startActivity(main);
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getActivity().finish();
        }
        return mainView;
    }

    // Initialize Layout
    private void initializeViews() {
        toolbar = mainView.findViewById(R.id.toolbar);
        recView = mainView.findViewById(R.id.shoppingList);
        et_article = mainView.findViewById(R.id.input_shopping);
        btn_save = mainView.findViewById(R.id.btn_save);
        btn_group = mainView.findViewById(R.id.btn_group);
        placeholder = mainView.findViewById(R.id.placeholder);

        et_article.setOnFocusChangeListener(this);
        et_article.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) saveArticle();
                return true;
            }
        });

        btn_save.setOnClickListener(this);
        btn_group.setOnClickListener(this);

        // Setup Toolbar as Actionbar
        ((HomeActivity)getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);
    }

    // Add article to the user's database
    private void saveArticle() {
        // Get the Article-Name
        final String articleName = et_article.getText().toString().trim();
        if (TextUtils.isEmpty(articleName)) return;
        et_article.setText("");

        // Create Article and a new document for it
        DocumentReference ref_article = ref_shopping.document();
        final Article article = new Article(ref_article.getId(), articleName, userID, !forAll);

        // Save Article private/for all
        if (!forAll) ref_article.set(article);
        else {
            CollectionReference ref_users = db.collection(GROUPS).document(groupID).collection(USERS);
            WriteBatch batch = db.batch();
            // Save article in each users collection
            for (User u : users.values()) {
                DocumentReference ref = ref_users.document(u.getUserID()).collection(SHOPPING).document(article.getKey());
                batch.set(ref, article);
            }
            batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // TODO: << Erfolgreich Arikel für jeden User hinzugefügt >>
                }
            });
        }
    }

    // Delete article from the database
    private void deleteArticle(Article article) {
        // If private: only delete from current user's collection
        if (article.isPrivate()) ref_shopping.document(article.getKey()).delete();
        // Otherwise: delete for every user in the group
        else {
            CollectionReference ref_users = db.collection(GROUPS).document(groupID).collection(USERS);
            WriteBatch batch = db.batch();
            // Save article in each users collection
            for (User u : users.values()) {
                DocumentReference ref = ref_users.document(u.getUserID()).collection(SHOPPING).document(article.getKey());
                batch.delete(ref);
            }
            batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // TODO: << Erfolgreich Arikel für jeden User hinzugefügt >>
                }
            });
        }
    }

    // Find all checked Articles and delete them
    private void findCheckedArticles()
    {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            try {
                CheckBox checkBox = ((ShoppingHolder) recView.findViewHolderForAdapterPosition(i)).checkBox;
                if (checkBox.isChecked()) {
                    Article article = (Article) adapter.getItem(i);
                    deleteArticle(article);
                }
            }
            // Falls onBindView noch nicht auf dem Item aufgerufen wurde (zb. wenn Article unmittelbar
            // vorher hinzugefügt wird
            catch (Exception e) {
                Crashlytics.logException(e);
            }

        }
    }

    // Load the Shopping-List, update checkedItems and set Listeners
    private void loadData() {
        Query shoppingQuery = ref_shopping.orderBy(TIMESTAMP, Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Article> options = new FirestoreRecyclerOptions.Builder<Article>()
                .setQuery(shoppingQuery, Article.class)
                .build();

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        adapter = new FirestoreRecyclerAdapter<Article, ShoppingHolder>(options) {

            @NonNull
            @Override
            public ShoppingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
                return new ShoppingHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final ShoppingHolder holder, int position, @NonNull final Article model) {
                // Set Checkbox-State
                holder.checkBox.setChecked(model.isChecked);
                // Artikelname
                holder.tv_article.setText(model.getName());
                // Username
                final User articleUser = users.get(model.getUserID());
                holder.tv_username.setText(articleUser.getName());
                // Timestamp
                if (model.getTimestamp() != null)
                    holder.tv_datum.setText(
                            DateUtils.getRelativeTimeSpanString(
                                    model.getTimestamp().getTime(),
                                    System.currentTimeMillis() + BUFFER,
                                    DateUtils.MINUTE_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_RELATIVE)
                    );
                // Group-Icon-Visibility
                if (model.isPrivate()) holder.img_group.setVisibility(View.GONE);
                else holder.img_group.setVisibility(View.VISIBLE);
                // Change Checkbox-State on Click
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        model.isChecked = !model.isChecked;
                        if (model.isChecked) holder.checkBox.setChecked(true);
                        else holder.checkBox.setChecked(false);

                    }
                });
            }

            @Override
            public void onDataChanged() {
                layoutManager.scrollToPositionWithOffset(0,0);
                if (getItemCount() == 0) placeholder.setVisibility(View.VISIBLE);
                else placeholder.setVisibility(View.GONE);
                super.onDataChanged();
            }
        };

        recView.setLayoutManager(layoutManager);
        recView.setAdapter(adapter);
    }

    // Custom Viewholder for Shopping-Articles
    public class ShoppingHolder extends RecyclerView.ViewHolder {
        TextView tv_username, tv_datum, tv_article;
        CheckBox checkBox;
        ImageView img_group;

        public ShoppingHolder(View itemView) {
            super(itemView);
            tv_username = itemView.findViewById(R.id.username);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_article = itemView.findViewById(R.id.article);
            checkBox = itemView.findViewById(R.id.checkbox);
            img_group = itemView.findViewById(R.id.img_group);
        }
    }

    // Initalize Toolbar and their Buttons
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.menu_shopping, menu);
        menu.findItem(R.id.action_finish).getActionView().setOnClickListener(this);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // Hide Keyboard on click outside
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
        else if (id == R.id.action_finish) {
            findCheckedArticles();
        }
        else if (id == R.id.btn_group) {
            forAll = !forAll;
            if (forAll) btn_group.setColorFilter(getResources().getColor(R.color.colorAccent));
            else btn_group.setColorFilter(getResources().getColor(R.color.colorPrimary));
        }
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
    private CollectionReference ref_shopping;
    private FirestoreRecyclerAdapter adapter;
    private String groupID, userID;
    private HashMap<String, User> users;
    private boolean forAll = false;

    private View mainView;
    private Toolbar toolbar;
    private RecyclerView recView;
    private EditText et_article;
    private ImageButton btn_save, btn_group;
    private TextView placeholder;
    private Menu menu;
}
