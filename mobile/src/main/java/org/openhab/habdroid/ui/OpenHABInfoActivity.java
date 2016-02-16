/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *  @author Victor Belov
 *  @since 1.4.0
 *
 */

package org.openhab.habdroid.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.openhab.habdroid.R;
import org.openhab.habdroid.util.MyAsyncHttpClient;
import org.openhab.habdroid.util.Util;

import java.util.HashMap;
import java.util.Map;

public class OpenHABInfoActivity extends Activity {

    private static final String TAG = "OpenHABInfoActivity";
    private TextView mOpenHABVersionText;
    private TextView mOpenHABUUIDText;
    private TextView mOpenHABSecretText;
    private TextView mOpenHABSecretLabel;
    private String mOpenHABBaseUrl;
    private String mUsername;
    private String mPassword;
    private static MyAsyncHttpClient mAsyncHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        Util.setActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.openhabinfo);
        mAsyncHttpClient = MyAsyncHttpClient.getInstance(this);
        mOpenHABVersionText = (TextView)findViewById(R.id.openhab_version);
        mOpenHABUUIDText = (TextView)findViewById(R.id.openhab_uuid);
        mOpenHABSecretText = (TextView)findViewById(R.id.openhab_secret);
        mOpenHABSecretLabel = (TextView)findViewById(R.id.openhab_secret_label);
        if (getIntent().hasExtra("openHABBaseUrl")) {
            mOpenHABBaseUrl = getIntent().getStringExtra("openHABBaseUrl");
            mUsername = getIntent().getStringExtra("username");
            mPassword = getIntent().getStringExtra("password");
            //mAsyncHttpClient.setBasicAuth(mUsername, mPassword);
        } else {
            Log.e(TAG, "No openHABBaseURl parameter passed, can't fetch openHAB info from nowhere");
            finish();
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        {
            Log.d(TAG, "url = " + mOpenHABBaseUrl + "static/version");
            String url = mOpenHABBaseUrl + "static/version";
            StringRequest request = new StringRequest
                    (Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d(TAG, "Got version = " + response);
                                    mOpenHABVersionText.setText(response);
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    mOpenHABVersionText.setText("Unknown");
                                    if (error.getMessage() != null) {
                                        Log.e(TAG, error.getMessage());
                                    }
                                }
                            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "text/plain; charset=utf-8");
                    headers.put("User-agent", "My useragent");
                    return headers;
                }
            };
            MyAsyncHttpClient.getInstance(this).addToRequestQueue(request);
        }
        {
            String url = mOpenHABBaseUrl + "static/uuid";
            StringRequest request = new StringRequest
                    (Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d(TAG, "Got uuid = " + response);
                                    mOpenHABUUIDText.setText(response);
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    mOpenHABUUIDText.setText("Unknown");
                                    if (error.getMessage() != null) {
                                        Log.e(TAG, error.getMessage());
                                    }
                                }
                            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "text/plain; charset=utf-8");
                    headers.put("User-agent", "My useragent");
                    return headers;
                }
            };
            MyAsyncHttpClient.getInstance(this).addToRequestQueue(request);
        }
        {
            String url = mOpenHABBaseUrl + "static/secret";
            StringRequest request = new StringRequest
                    (Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d(TAG, "Got secret = " + response);
                                    mOpenHABSecretText.setVisibility(View.VISIBLE);
                                    mOpenHABSecretLabel.setVisibility(View.VISIBLE);
                                    mOpenHABSecretText.setText(response);
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    mOpenHABSecretText.setVisibility(View.GONE);
                                    mOpenHABSecretLabel.setVisibility(View.GONE);
                                    if (error.getMessage() != null) {
                                        Log.e(TAG, error.getMessage());
                                    }
                                }
                            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "text/plain; charset=utf-8");
                    headers.put("User-agent", "My useragent");
                    return headers;
                }
            };
            MyAsyncHttpClient.getInstance(this).addToRequestQueue(request);
        }
    }

        @Override
    public void finish() {
        super.finish();
        Util.overridePendingTransition(this, true);
    }
}
