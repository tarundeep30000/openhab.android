package org.openhab.habdroid.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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



import com.software.shell.fab.ActionButton;


import org.openhab.habdroid.R;

import org.openhab.habdroid.model.OpenHABDiscoveryInbox;
import org.openhab.habdroid.model.thing.ThingType;
import org.openhab.habdroid.util.MyAsyncHttpClient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OpenHABDiscoveryInboxFragment extends ListFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "DiscoveryInboxFragment";

    private static final String ARG_USERNAME = "openHABUsername";
    private static final String ARG_PASSWORD = "openHABPassword";
    private static final String ARG_BASEURL = "openHABBaseUrl";

    private String openHABUsername = "";
    private String openHABPassword = "";
    private String openHABBaseUrl = "";

    private OpenHABMainActivity mActivity;

    private MyAsyncHttpClient mAsyncHttpClient;
    // keeps track of current request to cancel it in onPause
    private Request mRequestHandle;

    private OpenHABDiscoveryInboxAdapter mDiscoveryInboxAdapter;
    private ArrayList<OpenHABDiscoveryInbox> mDiscoveryInbox;
    private ArrayList<ThingType> mThingTypes;

    private SwipeRefreshLayout mSwipeLayout;

    private int selectedInbox;

    private ActionButton discoveryButton;

    public static OpenHABDiscoveryInboxFragment newInstance(String baseUrl, String username, String password) {
        OpenHABDiscoveryInboxFragment fragment = new OpenHABDiscoveryInboxFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_PASSWORD, password);
        args.putString(ARG_BASEURL, baseUrl);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public OpenHABDiscoveryInboxFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        mDiscoveryInbox = new ArrayList<OpenHABDiscoveryInbox>();
        mThingTypes = new ArrayList<ThingType>();
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
        View view = inflater.inflate(R.layout.openhabdiscoveryinboxlist_fragment, container, false);
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);
        discoveryButton = (ActionButton)view.findViewById(R.id.discovery_button);
        if (discoveryButton != null) {
            discoveryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Discovery button pressed");
                    if (mActivity != null) {
                        mActivity.openDiscovery();
                    }
                }
            });
        }
        return view;
    }


    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach()");
        try {
            mActivity = (OpenHABMainActivity) activity;
            mAsyncHttpClient = mActivity.getAsyncHttpClient();
            mActivity.setTitle(R.string.app_discoveryinbox);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must be OpenHABMainActivity");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDiscoveryInboxAdapter = new OpenHABDiscoveryInboxAdapter(this.getActivity(), R.layout.openhabdiscoveryinboxlist_item, mDiscoveryInbox);
        getListView().setAdapter(mDiscoveryInboxAdapter);
        getListView().setEmptyView(getActivity().findViewById(R.id.empty_inbox_view));
        Log.d(TAG, "onActivityCreated()");
        Log.d(TAG, "isAdded = " + isAdded());
    }

    @Override
    public void onResume () {
        super.onResume();
        Log.d(TAG, "onResume()");
        loadDiscoveryInbox();
        loadThingTypes();
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
        loadDiscoveryInbox();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        selectedInbox = position;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton(R.string.app_discoveryinbox_approve, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                sendInboxApprove(mDiscoveryInboxAdapter.getItem(selectedInbox).getThingUID());
            }
        });
        builder.setNeutralButton(R.string.app_discoveryinbox_ignore, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                sendInboxIgnore(mDiscoveryInboxAdapter.getItem(selectedInbox).getThingUID());
            }
        });
        builder.setNegativeButton(R.string.app_discoveryinbox_delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                sendInboxDelete(mDiscoveryInboxAdapter.getItem(selectedInbox).getThingUID());
            }
        });
        builder.setCancelable(true);
        builder.setTitle(R.string.app_discoveryinbox_deviceaction);
        builder.show();
    }

    private void loadDiscoveryInbox() {
        if (mAsyncHttpClient != null) {
            startProgressIndicator();
            String url = openHABBaseUrl + "rest/inbox";
            StringRequest request = new StringRequest
                    (Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    stopProgressIndicator();
                                    String jsonString = null;
                                    jsonString = response;
                                    Log.d(TAG, "Inbox request success");
                                    Log.d(TAG, jsonString);
                                    GsonBuilder gsonBuilder = new GsonBuilder();
                                    Gson gson = gsonBuilder.create();
                                    mDiscoveryInbox.clear();
                                    mDiscoveryInbox.addAll(Arrays.asList(gson.fromJson(jsonString, OpenHABDiscoveryInbox[].class)));
                                    mDiscoveryInboxAdapter.notifyDataSetChanged();
                                }},
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    stopProgressIndicator();
                                    Log.d(TAG, "Inbox request failure: " + error.getMessage());
                                }
                            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "text/plain; charset=utf-8");
                    headers.put("User-agent", "My useragent");
                    return headers;
                }

                @Override
                protected Map<String, String> getParams() {
                    Map<String,String> map = new HashMap<>();
                    return map;
                }
            };
            mRequestHandle = request;
            MyAsyncHttpClient.getInstance(getActivity()).addToRequestQueue(request);
        }
    }

    private void loadThingTypes () {
        if (mAsyncHttpClient != null) {
            startProgressIndicator();

            String url = openHABBaseUrl + "rest/thing-types";
            StringRequest request = new StringRequest
                    (Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    stopProgressIndicator();
                                    String jsonString = null;
                                    jsonString = response;
                                    Log.d(TAG, "Thing types request success");
                                    Log.d(TAG, jsonString);
                                    GsonBuilder gsonBuilder = new GsonBuilder();
                                    Gson gson = gsonBuilder.create();
                                    mThingTypes.clear();
                                    mThingTypes.addAll(Arrays.asList(gson.fromJson(jsonString, ThingType[].class)));
                                    mDiscoveryInboxAdapter.setThingTypes(mThingTypes);
                                    mDiscoveryInboxAdapter.notifyDataSetChanged();
                                }},
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    stopProgressIndicator();
                                    Log.d(TAG, "Thing types request failure: " + error.getMessage());
                                }
                            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "text/plain; charset=utf-8");
                    headers.put("User-agent", "My useragent");
                    return headers;
                }

                @Override
                protected Map<String, String> getParams() {
                    Map<String,String> map = new HashMap<>();
                    return map;
                }
            };
            mRequestHandle = request;
            MyAsyncHttpClient.getInstance(getActivity()).addToRequestQueue(request);
        }
    }

    private void sendInboxApprove(String UID) {
        if (mAsyncHttpClient != null) {
            startProgressIndicator();
            String url = openHABBaseUrl + "rest/inbox/" + UID + "/approve";
            StringRequest request = new StringRequest
                    (Request.Method.POST, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    stopProgressIndicator();
                                    Log.d(TAG, "Inbox approve request success");
                                    refresh();
                                }},
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    stopProgressIndicator();
                                    Log.e(TAG, "Inbox approve request error: " + error.getMessage());
                                }
                            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "text/plain; charset=utf-8");
                    headers.put("User-agent", "My useragent");
                    return headers;
                }

                @Override
                protected Map<String, String> getParams() {
                    Map<String,String> map = new HashMap<>();
                    return map;
                }
            };
            mRequestHandle = request;
            MyAsyncHttpClient.getInstance(getActivity()).addToRequestQueue(request);
        }
    }

    private void sendInboxIgnore(String UID) {
        if (mAsyncHttpClient != null) {
            startProgressIndicator();
            String url = openHABBaseUrl + "rest/inbox/" + UID + "/ignore";
            StringRequest request = new StringRequest
                    (Request.Method.POST, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    stopProgressIndicator();
                                    Log.d(TAG, "Inbox ignore request success");
                                    refresh();
                                }},
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    stopProgressIndicator();
                                    Log.e(TAG, "Inbox ignore request error: " + error.getMessage());
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


    private void sendInboxDelete(String UID) {
        if (mAsyncHttpClient != null) {
            startProgressIndicator();
            String url = openHABBaseUrl + "rest/inbox/" + UID;
            StringRequest request = new StringRequest
                    (Request.Method.DELETE, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    stopProgressIndicator();
                                    Log.d(TAG, "Inbox delete request success");
                                    refresh();
                                }},
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    stopProgressIndicator();
                                    Log.e(TAG, "Inbox ignore request error: " + error.getMessage());
                                }
                            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "text/plain; charset=utf-8");
                    headers.put("User-agent", "My useragent");
                    return headers;
                }

                @Override
                protected Map<String, String> getParams() {
                    Map<String,String> map = new HashMap<>();
                    return map;
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
