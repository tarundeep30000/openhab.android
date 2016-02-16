package org.openhab.habdroid.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;


public class MyAsyncHttpClient {
    public static final String BASE_URL = "http://dazhomes.com/";

    private static MyAsyncHttpClient mInstance;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static Context mCtx;

    private MyAsyncHttpClient(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();

        mImageLoader = new ImageLoader(mRequestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<String, Bitmap>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }

    public static synchronized MyAsyncHttpClient getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MyAsyncHttpClient(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}

/*
package org.openhab.habdroid.util;

import android.content.Context;
import android.preference.PreferenceManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import de.duenndns.ssl.MemorizingTrustManager;

public class MyAsyncHttpClient extends MyAsyncHttpClient {
	
	private SSLContext sslContext;
	private SSLSocketFactory sslSocketFactory;
	
	public MyAsyncHttpClient(Context ctx) {
        super();
//		super(ctx);
		try {
	        //sslContext = SSLContext.getInstance("TLS");
	        //sslContext.init(null, MemorizingTrustManager.getInstanceList(ctx), new java.security.SecureRandom());
	        //sslSocketFactory = new MySSLSocketFactory(sslContext);
            //if (PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(Constants.PREFERENCE_SSLHOST, false))
            //    sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	        //this.setSSLSocketFactory(sslSocketFactory);
	    } catch (Exception ex) {
	    }
	}
}
*/
