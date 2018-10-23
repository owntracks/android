package org.owntracks.android.robolectric;

import org.owntracks.android.App;
import org.owntracks.android.injection.components.AppComponent;
import org.owntracks.android.injection.modules.AppModule;
import org.owntracks.android.injection.modules.android.AndroindBindingModule;
import org.owntracks.android.injection.scopes.PerApplication;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.support.AndroidSupportInjectionModule;

@PerApplication
@Component(modules = {AppModule.class, DummyWaypointsModule.class, AndroidSupportInjectionModule.class, AndroindBindingModule.class})
public interface AppComponentForTest extends AppComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder app(App app);

        AppComponentForTest build();

    }
}
