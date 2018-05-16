package org.owntracks.android.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class OpencageGeocoder {
    private OkHttpClient httpClient;
    private String apiKey;
    private ObjectMapper jsonMapper;
    private final static String OPENCAGE_HOST = "api.opencagedata.com";

    OpencageGeocoder(String apiKey) {
        this.apiKey = apiKey;
        httpClient = new OkHttpClient();
        jsonMapper = new ObjectMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    public String reverse(double latitude, double longitude) {

        HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme("http")
                .host(OPENCAGE_HOST)
                .addPathSegment("geocode")
                .addPathSegment("v1")
                .addPathSegment("json");

        urlBuilder.addEncodedQueryParameter("q", String.valueOf(latitude)+","+String.valueOf(longitude));
        urlBuilder.addQueryParameter("no_annotations", "1");
        urlBuilder.addQueryParameter("abbrv", "1");
        urlBuilder.addQueryParameter("limit", "1");
        urlBuilder.addQueryParameter("no_dedupe", "1");
        urlBuilder.addQueryParameter("no_record", "1");
        urlBuilder.addQueryParameter("key", apiKey);

        HttpUrl url = urlBuilder.build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) throw new IOException("Unexpected code " + response);
            String rs = response.body().string();
            Timber.v(rs);

            return jsonMapper.readValue(rs, OpenCageResponse.class).getFormatted();
        } catch (IOException e) {
            Timber.e(e);
            return null;
        }
    }

}
