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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.Constants;
import com.example.xyzreader.R;
import com.example.xyzreader.adapters.ArticleDetailAdapter;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, TextView.OnEditorActionListener {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private ArticleDetailAdapter adapter;

    private Map<String, ActiveFilter> activeFilters = new HashMap<>();

    private SnapHelper snapHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(getSupportActionBar() != null)
            getSupportActionBar().hide();

        adapter = new ArticleDetailAdapter(this, null);
        adapter.setHasStableIds(true);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mRecyclerView.setAdapter(adapter);

        snapHelper = new PagerSnapHelper();
        renderWindowOption(R.id.view_single_window);

        getLoaderManager().initLoader(0, null, this);

        if(savedInstanceState == null) {
            refresh();
        }

        initWindowOptions();
        initFilters();
        generateFilters();
    }

    public void initWindowOptions() {
        LinearLayout linearLayout = findViewById(R.id.view_changeling);

        final ArrayList<View> children = new ArrayList<>();
        int length = linearLayout.getChildCount();

        for(int it = 0; it < length; it++) {
            children.add(linearLayout.getChildAt(it));

            linearLayout.getChildAt(it).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for(int it = 0; it < children.size(); it++) {
                        View child = children.get(it);

                        if(child.getId() != view.getId()) {
                            child.setAlpha(0.4f);
                        } else child.setAlpha(1.0f);
                    }

                    renderWindowOption(view.getId());
                }
            });
        }
    }

    public void renderWindowOption(int identifier) {
        switch(identifier) {
            case R.id.view_multi_window : {
                GridLayoutManager gridLayoutManager = new GridLayoutManager(this,
                        3,
                        LinearLayoutManager.VERTICAL,
                        false);

                snapHelper.attachToRecyclerView(null);

                mRecyclerView.setLayoutManager(gridLayoutManager);
                break;
            }
            case R.id.view_single_window : {
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                        LinearLayoutManager.VERTICAL,
                        false);

                snapHelper.attachToRecyclerView(mRecyclerView);

                mRecyclerView.setLayoutManager(linearLayoutManager);
                break;
            }
            default : Log.v("MainActivity", "Don't do it again");
        }
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
        adapter.swapCursor(cursor);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);

            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onClickSearch(View view) {
        final View mainSearchContainer = findViewById(R.id.main_search_container);
        mainSearchContainer.setVisibility(View.VISIBLE);

        final ImageButton imageButton = findViewById(R.id.search_close);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainSearchContainer.setVisibility(View.GONE);
            }
        });

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
                    editTextBorder.getLayoutParams().width = 16;
                } else {
                    Paint paint = new Paint();
                    paint.setTextSize(18);
                    paint.setTypeface(Typeface.MONOSPACE);

                    float nowWidth = paint.measureText(charSequence.toString());

                    editTextBorder.getLayoutParams().width = Math.max((int) nowWidth, 16);
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

            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if(inputMethodManager != null)
                inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);

            View mainSearchContainer = findViewById(R.id.main_search_container);
            mainSearchContainer.setVisibility(View.GONE);
            // Your piece of code on keyboard search click

            return true;
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

            int pad = getResources().getDimensionPixelSize(R.dimen.activity_margin_relative);

            activeFilter.textView.setBackgroundResource(0);
            activeFilter.textView = textView;
            activeFilter.textView.setBackgroundResource(R.drawable.border_view);
            activeFilter.textView.setPadding(pad, pad, pad, pad);
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
                int pad = getResources().getDimensionPixelSize(R.dimen.activity_margin_relative);

                filterTextView.setBackgroundResource(R.drawable.border_view);
                filterTextView.setPadding(pad, pad, pad, pad);

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
