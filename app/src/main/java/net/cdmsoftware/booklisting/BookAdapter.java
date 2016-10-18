package net.cdmsoftware.booklisting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class BookAdapter extends ArrayAdapter<Book> {
    private Context mContext;

    public BookAdapter(Context context, ArrayList<Book> books) {
        super(context, 0, books);
        this.mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if the existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.listitem, parent, false);
        }

        final Book currentBook = getItem(position);

        TextView titleView = (TextView) listItemView.findViewById(R.id.title);
        titleView.setText(currentBook.getTitle());

        TextView authorsView = (TextView) listItemView.findViewById(R.id.authors);
        if (currentBook.getAuthors().equals("")) {
            authorsView.setVisibility(View.GONE);
        } else {
            authorsView.setText(currentBook.getAuthors());
            authorsView.setVisibility(View.VISIBLE);
        }

        TextView publisherView = (TextView) listItemView.findViewById(R.id.publisher);
        if (currentBook.getPublisher().equals("")) {
            publisherView.setVisibility(View.GONE);
        } else {
            publisherView.setText(currentBook.getPublisher());
            publisherView.setVisibility(View.VISIBLE);
        }

        // load image from URL
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.coverImage = currentBook.getCoverImage();
        viewHolder.coverImageView = (ImageView) listItemView.findViewById(R.id.coverImage);
        viewHolder.coverImageView.setVisibility(View.INVISIBLE);
        new ImageLoaderClass().execute(viewHolder);

        return listItemView;
    }

    private static class ViewHolder {
        String coverImage;
        ImageView coverImageView;
    }

    private class ImageLoaderClass extends AsyncTask<ViewHolder, String, Bitmap> {
        ViewHolder viewHolder;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        protected Bitmap doInBackground(ViewHolder... args) {
            Bitmap imageBitmap = null;
            viewHolder = args[0];
            try {
                imageBitmap = BitmapFactory.decodeStream((InputStream) new URL(viewHolder.coverImage).getContent());

            } catch (Exception e) {
                e.printStackTrace();
            }
            return imageBitmap;
        }

        protected void onPostExecute(Bitmap image) {
            if (image != null) {
                Drawable drawable = new BitmapDrawable(mContext.getResources(), image);
                viewHolder.coverImageView.setImageDrawable(drawable);
                viewHolder.coverImageView.setVisibility(View.VISIBLE);
            }
        }
    }
}
