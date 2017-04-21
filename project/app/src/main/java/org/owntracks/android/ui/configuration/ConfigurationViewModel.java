package org.owntracks.android.ui.configuration;

import android.content.Context;
import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.io.IOException;

import javax.inject.Inject;


@PerActivity
public class ConfigurationViewModel extends BaseViewModel<org.owntracks.android.ui.configuration.ConfigurationMvvm.View> implements org.owntracks.android.ui.configuration.ConfigurationMvvm.ViewModel<org.owntracks.android.ui.configuration.ConfigurationMvvm.View> {
    @Bindable
    String effectiveConfiguration;

    @Inject
    public ConfigurationViewModel(@AppContext Context context) {

    }

    public void attachView(@NonNull ConfigurationMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
        updateEffectiveConfiguration();
    }

    private void updateEffectiveConfiguration() {
        try {
            setEffectiveConfiguration(formatString(Parser.toJsonPlain(Preferences.exportToMessage())));
        } catch (IOException e) {
            getView().displayLoadError();
        }
    }

    private static String formatString(String text) throws OutOfMemoryError{

        StringBuilder v = new StringBuilder();
        String indentString = "";

        for (int i = 0; i < text.length(); i++) {
            char letter = text.charAt(i);
            switch (letter) {
                case '{':
                case '[':
                    v.append("\n").append(indentString).append(letter).append("\n");
                    indentString = indentString + "\t";
                    v.append(indentString);
                    break;
                case '}':
                case ']':
                    indentString = indentString.replaceFirst("\t", "");
                    v.append("\n").append(indentString).append(letter);
                    break;
                case ',':
                    v.append(letter).append("\n").append(indentString);
                    break;

                default:
                    v.append(letter);
                    break;
            }
        }

        return v.toString();
    }

    @Bindable
    public String getEffectiveConfiguration() {
        return effectiveConfiguration;
    }

    @Bindable
    public void setEffectiveConfiguration(String effectiveConfiguration) {
        this.effectiveConfiguration = effectiveConfiguration;
    }
}
