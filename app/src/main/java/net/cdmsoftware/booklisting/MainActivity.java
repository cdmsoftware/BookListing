package net.cdmsoftware.booklisting;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String API_BASE_URL =
            "https://www.googleapis.com/books/v1/volumes";
    private static final String QUERY_PARAM = "q";
    private static final String MAX_RESULT_PARAM = "maxResults";
    private BookAdapter bookAdapter;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progress = (ProgressBar) findViewById(R.id.progress);

        ArrayList<Book> books = new ArrayList<>();
        bookAdapter = new BookAdapter(this, books);

        // load data from URL
        if (!hasInternetConnection()){
            Toast.makeText(MainActivity.this,R.string.no_internet,Toast.LENGTH_LONG).show();
        }else {
            new BookAsyncTask(progress).execute("android");
        }

        ListView bookListView = (ListView) findViewById(R.id.list);
        bookListView.setAdapter(bookAdapter);

        bookListView.setEmptyView(findViewById(R.id.no_data));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!hasInternetConnection()){
                    Toast.makeText(MainActivity.this, R.string.no_internet,Toast.LENGTH_LONG).show();
                    return false;
                }else {
                    new BookAsyncTask(progress).execute(query);
                    return true;
                }
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private boolean hasInternetConnection(){
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void updateUi(ArrayList<Book> books) {
        bookAdapter.clear();
        bookAdapter.addAll(books);
    }

    private class BookAsyncTask extends AsyncTask<String, Void, ArrayList<Book>> {
        private final ProgressBar progress;

        public BookAsyncTask(final ProgressBar progress) {
            this.progress = progress;
        }

        @Override
        protected ArrayList<Book> doInBackground(String... keywords) {
            // Create URL object
            Uri builtUri = Uri.parse(API_BASE_URL)
                    .buildUpon()
                    .appendQueryParameter(QUERY_PARAM, keywords[0])
                    .appendQueryParameter(MAX_RESULT_PARAM, Integer.toString(10))
                    .build();
            URL url = createUrl(builtUri.toString());

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                if (url != null) {
                    jsonResponse = makeHttpRequest(url);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "HTTP request error", e);
            }

            return extractBookFromJson(jsonResponse);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(ArrayList<Book> books) {
            updateUi(books);
            progress.setVisibility(View.GONE);
        }

        private URL createUrl(String stringUrl) {
            URL url;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }

        private String makeHttpRequest(URL url) throws IOException {
            if (url == null) return "";

            String jsonResponse = "";
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();
                if (urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                } else {
                    Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving JSON result", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        private ArrayList<Book> extractBookFromJson(String bookJSON) {
            if (TextUtils.isEmpty(bookJSON)) return null;

            try {
                ArrayList<Book> books = new ArrayList<>();

                JSONObject baseJsonResponse = new JSONObject(bookJSON);

                if (baseJsonResponse.isNull("items")) return null;
                JSONArray itemsArray = baseJsonResponse.getJSONArray("items");

                //loop through book items
                for (int i = 0; i < itemsArray.length(); i++) {
                    // get book item
                    JSONObject item = itemsArray.getJSONObject(i);
                    JSONObject volumeInfo = item.getJSONObject("volumeInfo");

                    // Extract out the title, authors, publisher, cover image values
                    String title = volumeInfo.getString("title");
                    String authors = "";
                    if (!volumeInfo.isNull("authors")) {
                        authors = volumeInfo.getJSONArray("authors").join(", ").replaceAll("\"", "");
                    }
                    String publisher = "";
                    if (!volumeInfo.isNull("publisher")) {
                        publisher = volumeInfo.getString("publisher");
                    }
                    String coverImage = "";
                    if (!volumeInfo.isNull("imageLinks")) {
                        JSONObject imageLinks = volumeInfo.getJSONObject("imageLinks");
                        coverImage = imageLinks.getString("thumbnail");
                    }

                    books.add(new Book(title, authors, publisher, coverImage));
                }
                return books;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing JSON results", e);
            }
            return null;
        }
    }
}
