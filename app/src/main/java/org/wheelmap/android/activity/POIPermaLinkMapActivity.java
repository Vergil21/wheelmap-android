package org.wheelmap.android.activity;

import org.wheelmap.android.activity.base.BaseActivity;
import org.wheelmap.android.app.WheelmapApp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;

import java.io.UnsupportedEncodingException;

/**
 * Created by SMF on 10/06/14.
 */
public class POIPermaLinkMapActivity extends BaseActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String uri  = this.getIntent().getDataString();
        String[]  uriArray = uri.split("\\?");

        uri = uriArray[0];
        String uriAdress = uriArray[1];

        WheelmapApp app = (WheelmapApp) getApplication();

        uriArray = uri.split(":");
        uri = uriArray[1];
        uriArray = uri.split(",");

        double lat = Double.parseDouble(uriArray[0]);
        double lon = Double.parseDouble(uriArray[1]);

        app.setGeoLat(lat);
        app.setGeoLon(lon);

        uriArray = uriAdress.split("=");
        uriAdress = uriArray[1];
        uri = uriAdress.replaceAll("\\+", " ");

        String result = null;
        try {
            result = java.net.URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        app.setAddressString(result);

        startActivity(new Intent(this, StartupActivity.class));
    }
}