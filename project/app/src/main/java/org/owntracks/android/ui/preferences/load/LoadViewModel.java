package org.owntracks.android.ui.preferences.load;

import android.content.ContentResolver;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.codec.binary.Base64;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.model.messages.MessageBase;
import org.owntracks.android.model.messages.MessageConfiguration;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.scopes.ActivityScoped;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;


@ActivityScoped
public class LoadViewModel extends BaseViewModel<LoadMvvm.View> implements LoadMvvm.ViewModel<LoadMvvm.View> {
    private final Preferences preferences;
    private final Parser parser;
    private final WaypointsRepo waypointsRepo;

    private MessageConfiguration configuration;
    private String displayedConfiguration;
    private ImportStatus importStatus = ImportStatus.LOADING;

    @Inject
    public LoadViewModel(Preferences preferences, Parser parser, WaypointsRepo waypointsRepo) {
        this.preferences = preferences;
        this.parser = parser;
        this.waypointsRepo = waypointsRepo;
    }

    public void attachView(@Nullable Bundle savedInstanceState, @NonNull LoadMvvm.View view) {
        super.attachView(savedInstanceState, view);
    }

    private void setConfiguration(String json) throws IOException, Parser.EncryptionException {
        MessageBase message = parser.fromJson(json.getBytes());
        if (message instanceof MessageConfiguration) {
            this.configuration = (MessageConfiguration) parser.fromJson(json.getBytes());
            String prettyConfiguration;
            try {
                prettyConfiguration = parser.toUnencryptedJsonPretty(this.configuration);
            } catch (IOException e) {
                Timber.e(e);
                prettyConfiguration = "Unable to parse configuration";
            }
            displayedConfiguration = prettyConfiguration;
            importStatus = ImportStatus.SUCCESS;
            notifyChange();
        } else {
            throw new IOException("Message is not a valid configuration message");
        }
    }

    @Override
    public void saveConfiguration() {
        preferences.importFromMessage(configuration);
        if (!configuration.getWaypoints().isEmpty()) {
            waypointsRepo.importFromMessage(configuration.getWaypoints());
        }

        getView().showFinishDialog();
    }


    @Override
    public void extractPreferences(byte[] content) {
        try {
            setConfiguration(new String(content, StandardCharsets.UTF_8));
        } catch (IOException | Parser.EncryptionException e) {
            configurationImportFailed(e);
        }
    }

    @Override
    public void extractPreferences(URI uri) {
        try {
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                // Note: left here to avoid breaking compatibility.  May be removed
                // with sufficient testing. Will not work on Android >5 without granting READ_EXTERNAL_STORAGE permission
                Timber.v("using file:// uri");
                BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(uri.getPath())));
                StringBuilder total = new StringBuilder();

                String content;
                while ((content = r.readLine()) != null) {
                    total.append(content);
                }
                setConfiguration(total.toString());
            } else if ("owntracks".equals(uri.getScheme()) && "/config".equals(uri.getPath())) {
                Timber.v("Importing config using owntracks: scheme");

                List<NameValuePair> queryParams = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
                List<String> urlQueryParam = new ArrayList<>();
                List<String> configQueryParam = new ArrayList<>();
                for (NameValuePair queryParam : queryParams) {
                    if (queryParam.getName().equals("url")) {
                        urlQueryParam.add(queryParam.getValue());
                    }
                    if (queryParam.getName().equals("inline")) {
                        configQueryParam.add(queryParam.getValue());
                    }
                }
                if (configQueryParam.size() == 1) {

                    byte[] config = new Base64().decodeBase64(configQueryParam.get(0).getBytes());
                    setConfiguration(new String(config, StandardCharsets.UTF_8));
                } else if (urlQueryParam.size() == 1) {
                    URL remoteConfigUrl = new URL(urlQueryParam.get(0));
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(remoteConfigUrl)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            configurationImportFailed(new Exception("Failure fetching config from remote URL", e));
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try (ResponseBody responseBody = response.body()) {
                                if (!response.isSuccessful()) {
                                    configurationImportFailed(new IOException(String.format("Unexpected status code: %s", response)));
                                    return;
                                }
                                setConfiguration(responseBody != null ? responseBody.string() : "");
                            } catch (Parser.EncryptionException e) {
                                configurationImportFailed(e);
                            }
                        }
                    });
                    // This is async, so result handled on the callback
                } else {
                    throw new IOException("Invalid config URL");
                }
            } else {
                throw new IOException("Invalid config URL");
            }

        } catch (OutOfMemoryError | IOException | Parser.EncryptionException | IllegalArgumentException e) {
            configurationImportFailed(e);
        }
    }

    @Override
    public String getDisplayedConfiguration() {
        return displayedConfiguration;
    }

    @Override
    public ImportStatus getConfigurationImportStatus() {
        return importStatus;
    }

    @Override
    public void setError(Throwable e) {
        configurationImportFailed(e);
    }

    private void configurationImportFailed(Throwable e) {
        Timber.e(e);
        displayedConfiguration = String.format("Import failed: %s",e.getMessage());
        importStatus = ImportStatus.FAILED;
        notifyChange();
    }
}

