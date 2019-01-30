package org.owntracks.android.support.widgets;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import androidx.appcompat.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;

// This is a prefrence that fakes a toolbar for a preference screen until the Android Support library supports toolbars in preferences_private screens
public class ToolbarPreference extends Preference {
    private String title;
    Toolbar toolbar;
    PreferenceScreen screen;
    public ToolbarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ToolbarPreference(Context context, PreferenceScreen parent) {
        super(context);
        this.screen = parent;

    }




    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        parent.setPadding(0, 0, 0, 0);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.settings_toolbar, parent, false);

        toolbar = layout.findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goUp();
            }
        });


        return layout;
    }

    public void goUp() {

        if (screen == null)
            screen= (PreferenceScreen)findPreferenceInHierarchy(getKey()+"Screen") ;

        if(screen != null)
            screen.getDialog().dismiss();

    }

    public Toolbar getToolbar(){
        return toolbar;
    }

}
