package christian.eilers.flibber.Home;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;

import christian.eilers.flibber.MainActivity;
import christian.eilers.flibber.Models.Article;
import christian.eilers.flibber.Models.User;
import christian.eilers.flibber.R;
import christian.eilers.flibber.Utils.LocalStorage;

public class ShoppingFragment extends Fragment implements View.OnClickListener, View.OnFocusChangeListener{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mainView = inflater.inflate(R.layout.fragment_shopping, container, false);
        initializeViews();
        userID = LocalStorage.getUserID(getContext());
        groupID = LocalStorage.getGroupID(getContext());
        users = ((HomeActivity) getActivity()).getUsers();
        ref_shopping = FirebaseFirestore.getInstance().collection(GROUPS).document(groupID).collection(USERS).document(userID).collection(SHOPPING);
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
        refreshLayout = mainView.findViewById(R.id.shoppingRefreshLayout);
        et_article = mainView.findViewById(R.id.input_shopping);
        btn_save = mainView.findViewById(R.id.btn_save);
        btn_more = mainView.findViewById(R.id.btn_more);

        et_article.setOnFocusChangeListener(this);
        et_article.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) saveArticle();
                return true;
            }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                adapter.notifyDataSetChanged();
                refreshLayout.setRefreshing(false);
            }
        });

        btn_save.setOnClickListener(this);
        btn_more.setOnClickListener(this);

        ((HomeActivity)getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);
    }

    // Add article to the user's database
    private void saveArticle() {
        final String articleName = et_article.getText().toString().trim();
        if (TextUtils.isEmpty(articleName)) return;
        et_article.setText("");

        DocumentReference ref_article = ref_shopping.document();
        final Article article = new Article(ref_article.getId(), articleName, userID, true);
        ref_article.set(article);
    }

    // Delete article from the shopping list and compute costs
    private void deleteArticle(String key) {
        ref_shopping.document(key).delete();
    }

    private void updateButtonVisibility() {
        if (checkedItems == null) return;
        if (checkedItems.isEmpty()) {
            menu.findItem(R.id.action_finish).setVisible(false);
        }
        else {
            menu.findItem(R.id.action_finish).setVisible(true);
        }
    }

    // Load the Shopping-List, update checkedItems and set Listeners
    private void loadData() {
        Query shoppingQuery = ref_shopping.orderBy(TIMESTAMP, Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Article> options = new FirestoreRecyclerOptions.Builder<Article>()
                .setQuery(shoppingQuery, Article.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Article, ShoppingHolder>(options) {

            @NonNull
            @Override
            public ShoppingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                checkedItems = new HashMap<>();
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
                return new ShoppingHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final ShoppingHolder holder, int position, @NonNull final Article model) {
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

                // Open Dialog onItemClick
                // TODO: umschreiben sodass Item "gecheckt" wird...Dialog nur bei Button-Click
                // TODO: Dialog hat Option: "als Finanzeintrag" sodass Beteiligte-Auswahl möglich ist
                // TODO: Erstmal einkaufslisten ohne Finanzsystem, dafür User-übergreifend ermöglichen
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new MaterialDialog.Builder(getContext())
                                .title(model.getName())
                                .inputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                                .input("Preis...", null, true, new MaterialDialog.InputCallback() {
                                    @Override
                                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                                    }
                                })
                                .positiveText("Eingekauft!")
                                .negativeText("Abbrechen")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        deleteArticle(model.getKey());
                                        checkedItems.remove(model.getKey());
                                        updateButtonVisibility();
                                    }
                                })
                                .show();
                    }
                });

                holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        if(isChecked){
                            checkedItems.put(model.getKey(), model);
                        }
                        else{
                            checkedItems.remove(model.getKey());
                        }
                        updateButtonVisibility();
                    }
                });

            }
        };

        recView.setLayoutManager(new LinearLayoutManager(getContext()));
        recView.setAdapter(adapter);
    }

    // Custom Viewholder for Shopping-Articles
    public class ShoppingHolder extends RecyclerView.ViewHolder {
        TextView tv_username, tv_datum, tv_article;
        CheckBox checkBox;

        public ShoppingHolder(View itemView) {
            super(itemView);
            tv_username = itemView.findViewById(R.id.username);
            tv_datum = itemView.findViewById(R.id.datum);
            tv_article = itemView.findViewById(R.id.article);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }

    // Initalize Toolbar and their Buttons
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.menu_shopping, menu);
        menu.findItem(R.id.action_finish).getActionView().setOnClickListener(this);
        updateButtonVisibility();
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
            for (Article a : checkedItems.values()) {
                deleteArticle(a.getKey());
            }
            checkedItems.clear();
            updateButtonVisibility();
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


    private CollectionReference ref_shopping;
    private FirestoreRecyclerAdapter adapter;
    private String groupID, userID;
    private HashMap<String, Article> checkedItems;
    private HashMap<String, User> users;

    private View mainView;
    private Toolbar toolbar;
    private RecyclerView recView;
    private EditText et_article;
    private ImageButton btn_save, btn_more;
    private Menu menu;
    private SwipeRefreshLayout refreshLayout;

    private final String GROUPS = "groups";
    private final String USERS = "users";
    private final String SHOPPING = "shopping";
    private final String TIMESTAMP = "timestamp";
    private final int BUFFER = 10000; // Millisekunden // entspricht 10 Sekunden

}
