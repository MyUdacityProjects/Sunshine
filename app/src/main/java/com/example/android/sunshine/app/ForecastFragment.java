package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class ForecastFragment extends Fragment {

    ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu);
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

        if (id == R.id.action_refresh) {
            new FetchWeatherTask().execute("94043");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHER - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };


        List<String> weekForecast = new ArrayList<>(Arrays.asList(data));
        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                weekForecast);
        ListView weatherListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        weatherListView.setAdapter(mForecastAdapter);

        weatherListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String weatherForecast = mForecastAdapter.getItem(position);
                Toast.makeText(getActivity(),weatherForecast,Toast.LENGTH_SHORT).show();
            }
        });
        return rootView;
    }

    private String getReadableDateString(Date day) {
        
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(day);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        String highAndLow = "";
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        highAndLow = roundedHigh + "/" + roundedLow;
        return highAndLow;
    }

    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException {
        String[] forecastString = new String[numDays];

        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_DESC = "main";
        final String OWM_TEMP = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        JSONObject forecastJSON = new JSONObject(forecastJsonStr);
        JSONArray forecastList = forecastJSON.getJSONArray(OWM_LIST);

        GregorianCalendar gc = new GregorianCalendar();

        for (int i = 0; i < numDays; ++i) {
            JSONObject dayForecast = forecastList.getJSONObject(i);
            Double minTemp = 0d, maxTemp = 0d;
            String desc = "", highAndLow = "", day = "";
            gc.add(GregorianCalendar.DATE, 1);
            Date time = gc.getTime();
            if (dayForecast.has(OWM_TEMP)) {
                JSONObject dayTempDetails = dayForecast.getJSONObject(OWM_TEMP);
                if (dayTempDetails.has(OWM_MIN)) {
                    minTemp = dayTempDetails.getDouble(OWM_MIN);
                }
                if (dayTempDetails.has(OWM_MAX)) {
                    maxTemp = dayTempDetails.getDouble(OWM_MAX);
                }
            }
            if (dayForecast.has(OWM_WEATHER)) {
                JSONArray dayWeatherList = dayForecast.getJSONArray(OWM_WEATHER);
                JSONObject dayWeatherDetails = dayWeatherList.getJSONObject(0);
                if (dayWeatherDetails.has(OWM_DESC)) {
                    desc = dayWeatherDetails.getString(OWM_DESC);
                }
            }
            highAndLow = formatHighLows(maxTemp, minTemp);
            day = getReadableDateString(time);
            forecastString[i] = day + " - " + desc + " - " + highAndLow;
        }



        return forecastString;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String[] forecastString = null;

            String units = "metric";
            Integer numDays = 7;
            String mode = "json";

            try {

                final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String ZIP_QUERY_PARAM = "zip";
                final String APPID_QUERY_PARAM = "appid";
                final String UNITS_QUERY_PARAM = "units";
                final String CNT_QUERY_PARAM = "cnt";
                final String MODE_QUERY_PARAM = "mode";

                Uri uri = Uri.parse(BASE_URL).buildUpon().
                        appendQueryParameter(ZIP_QUERY_PARAM, params[0]).
                        appendQueryParameter(APPID_QUERY_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY).
                        appendQueryParameter(UNITS_QUERY_PARAM, units).
                        appendQueryParameter(CNT_QUERY_PARAM, Integer.toString(numDays)).
                        appendQueryParameter(MODE_QUERY_PARAM, mode).build();

                String weatherURL = uri.toString();
                URL url = new URL(weatherURL);
                // Create the request to OpenWeatherMap, and open the connection
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();


            } catch (Exception e) {
                Log.e("FORECAST FRAGMENT", "Error ", e);
                return forecastString;

            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (final IOException e) {
                        Log.e("FORECAST FRAGMENT", "Error closing stream", e);
                        return forecastString;
                    }
                }
                try {
                    forecastString = getWeatherDataFromJson(forecastJsonStr, numDays);
                } catch (Exception e) {
                    Log.e("FORECAST FRAGMENT", "Error parsing JSON", e);
                }
                return forecastString;
            }

        }

        @Override
        protected void onPostExecute(String[] forecastString) {
            super.onPostExecute(forecastString);
            mForecastAdapter.clear();
            for(String s: forecastString){
                mForecastAdapter.add(s);
            }
        }
    }
}
