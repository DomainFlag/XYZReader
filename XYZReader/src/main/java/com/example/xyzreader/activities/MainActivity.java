package com.example.xyzreader.activities;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.example.xyzreader.Constants;
import com.example.xyzreader.R;
import com.example.xyzreader.adapters.ArticleDetailAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;

import java.util.HashMap;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, TextView.OnEditorActionListener {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private Map<String, ActiveFilter> activeFilters = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(getSupportActionBar() != null)
            getSupportActionBar().hide();

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(mRecyclerView);
        getLoaderManager().initLoader(0, null, this);

        if(savedInstanceState == null) {
            refresh();
        }

        initFilters();
        generateFilters();
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        ArticleDetailAdapter adapter = new ArticleDetailAdapter(this, cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);

        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);

        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public NetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);

            thumbnailView = (NetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }

    public void onMainSearch(View view) {
        initSearch();
    }

    public void initSearch() {
        View mainSearchContainer = findViewById(R.id.main_search_container);
        mainSearchContainer.setVisibility(View.VISIBLE);

        final EditText editText = (EditText) findViewById(R.id.search_edit);
        editText.setOnEditorActionListener(this);
        editText.requestFocus();

        final View editTextBorder = findViewById(R.id.search_edit_border);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int len = charSequence.length();

                if(len == 0 || charSequence.equals(getResources().getString(R.string.app_filter_search_hint))) {
                    editTextBorder.getLayoutParams().width = 8;
                } else {
                    Paint paint = new Paint();
                    paint.setTextSize(18);
                    paint.setTypeface(Typeface.MONOSPACE);

                    float nowWidth = paint.measureText(charSequence.toString());

                    editTextBorder.getLayoutParams().width = Math.max((int) nowWidth, 8);
                }

                editTextBorder.requestLayout();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };

        editText.addTextChangedListener(textWatcher);
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionType, KeyEvent keyEvent) {
        if(actionType == EditorInfo.IME_ACTION_SEARCH) {
            EditText editText = (EditText) findViewById(R.id.search_edit);
            editText.clearFocus();

            View mainSearchContainer = findViewById(R.id.main_search_container);
            mainSearchContainer.setVisibility(View.GONE);
            // Your piece of code on keyboard search click
        }

        return false;
    }

    public void initFilters() {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.FILTER_PREFERENCES, MODE_PRIVATE);

        for(Constants.Filter filterType : Constants.Filter.values()) {
            String filterTypeName = filterType.name();

            if(sharedPreferences.contains(filterTypeName)) {
                ActiveFilter activeFilter = new ActiveFilter(sharedPreferences.getString(filterTypeName, ""), filterTypeName);

                activeFilters.put(filterTypeName, activeFilter);
            }
        }
    }

    private class ActiveFilter {

        private TextView textView;
        private String activeOption;
        private String filterType;

        private ActiveFilter(String activeOption, String filterType, TextView textView) {
            this.activeOption = activeOption;
            this.filterType = filterType;
            this.textView = textView;
        }

        private ActiveFilter(String activeOption, String filterType) {
            this.activeOption = activeOption;
            this.filterType = filterType;
        }
    }

    /**
     * Managing the preferences
     */
    private void changePreferenceFilter(String preferenceType, String value, TextView textView, boolean rewrite) {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.FILTER_PREFERENCES, MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        if(!sharedPreferences.contains(preferenceType) && !rewrite) {
            ActiveFilter activeFilter = new ActiveFilter(value, preferenceType);
            activeFilters.put(preferenceType, activeFilter);

            editor.putString(preferenceType, value);
            editor.apply();
        }

        if(rewrite && textView != null) {
            ActiveFilter activeFilter = activeFilters.get(preferenceType);

            activeFilter.textView.setBackgroundResource(0);
            activeFilter.textView = textView;
            activeFilter.textView.setBackgroundResource(R.drawable.border_view);
            activeFilter.activeOption = value;

            editor.putString(preferenceType, value);
            editor.apply();
        }
    }

    private void generateFilters() {
        LayoutInflater layoutInflater = getLayoutInflater();
        LinearLayout linearLayout = findViewById(R.id.main_filter);

        String[] filterByOptions = getResources().getStringArray(R.array.filter_by_options_array);
        if(filterByOptions.length > 0)
            changePreferenceFilter(Constants.Filter.FILTER_PREFERENCE_OPTION_BY.name(), filterByOptions[0], null, false);

        generateFilterOptions(layoutInflater, linearLayout, filterByOptions, Constants.Filter.FILTER_PREFERENCE_OPTION_BY.name());

        // Adding a divider to differentiate different types of filtering
        generateFilterOption(layoutInflater, linearLayout, "|", null);

        String[] filterOptions = getResources().getStringArray(R.array.filter_options_array);
        if(filterOptions.length > 0)
            changePreferenceFilter(Constants.Filter.FILTER_PREFERENCE_OPTION_ON.name(), filterOptions[0], null, false);

        generateFilterOptions(layoutInflater, linearLayout, filterOptions, Constants.Filter.FILTER_PREFERENCE_OPTION_ON.name());

        generateExpandedFilterOptions(layoutInflater, Constants.Filter.FILTER_PREFERENCE_SUB_OPTION.name());
    }

    private void generateExpandedFilterOptions(LayoutInflater inflater, String type) {
        LinearLayout linearLayout = findViewById(R.id.main_filter_expanded);

        String[] expandedFilterOptions = getResources().getStringArray(R.array.filter_option_genres_array);
        if(expandedFilterOptions.length > 0)
            changePreferenceFilter(Constants.Filter.FILTER_PREFERENCE_SUB_OPTION.name(), expandedFilterOptions[0], null, false);

        generateFilterOptions(inflater, linearLayout, expandedFilterOptions, type);
    }

    private void generateFilterOptions(LayoutInflater inflater, LinearLayout parentContainer, String[] filterOptions, String type) {
        for(String filterOption : filterOptions) {
            generateFilterOption(inflater, parentContainer, filterOption, type);
        }
    }

    private void generateFilterOption(LayoutInflater inflater, LinearLayout parentContainer, final String filterOption, final String type) {
        TextView filterTextView;

        filterTextView = (TextView) inflater.inflate(R.layout.article_filter_option_layout, parentContainer, false);
        filterTextView.setText(filterOption);

        for(String key : activeFilters.keySet()) {
            ActiveFilter activeFilter = activeFilters.get(key);

            if(filterOption.equals(activeFilter.activeOption)) {
                filterTextView.setBackgroundResource(R.drawable.border_view);

                activeFilter.textView = filterTextView;
            }
        }

        filterTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePreferenceFilter(type, filterOption, (TextView) view, true);
            }
        });

        parentContainer.addView(filterTextView);
    }
}
