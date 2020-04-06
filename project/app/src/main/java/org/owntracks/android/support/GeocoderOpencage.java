package org.owntracks.android.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

import static org.owntracks.android.services.MessageProcessorEndpointHttp.USERAGENT;

public class GeocoderOpencage implements Geocoder {
    private OkHttpClient httpClient;
    private String apiKey;
    private ObjectMapper jsonMapper;
    private final static String OPENCAGE_HOST = "api.opencagedata.com";

    GeocoderOpencage(String apiKey) {
        this.apiKey = apiKey;
        httpClient = new OkHttpClient();
        jsonMapper = new ObjectMapper();
        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String reverse(double latitude, double longitude) {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(OPENCAGE_HOST)
                .addPathSegment("geocode")
                .addPathSegment("v1")
                .addPathSegment("json")

                .addEncodedQueryParameter("q", String.format("%s,%s", latitude, longitude))
                .addQueryParameter("no_annotations", "1")
                .addQueryParameter("abbrv", "1")
                .addQueryParameter("limit", "1")
                .addQueryParameter("no_dedupe", "1")
                .addQueryParameter("no_record", "1")
                .addQueryParameter("key", apiKey)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USERAGENT)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null)
                throw new IOException(String.format("Unexpected code %s", response));
            String rs = response.body().string();
            Timber.d(rs);
            String toot = jsonMapper.readValue(rs, OpenCageResponse.class).getFormatted();
            Timber.d(toot);
            return toot;
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

}
