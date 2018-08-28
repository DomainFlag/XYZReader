package com.example.xyzreader.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class ArticleDetailAdapter extends RecyclerView.Adapter<ArticleDetailAdapter.ArticleViewHolder> {

    private static final String TAG = "ArticleDetailAdapter";

    public static final int LAYOUT_VIEW_MULTI = 0;
    public static final int LAYOUT_VIEW_SINGLE = 1;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.US);
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private Context mContext;
    private Cursor mCursor;
    private int mLayoutViewMode = LAYOUT_VIEW_SINGLE;

    public ArticleDetailAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        switch(mLayoutViewMode) {
            case LAYOUT_VIEW_MULTI : {
                view = ((Activity) mContext).getLayoutInflater().inflate(R.layout.article_view_multi_layout, parent, false);
                break;
            }
            default : {
                view = ((Activity) mContext).getLayoutInflater().inflate(R.layout.article_view_single_layout, parent, false);
                break;
            }
        }

        return new ArticleViewHolder(view);
    }

    public class ArticleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView thumbnailView;
        private TextView titleView;
        private TextView subtitleView;
        private TextView articleLink;

        private ArticleViewHolder(View view) {
            super(view);

            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
            articleLink = view.findViewById(R.id.article_link);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(mLayoutViewMode != LAYOUT_VIEW_SINGLE) {
                int position = getAdapterPosition();
                onClickArticle(position);
            }
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);

            return dateFormat.parse(date);
        } catch(ParseException e) {
            Log.v(TAG, e.getMessage());
        }

        return new Date();
    }

    public void swapCursor(Cursor cursor) {
        mCursor = cursor;
    }

    public void changeLayoutViewMode(int layoutViewMode) {
        mLayoutViewMode = layoutViewMode;
    }

    public int getLayoutViewMode() {
        return mLayoutViewMode;
    }

    private void onClickArticle(int position) {
        mContext.startActivity(new Intent(Intent.ACTION_VIEW,
                ItemsContract.Items.buildItemUri(getItemId(position))));
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, final int position) {
        mCursor.moveToPosition(position);

        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

        Date publishedDate = parsePublishedDate();
        if(!publishedDate.before(START_OF_EPOCH.getTime())) {
            String date = DateUtils.getRelativeTimeSpanString(
                    publishedDate.getTime(),
                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL).toString();

            String subtitle = String.format("%1$s \r\nby %2$s", date,
                    mCursor.getString(ArticleLoader.Query.AUTHOR));

            holder.subtitleView.setText(subtitle);
        } else {
            String subtitle = String.format("%1$s \r\nby %2$s", outputFormat.format(publishedDate),
                    mCursor.getString(ArticleLoader.Query.AUTHOR));

            holder.subtitleView.setText(subtitle);
        }

        if(holder.articleLink != null) {
            holder.articleLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickArticle(position);
                }
            });
        }

        Picasso.get()
                .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                .into(holder.thumbnailView);
    }

    @Override
    public int getItemCount() {
        if(mCursor == null)
            return 0;
        else return mCursor.getCount();
    }
}
