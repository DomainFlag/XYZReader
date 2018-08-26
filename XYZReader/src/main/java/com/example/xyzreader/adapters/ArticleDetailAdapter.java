package com.example.xyzreader.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.example.xyzreader.R;
import com.example.xyzreader.activities.MainActivity;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class ArticleDetailAdapter extends RecyclerView.Adapter<MainActivity.ViewHolder> {

    private static final String TAG = "ArticleDetailAdapter";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss", Locale.US);
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private Context mContext;

    private Cursor mCursor;

    public ArticleDetailAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public MainActivity.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = ((Activity) mContext).getLayoutInflater().inflate(R.layout.article_layout, parent, false);
        final MainActivity.ViewHolder viewHolder = new MainActivity.ViewHolder(view);

        View detailedArticle = view.findViewById(R.id.detailed_article);
        detailedArticle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContext.startActivity(new Intent(Intent.ACTION_VIEW,
                        ItemsContract.Items.buildItemUri(getItemId(viewHolder.getAdapterPosition()))));
            }
        });

        return viewHolder;
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

    @Override
    public void onBindViewHolder(MainActivity.ViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
        Date publishedDate = parsePublishedDate();
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            holder.subtitleView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
        } else {
            holder.subtitleView.setText(Html.fromHtml(
                    outputFormat.format(publishedDate)
                            + "<br/>" + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)));
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
