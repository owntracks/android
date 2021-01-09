package org.owntracks.android.ui.preferences.load;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.model.messages.MessageBase;
import org.owntracks.android.model.messages.MessageConfiguration;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import javax.inject.Inject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;


@PerActivity
public class LoadViewModel extends BaseViewModel<LoadMvvm.View> implements LoadMvvm.ViewModel<LoadMvvm.View> {
    private final Preferences preferences;
    private final Parser parser;
    private final WaypointsRepo waypointsRepo;

    private MessageConfiguration configuration;
    private final MutableLiveData<String> formattedEffectiveConfiguration = new MutableLiveData<>();
    private final MutableLiveData<Throwable> importFailure = new MutableLiveData<>();
    private Context context;


    @Inject
    public LoadViewModel(@AppContext Context context, Preferences preferences, Parser parser, WaypointsRepo waypointsRepo) {
        this.preferences = preferences;
        this.parser = parser;
        this.waypointsRepo = waypointsRepo;
        this.context = context;
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
                prettyConfiguration = parser.toJsonPlainPretty(this.configuration);
            } catch (IOException e) {
                Timber.e(e);
                prettyConfiguration = "Unable to parse configuration";
            }
            formattedEffectiveConfiguration.postValue(prettyConfiguration);
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
    public MutableLiveData<String> formattedEffectiveConfiguration() {
        return formattedEffectiveConfiguration;
    }

    @Override
    public MutableLiveData<Throwable> importFailure() {
        return importFailure;
    }

    @Override
    public void extractPreferences(Uri uri) {
        try {
            BufferedReader r;
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                // Note: left here to avoid breaking compatibility.  May be removed
                // with sufficient testing. Will not work on Android >5 without granting READ_EXTERNAL_STORAGE permission
                Timber.v("using file:// uri");
                r = new BufferedReader(new InputStreamReader(new FileInputStream(uri.getPath())));
            } else if ("owntracks".equals(uri.getScheme()) && "/config".equals(uri.getPath())) {
                Timber.v("Importing config using owntracks: scheme");

                List<String> urlQueryParam = uri.getQueryParameters("url");
                List<String> configQueryParam = uri.getQueryParameters("inline");
                if (configQueryParam.size() == 1) {

                    byte[] config = Base64.decode(configQueryParam.get(0), Base64.DEFAULT);
                    r = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(config)));
                } else if (urlQueryParam.size() == 1) {
                    URL remoteConfigUrl = new URL(urlQueryParam.get(0));
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(remoteConfigUrl)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            importFailure.postValue(new Exception("Failure fetching config from remote URL", e));
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try (ResponseBody responseBody = response.body()) {
                                if (!response.isSuccessful()) {
                                    importFailure.postValue(new IOException(String.format("Unexpected status code: %s", response)));
                                    return;
                                }
                                setConfiguration(responseBody != null ? responseBody.string() : "");
                            } catch (Parser.EncryptionException e) {
                                importFailure.postValue(e);
                            }
                        }
                    });
                    // This is async, so result handled on the callback
                    return;
                } else {
                    throw new IOException("Invalid config URL");
                }
            } else if ("content".equals(uri.getScheme())) {
                Timber.v("using content:// uri");
                InputStream stream = this.context.getContentResolver().openInputStream(uri);
                r = new BufferedReader(new InputStreamReader(stream));
            } else {
                throw new IOException("Invalid config URL");
            }

            StringBuilder total = new StringBuilder();

            String content;
            while ((content = r.readLine()) != null) {
                total.append(content);
            }
            setConfiguration(total.toString());
        } catch (OutOfMemoryError | IOException | Parser.EncryptionException | IllegalArgumentException e) {
            importFailure.postValue(e);
        }
    }
}
