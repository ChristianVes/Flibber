package christian.eilers.flibber.Home;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import christian.eilers.flibber.R;

public class HomeFragment extends Fragment {

    public HomeFragment() {}

    public static HomeFragment newInstance() {
        if(thisFragment == null) thisFragment = new HomeFragment();
        return thisFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_home, container, false);
        recView = mainView.findViewById(R.id.recView);
        fab = mainView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        return mainView;
    }

    private static HomeFragment thisFragment;

    private View mainView;
    private RecyclerView recView;
    private FloatingActionButton fab;


}
