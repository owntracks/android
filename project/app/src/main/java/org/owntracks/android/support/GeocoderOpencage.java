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

        HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme("http")
                .host(OPENCAGE_HOST)
                .addPathSegment("geocode")
                .addPathSegment("v1")
                .addPathSegment("json");

        urlBuilder.addEncodedQueryParameter("q", String.format("%s,%s", latitude, longitude));
        urlBuilder.addQueryParameter("no_annotations", "1");
        urlBuilder.addQueryParameter("abbrv", "1");
        urlBuilder.addQueryParameter("limit", "1");
        urlBuilder.addQueryParameter("no_dedupe", "1");
        urlBuilder.addQueryParameter("no_record", "1");
        urlBuilder.addQueryParameter("key", apiKey);

        HttpUrl url = urlBuilder.build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent",USERAGENT)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) throw new IOException("Unexpected code " + response);
            String rs = null;
            if (response.body() != null) {
                rs = response.body().string();
            }
            Timber.v(rs);

            return jsonMapper.readValue(rs, OpenCageResponse.class).getFormatted();
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

}
