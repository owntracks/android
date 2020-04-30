package org.owntracks.android.support.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.owntracks.android.R;

// This is a prefrence that fakes a toolbar for a preference screen until the Android Support library supports toolbars in preferences_private screens
public class ToolbarPreference extends Preference {
    private String title;
    private Toolbar toolbar;
    private PreferenceScreen screen;
    public ToolbarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ToolbarPreference(Context context, PreferenceScreen parent) {
        super(context);
        this.screen = parent;
//        parent.setPadding(0, 0, 0, 0);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.setLayoutResource(R.layout.settings_toolbar);
//        View layout = inflater.inflate(R.layout.settings_toolbar, parent, false);

//        toolbar = layout.findViewById(R.id.toolbar);
//        toolbar.setTitle(getTitle());
//        toolbar.setNavigationOnClickListener(v -> goUp());

    }

    private void goUp() {

        if (screen == null)
            screen= findPreferenceInHierarchy(getKey()+"Screen");

//        if(screen != null)
//            screen.getDialog().dismiss();

    }

    public Toolbar getToolbar(){
        return toolbar;
    }

}
