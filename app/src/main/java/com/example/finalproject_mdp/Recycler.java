package com.example.finalproject_mdp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.JsonReader;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import java.util.ArrayList;
import java.util.List;

public class Recycler extends AppCompatActivity implements adapterdatos.OnRecyclerListener {

    private static final String TAG = "RecycleView";
    private Button btn_mqtt;
    private Button next_graph;
    private TextView tv_name;

    private ArrayList<String> listDatos = new ArrayList<String>(); //lista igual a la anterior
    private ArrayList<String> listSchedules = new ArrayList<String>(); //lista igual a la anterior
    private ArrayList<LocationJSON> listLoc = new ArrayList<LocationJSON>();
    private RecyclerView recycler;
    private RecyclerView.RecyclerListener listener;
 //   private Button list_view_centers;
    private Boolean centersListed = false;
    String soccer;
    String tennis;
    String soccer_tennis;
    private Handler mainHandler;

    {
        mainHandler = new Handler();
    }

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_ly);
        btn_mqtt = findViewById(R.id.btn_mqtt);
        next_graph = findViewById(R.id.btn_next_graph);
        tv_name = findViewById(R.id.tv_hello);
        recycler = findViewById(R.id.recyclerId);
      //  list_view_centers = findViewById(R.id.btn_list);


        String name = getIntent().getStringExtra("name");
        soccer = getIntent().getStringExtra("soccer");
        tennis = getIntent().getStringExtra("tennis");
        soccer_tennis = getIntent().getStringExtra("soccer_tennis");

        tv_name.setText("Hello " + name);

        new fetchData().start();
        centersListed = true;


//  call the constructor of CustomAdapter to send the reference and data to Adapter
        adapterdatos customAdapter = new adapterdatos(Recycler.this, listDatos, listSchedules, this::onRecyclerClick);
        recycler.setAdapter(customAdapter); // set the Adapter to RecyclerView

        //TOPICS ARE SENDING TO MQTT
        btn_mqtt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i_mqtt = new Intent(Recycler.this,mqtt.class);

                i_mqtt.putExtra("soccer_tennis",soccer_tennis);
                i_mqtt.putExtra("soccer",soccer);
                i_mqtt.putExtra("tennis",tennis);
                i_mqtt.putExtra("name",name);
                startActivity(i_mqtt);
            }
        });

        next_graph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i_graph = new Intent(Recycler.this,Graph_sensor.class);
                startActivity(i_graph);
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onStart() {
        super.onStart();


    }
    @Override
    public void onRecyclerClick(int position) {
        //listLocation.get(position).toString();
        LocationJSON l = listLoc.get(position);

        Intent intent = new Intent(this, MapsSportCenters.class);
        intent.putExtra("lat",l.getLatitude());
        intent.putExtra("long",l.getLongitude());
        intent.putExtra("tennis",tennis);
        intent.putExtra("soccer",soccer);
        intent.putExtra("soccer_tennis",soccer_tennis);
        startActivity(intent);
        Log.d(TAG,listLoc.get(position).toString());
        Log.d(TAG,"OnRecycle click: clicked");
    }
    class LocationJSON{
        double latitude;
        double longitude;
        public LocationJSON(double latitude, double longitude){
            this.latitude = latitude;
            this.longitude = longitude;

        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        @Override
        public String toString() {
            return "Location{" +
                    "latitude=" + latitude +
                    ", longitude=" + longitude +
                    '}';
        }
    }

    //THREAD TO DOWNLOAD AND PARSER JSON FILE
    class fetchData extends Thread{
        //WE SAVE THE JSON FILE IN DATA VARIABLE
        String data = "";
        public void run(){

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog = new ProgressDialog(Recycler.this);
                    progressDialog.setMessage("Searching Data");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                }
            });
            try {
                //OPEN CONNECTION
                URL url = new URL("https://datos.madrid.es/egob/catalogo/200186-0-polideportivos.json");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                //BUFFER
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = bufferedReader.readLine())!=null){
                    data = data + line;
                }

                if(data != null){
                    JSONObject jsonObject = new JSONObject(data);
                    JSONArray title_centers = jsonObject.getJSONArray("@graph");
                    //clear data if the user press button two times
                    listDatos.clear();
                    listSchedules.clear();
                    listLoc.clear();
                    //JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
                    //readLoc(reader);
                    for (int i = 0;i<title_centers.length();i++){
                        //PARSER NAMES OF CENTERS
                        JSONObject titles = title_centers.getJSONObject(i);
                        String titles_centers = titles.getString("title");
                        listDatos.add(titles_centers);
                        //PARSER SCHEDULE OF EACH CENTER
                        JSONObject organization = titles.getJSONObject("organization");
                        String schedule = organization.getString("schedule");
                        listSchedules.add(schedule);
                        JSONObject location = titles.getJSONObject("location");
                        double latitude = location.getDouble("latitude");
                        double longitude = location.getDouble("longitude");
                        LocationJSON l = new LocationJSON(latitude,longitude);
                        listLoc.add(l);
                    }

                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //progress bar dissapear and in that moment we show the recycler view
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(progressDialog.isShowing()){
                        progressDialog.dismiss();
                        recycler.setLayoutManager(new LinearLayoutManager(Recycler.this,LinearLayoutManager.VERTICAL,false));

                    }
                }
            });

        }
    }



}