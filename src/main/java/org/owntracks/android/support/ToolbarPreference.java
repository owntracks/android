package org.owntracks.android.support;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;

// This is a prefrence that fakes a toolbar for a preference screen until the Android Support library supports toolbars in preferences_private screens
public class ToolbarPreference extends Preference {
    Toolbar toolbar;

    public ToolbarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }




    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        parent.setPadding(0, 0, 0, 0);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.settings_toolbar, parent, false);

        toolbar = (Toolbar) layout.findViewById(R.id.fragmentToolbar);
        toolbar.setTitle(getTitle());
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goUp();
            }
        });



        return layout;
    }

    public void goUp(){
        PreferenceScreen prefScreen = (PreferenceScreen) getPreferenceManager().findPreference(getKey() + "Screen");
        if(prefScreen != null && prefScreen.getDialog() != null)
            prefScreen.getDialog().dismiss();
    }

    public Toolbar getToolbar(){
        return toolbar;
    }

}
