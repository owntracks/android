package org.owntracks.android.ui.preferences.connection.dialog;

import android.content.Intent;

import org.owntracks.android.preferences.Preferences;

import timber.log.Timber;

public class ConnectionHostHttpDialogViewModel extends BaseDialogViewModel {
    private String url;
    public ConnectionHostHttpDialogViewModel(Preferences preferences) {
        super(preferences);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void load() {
        this.url = preferences.getUrl();
    }

    @Override
    public void save() {
        Timber.v("saving url:%s", url);
        preferences.setUrl(url);
    }

    public String getUrlText() {
        return url;
    }

    public void setUrlText(String url) {
        this.url = url;
    }
}
