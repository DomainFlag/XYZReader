package com.example.xyzreader.activities;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.squareup.picasso.Picasso;

public class ArticleActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private long mStartId;

    private NestedScrollView articleScrollContent;

    private AppBarLayout appBarLayout;
    private boolean toolbarStateCollapsed = false;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
            }
        }

        if(getSupportActionBar() != null)
            getSupportActionBar().hide();

        findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, "Some sample text");

                startActivity(Intent.createChooser(intent, getString(R.string.action_share)));
            }
        });

        getLoaderManager().initLoader(0, null, this);

        appBarLayout = findViewById(R.id.app_bar_layout);
        articleScrollContent = findViewById(R.id.article_content);
        articleScrollContent.setNestedScrollingEnabled(true);

        findViewById(R.id.article_info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appBarLayout.setExpanded(toolbarStateCollapsed, true);
            }
        });

        appBarLayout.setElevation(getResources().getDimension(R.dimen.activity_app_bar));
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if(Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    toolbarStateCollapsed = true;
                    appBarLayout.setElevation(0);
                } else if(verticalOffset == 0) {
                    toolbarStateCollapsed = false;
                    appBarLayout.setElevation(0);
                } else {
                    appBarLayout.setElevation(getResources().getDimension(R.dimen.activity_app_bar));
                }
            }
        });

        findViewById(R.id.action_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSupportNavigateUp();
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(this, mStartId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;

        if(mCursor.getCount() == 1) {
            mCursor.moveToFirst();

            TextView articleInfo = findViewById(R.id.article_info);
            articleInfo.setText(mCursor.getString(ArticleLoader.Query.BODY));

            ImageView imageView = findViewById(R.id.article_caption);
            Picasso.get()
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .into(imageView);

            ((TextView) findViewById(R.id.article_title)).setText(mCursor.getString(ArticleLoader.Query.TITLE));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
    }
}
