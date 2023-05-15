package com.example.gps;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.graphics.Color;
import com.google.android.gms.maps.model.PolylineOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import org.json.*;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import java.net.HttpURLConnection;
import java.net.URL;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import java.io.InputStream;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.BufferedReader;

import java.io.InputStreamReader;




public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap myMap;
    private Location mLastLocation;
    private Marker mMarker;

    private LocationManager locationManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }

        Button displayRouteButton = findViewById(R.id.btn_display_route);
        displayRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayRoute();
            }
        });
    }



    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        myMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            // Activer la localisation
            myMap.setMyLocationEnabled(true);
            myMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location location) {
                    // Mettre à jour la caméra de la carte pour suivre la localisation de l'utilisateur
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                    mLastLocation = location;
                }

            });
        }

        LatLng casablanca = new LatLng(33.48025259367308, -7.6189140722597175);
        BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        mMarker = myMap.addMarker(new MarkerOptions()
                .position(casablanca)
                .title("Casablanca")
                .icon(icon));

        myMap.moveCamera(CameraUpdateFactory.newLatLng(casablanca));

    }
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
        LatLng destination = mMarker.getPosition();
        String url = getDirectionsUrl(origin, destination);
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);
    }

    private void displayRoute() {
        LatLng origin = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        LatLng destination = mMarker.getPosition();
        String url = getDirectionsUrl(origin, destination);
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);
    }
    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // Set the origin and destination coordinates
        String strOrigin = "origin=" + origin.latitude + "," + origin.longitude;
        String strDest = "destination=" + dest.latitude + "," + dest.longitude;

        // Set the API key and travel mode
        String key = "AIzaSyAAAAfxYOvrxgyD7XLaa1pzgv1Hqe8pLQQ";
        String mode = "mode=driving";

        // Build the URL
        String url = "https://maps.googleapis.com/maps/api/directions/json?" + strOrigin + "&" + strDest + "&" + mode + "&key=" + key;

        return url;
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                URL urlObj = new URL(url[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) urlObj.openConnection();
                httpURLConnection.connect();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                data = stringBuilder.toString();
                bufferedReader.close();
                inputStream.close();
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>>> {
        @Override
        protected List<List<HashMap<String,String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
            ArrayList<LatLng> points = new ArrayList<>();
            PolylineOptions polylineOptions = new PolylineOptions();
            for (List<HashMap<String, String>> path : routes) {
                for (HashMap<String, String> point : path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                polylineOptions.addAll(points);
                polylineOptions.width(10);
                polylineOptions.color(Color.BLUE);
                polylineOptions.geodesic(true);
            }
            myMap.addPolyline(polylineOptions);
        }

    }
    public class DirectionsJSONParser {
        public List<List<HashMap<String,String>>> parse(JSONObject jObject){

            List<List<HashMap<String, String>>> routes = new ArrayList<>() ;
            JSONArray jRoutes;
            JSONArray jLegs;
            JSONArray jSteps;

            try {
                jRoutes = jObject.getJSONArray("routes");

                // Traversing all routes
                for(int i=0;i<jRoutes.length();i++){
                    jLegs = ((JSONObject)jRoutes.get(i)).getJSONArray("legs");

                    List path = new ArrayList<HashMap<String, String>>();

                    // Traversing all legs
                    for(int j=0;j<jLegs.length();j++){
                        jSteps = ((JSONObject)jLegs.get(j)).getJSONArray("steps");

                        // Traversing all steps
                        for(int k=0;k<jSteps.length();k++){
                            String polyline;
                            polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            // Traversing all points
                            for(int l=0;l<list.size();l++){
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString(list.get(l).latitude));
                                hm.put("lng", Double.toString(list.get(l).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e){
            }
            return routes;
        }

        /**
         * Method to decode polyline points
         */
        private List<LatLng> decodePoly(String encoded) {

            List<LatLng> poly = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
                poly.add(p);
            }

            return poly;
        }
    }
}


