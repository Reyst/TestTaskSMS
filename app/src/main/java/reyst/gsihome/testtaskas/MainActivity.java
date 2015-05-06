package reyst.gsihome.testtaskas;

import android.app.Activity;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends Activity implements View.OnClickListener {

    private static final Uri SMS_INBOX = Uri.parse("content://sms/inbox");
    private static final String REGEXP = ".*(apptest\\.com/i\\?id=)([a-zA-Z0-9]+)(.*)";
    private static final String WHERE_FOR_BODY_LIKE = "body like '%apptest.com/i?id=%'";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = (Button) findViewById(R.id.btnStart);

        btnStart.setOnClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

        new AsyncTask<Void, Void, String[]>() {

            @Override
            protected void onPostExecute(String[] result) {

                showResult(result);
            }

            @Override
            protected String[] doInBackground(Void... params) {

                Pattern pattern = Pattern.compile(REGEXP);

                List<String> resList = new LinkedList<>();

                Cursor c = getContentResolver().query(SMS_INBOX, new String[]{"body"}, WHERE_FOR_BODY_LIKE, null, null);

                if (c != null) {
                    Log.d("INBOX_SMS", String.valueOf(c.getCount()));
                    while (c.moveToNext()) {
                        Log.d("INBOX_SMS", c.getString(0));
                        Matcher matcher = pattern.matcher(c.getString(0));
                        while (matcher.find()) {
                            if (matcher.groupCount() > 2) {
                                if (!matcher.group(2).isEmpty()) {
                                    resList.add(getResultString(matcher.group(2)));
                                }
                            }
                        }
                    }
                    c.close();
                }

                return (resList.size() == 0) ? null : resList.toArray(new String[resList.size()]);
            }
        }.execute();

    }

    private String getResultString(String id) {
        String res;
        if (isConnected()) {

            try {
                URL url = new URL("http://app.mobilenobo.com/c/apptest?id=" + id);
                InputStream is = url.openStream();
                res = String.format("ID: %s, result: %s", id, convertStreamToString(is));
                is.close();
            } catch (IOException e) {
                res = String.format("ID: %s, result: %s", id, e.getLocalizedMessage());
            }
        } else {
            res = String.format("ID: %s, result: no i-net connection", id);
        }
        return res;
    }

    private void showResult(final String[] strings) {

        if (strings == null) {
            Toast.makeText(getApplicationContext(), "Not found!", Toast.LENGTH_SHORT).show();
        } else {
            for (String s : strings) {
                Toast.makeText(this, s, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
