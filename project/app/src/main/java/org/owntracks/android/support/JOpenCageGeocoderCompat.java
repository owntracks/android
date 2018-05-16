package org.owntracks.android.support;

import com.byteowls.jopencage.JOpenCageGeocoder;
import com.byteowls.jopencage.model.JOpenCageRequest;
import com.byteowls.jopencage.model.JOpenCageResponse;
import com.byteowls.jopencage.model.JOpenCageReverseRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class JOpenCageGeocoderCompat extends JOpenCageGeocoder {
    OkHttpClient httpClient;

    private final static String OPENCAGE_HOST = "api.opencagedata.com";
    private final static String OPENCAGE_PATH = "/geocode/v1/json";

    JOpenCageGeocoderCompat(String apiKey) {
        super(apiKey);
        httpClient = new OkHttpClient();
    }

    public HttpUrl buildOkUri(JOpenCageRequest jOpenCageRequest)  {
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme("http")
                .host(OPENCAGE_HOST)
                .addPathSegment("geocode")
                .addPathSegment("v1")
                .addPathSegment("json");


        for (Map.Entry<String, String> e : jOpenCageRequest.getParameter().entrySet()) {
            if (e.getValue() != null) {
                urlBuilder.addQueryParameter(e.getKey(), e.getValue());
            }
        }
        urlBuilder.addQueryParameter("abbrv", "1");
        urlBuilder.addQueryParameter("key", getApiKey());
        urlBuilder.addQueryParameter("limit", "1");
        urlBuilder.addQueryParameter("no_dedupe", "1");
        urlBuilder.addQueryParameter("no_record", "1");

        return urlBuilder.build();
    }

    public JOpenCageResponse reverse(JOpenCageReverseRequest request) {
        return sendRequestOk(request);
    }

    private JOpenCageResponse sendRequestOk(JOpenCageRequest jOpenCageRequest) {
        HttpUrl url = buildOkUri(jOpenCageRequest);
        Timber.v("JOpencage request url %s", url);


        if (url != null) {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                String rs = response.body().string();
                Timber.v(rs);

                ObjectMapper mapper = new ObjectMapper();

                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return mapper.readValue(rs, JOpenCageResponse.class);
            } catch (IOException e) {
                Timber.e(e);
                return null;
            }
        } else {
            Timber.e("No jopencage request url build!");
        }
        return null;
    }

}
