package org.openhab.habdroid.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;





import org.openhab.habdroid.R;

import org.openhab.habdroid.model.OpenHABBinding;
import org.openhab.habdroid.util.MyAsyncHttpClient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class OpenHABDiscoveryFragment extends ListFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "DiscoveryFragment";

    private static final String ARG_USERNAME = "openHABUsername";
    private static final String ARG_PASSWORD = "openHABPassword";
    private static final String ARG_BASEURL = "openHABBaseUrl";

    private String openHABUsername = "";
    private String openHABPassword = "";
    private String openHABBaseUrl = "";

    private OpenHABMainActivity mActivity;
    // loopj
    private MyAsyncHttpClient mAsyncHttpClient;
    // keeps track of current request to cancel it in onPause
    private Request mRequestHandle;

    private OpenHABDiscoveryAdapter mDiscoveryAdapter;
    private ArrayList<OpenHABBinding> bindings;
    private ArrayList<String> discoveries;
    private ArrayList<OpenHABBinding> discoverableBindings;

    private SwipeRefreshLayout mSwipeLayout;

    private int selectedInbox;

    private Timer discoveryTimer;

    public static OpenHABDiscoveryFragment newInstance(String baseUrl, String username, String password) {
        OpenHABDiscoveryFragment fragment = new OpenHABDiscoveryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_PASSWORD, password);
        args.putString(ARG_BASEURL, baseUrl);
        fragment.setArguments(args);
        return fragment;
    }

    public OpenHABDiscoveryFragment() {
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        bindings = new ArrayList<OpenHABBinding>();
        discoverableBindings = new ArrayList<OpenHABBinding>();
        discoveries = new ArrayList<String>();
        if (getArguments() != null) {
            openHABUsername = getArguments().getString(ARG_USERNAME);
            openHABPassword = getArguments().getString(ARG_PASSWORD);
            openHABBaseUrl = getArguments().getString(ARG_BASEURL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.i(TAG, "onCreateView");
        Log.d(TAG, "isAdded = " + isAdded());
        View view = inflater.inflate(R.layout.openhabdiscoverylist_fragment, container, false);
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);
        return view;
    }


    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach()");
        try {
            mActivity = (OpenHABMainActivity) activity;
            mAsyncHttpClient = mActivity.getAsyncHttpClient();
            mActivity.setTitle(R.string.app_discovery);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must be OpenHABMainActivity");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDiscoveryAdapter = new OpenHABDiscoveryAdapter(this.getActivity(), R.layout.openhabdiscoverylist_item, discoverableBindings);
        getListView().setAdapter(mDiscoveryAdapter);
//        getListView().setEmptyView(getActivity().findViewById(R.id.empty_inbox_view));
        Log.d(TAG, "onActivityCreated()");
        Log.d(TAG, "isAdded = " + isAdded());
    }

    @Override
    public void onResume () {
        super.onResume();
        Log.d(TAG, "onResume()");
        loadDiscovery();
        loadBindings();
    }

    @Override
    public void onPause () {
        super.onPause();
        Log.d(TAG, "onPause()");
        // Cancel request for notifications if there was any
        if (mRequestHandle != null) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mRequestHandle.cancel();
                }
            });
            thread.start();
        }
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "onRefresh()");
        refresh();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach()");
        mActivity = null;
    }

    public void refresh() {
        Log.d(TAG, "refresh()");
        loadDiscovery();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        activateDiscovery(discoverableBindings.get(position).getId());
    }

    private void loadDiscovery() {
        if (mAsyncHttpClient != null) {
            startProgressIndicator();
            String url = openHABBaseUrl + "rest/discovery";
            StringRequest request = new StringRequest
                    (Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    stopProgressIndicator();
                                    String jsonString = null;
                                    jsonString = response;
                                    Log.d(TAG, "Discovery request success");
                                    Log.d(TAG, jsonString);
                                    GsonBuilder gsonBuilder = new GsonBuilder();
                                    Gson gson = gsonBuilder.create();
                                    discoveries.clear();
                                    discoveries.addAll(Arrays.asList(gson.fromJson(jsonString, String[].class)));
                                    updateDiscoverableBindings();
                                }},
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    stopProgressIndicator();
                                    Log.d(TAG, "Discovery request failure: " + error.getMessage());
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
            mRequestHandle = request;
            MyAsyncHttpClient.getInstance(getActivity()).addToRequestQueue(request);
        }
    }

    private void loadBindings() {
        if (mAsyncHttpClient != null) {
            startProgressIndicator();
            String url = openHABBaseUrl + "rest/bindings";
            StringRequest request = new StringRequest
                    (Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    stopProgressIndicator();
                                    String jsonString = null;
                                    jsonString = response;
                                    Log.d(TAG, "Bindings request success");
                                    Log.d(TAG, jsonString);
                                    GsonBuilder gsonBuilder = new GsonBuilder();
                                    Gson gson = gsonBuilder.create();
                                    bindings.clear();
                                    bindings.addAll(Arrays.asList(gson.fromJson(jsonString, OpenHABBinding[].class)));
                                    updateDiscoverableBindings();
                                }},
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    stopProgressIndicator();
                                    Log.d(TAG, "Discovery request failure: " + error.getMessage());
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
            mRequestHandle = request;
            MyAsyncHttpClient.getInstance(getActivity()).addToRequestQueue(request);
        }
    }

    private void updateDiscoverableBindings() {
        discoverableBindings.clear();
        for (OpenHABBinding binding : bindings) {
            Log.d(TAG, "Checking " + binding.getId());
            if (discoveries.contains(binding.getId())) {
                Log.d(TAG, binding.getName() + " is discoverable");
                discoverableBindings.add(binding);
            } else {
                Log.d(TAG, binding.getName() + " is not discoverable");
            }
        }
        if (mDiscoveryAdapter != null) {
            mDiscoveryAdapter.notifyDataSetChanged();
        }
    }

    private void activateDiscovery(String id) {
        if (mAsyncHttpClient != null) {
            startProgressIndicator();
            String url = openHABBaseUrl + "rest/discovery/bindings/" + id + "/scan";
            StringRequest request = new StringRequest
                    (Request.Method.POST, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d(TAG, "Activate discovery request success");
                                    if (discoveryTimer != null) {
                                        discoveryTimer.cancel();
                                        discoveryTimer.purge();
                                        discoveryTimer = null;
                                    }
                                    discoveryTimer = new Timer();
                                    discoveryTimer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            Log.d(TAG, "Discovery timer ended");
                                            if (getActivity() != null) {
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        stopProgressIndicator();
                                                        if (mActivity != null) {
                                                            mActivity.openDiscoveryInbox();
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    }, 10000);
                                }},
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    stopProgressIndicator();
                                    Log.e(TAG, "Activate discovery request error: " + error.getMessage());
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
            mRequestHandle = request;
            MyAsyncHttpClient.getInstance(getActivity()).addToRequestQueue(request);
        }
    }

    private void stopProgressIndicator() {
        if (mActivity != null)
            Log.d(TAG, "Stop progress indicator");
        mActivity.stopProgressIndicator();
    }

    private void startProgressIndicator() {
        if (mActivity != null)
            Log.d(TAG, "Start progress indicator");
        mActivity.startProgressIndicator();
        mSwipeLayout.setRefreshing(false);
    }

}
