
package org.openhab.habdroid.util;

import android.app.Activity;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ImageWithUrl {

    public static void loadImageUrl(final ImageView viewImage, final String url, final int backupId) {
        ViewTreeObserver vto = viewImage.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                viewImage.getViewTreeObserver().removeOnPreDrawListener(this);
                final int finalHeight = viewImage.getMeasuredHeight();
                final int finalWidth = viewImage.getMeasuredWidth();
                Picasso.with(viewImage.getContext())
                        .load(url)
                        .resize(finalWidth, finalHeight)
                        .centerCrop()
                        .into(viewImage, new Callback() {
                            @Override
                            public void onSuccess() {
                            }

                            @Override
                            public void onError() {
                                // load this:
                                Log.d("ImageWithUrl", "Image not loaded " + url);
                                if (backupId != 0) {
                                    viewImage.setImageResource(backupId);
                                }
                            }
                        });
                return true;
            }
        });
    }
}
