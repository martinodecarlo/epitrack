package com.martinodecarlo.epitrack;


import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.iid.InstanceID;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class Utils {
    public static final int MULTIPLE_PERMISSIONS = 100;


    public static class loadUrl extends AsyncTask<String, String, String> {

        HttpURLConnection conn;
        URL url = null;
        ProgressDialog pdLoading;
        String confirmingcode;


        //this method will interact with UI, here display loading message
        @Override
        protected void onPreExecute() {
            super.onPreExecute();


        }

        // This method does not interact with UI, You need to pass result to onPostExecute to display
        @Override
        protected String doInBackground(String... params) {


            try {

                // Setup HttpURLConnection class to send and receive data from php
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(10000);
                conn.setRequestMethod("GET");

                // setDoOutput to true as we recieve data from json file
                conn.setDoOutput(true);

            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return e1.toString();
            }

            try {

                int response_code = conn.getResponseCode();

                // Check if successful connection made
                if (response_code == HttpURLConnection.HTTP_OK) {

                    // Read data sent from server
                    InputStream input = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    // Pass data to onPostExecute method
                    return (result.toString());

                } else {

                    return ("unsuccessful");
                }

            } catch (IOException e) {
                e.printStackTrace();
                return e.toString();
            } finally {
                conn.disconnect();
            }
        }

        // this method will interact with UI, display result sent from doInBackground method
        @Override
        protected void onPostExecute(String result) {
            Log.d("THISS","QUII"+result);
        }

    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static String getLastDate(Context mContext){
        String toBeReturned="";
        final String fileName = mContext.getExternalCacheDir().getAbsolutePath() + "/"+MainActivity.lastDate;


//Get the text file
        File file = new File(fileName);

//Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        toBeReturned=text.toString();
        if(toBeReturned.equals(""))
            toBeReturned="";
        Log.d("TAG",toBeReturned);
        return toBeReturned;
    }


    public static void addContact(String str, Context mContext) {


        final String fileName = mContext.getExternalCacheDir().getAbsolutePath() + "/"+MainActivity.fileContacts;

        try {

            // Open given file in append mode.
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(fileName, true));
            out.write(str+";");
            out.close();
        } catch (IOException e) {
            System.out.println("exception occoured" + e);
        }
    }





    public static String readContacts(Context mContext){

        final String fileName = mContext.getExternalCacheDir().getAbsolutePath() + "/"+MainActivity.fileContacts;

//Get the text file
        File file = new File(fileName);

//Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append(';');
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }


        return text.toString();
    }



    public static void saveLastDate(String str, Context mContext) {


        final String fileName = mContext.getExternalCacheDir().getAbsolutePath() + "/"+MainActivity.lastDate;

        try {

            // Open given file in append mode.
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(fileName, false));
            out.write(str);
            out.close();
        } catch (IOException e) {
            System.out.println("exception occoured" + e);
        }
    }




    public static String getUniqueKey(Context mContext){

        SharedPreferences prefs = mContext.getSharedPreferences(mContext.getString(R.string.prefName), Context.MODE_PRIVATE);
        String myUserUID = prefs.getString(mContext.getString(R.string.UID), "");

        if(myUserUID.equals(""))
        {
            myUserUID = InstanceID.getInstance(mContext).getId();
            prefs.edit().putString(mContext.getString(R.string.UID),myUserUID).commit();
        }



        return myUserUID;
    }




}
