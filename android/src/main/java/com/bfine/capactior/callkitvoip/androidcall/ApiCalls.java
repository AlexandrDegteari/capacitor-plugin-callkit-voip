package com.bfine.capactior.callkitvoip.androidcall;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiCalls {

    private final OkHttpClient client = new OkHttpClient();

    public void gettwiliotoken(final String connectionId, final RetreivedTokenCallback callback) {
        Log.d("gettwiliotoken", "called " + connectionId);

        String url = "http://us-central1-bettercall-app.cloudfunctions.net/getTokenTwilioVideo?name=operator&token=" + connectionId;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("gettwiliotoken", "Error: " + e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        Log.d("gettwiliotoken", "called " + jsonResponse);

                        String token = jsonResponse.getString("token");
                        Log.d("token", token);
                        callback.onTokenRetreived(token);
                    } catch (JSONException e) {
                        Log.d("gettwiliotoken", e.toString());
                        e.printStackTrace();
                    }
                } else {
                    Log.d("gettwiliotoken", "Request failed with code: " + response.code());
                }
                response.close();
            }
        });
    }
}