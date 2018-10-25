package org.owntracks.android.ui.welcome;

import android.os.Build;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.owntracks.android.support.RequirementsChecker;
import org.owntracks.android.ui.welcome.finish.FinishFragment;
import org.owntracks.android.ui.welcome.intro.IntroFragment;
import org.owntracks.android.ui.welcome.permission.PermissionFragment;
import org.owntracks.android.ui.welcome.play.PlayFragment;
import org.owntracks.android.ui.welcome.version.VersionFragment;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(Parameterized.class)
public class WelcomeAdapterTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private final boolean initialSetupCheckPassed;
    private final boolean playCheckPassed;
    private final boolean permissionCheckPassed;
    private final int androidVersion;
    private final int expectedFragments;

    public WelcomeAdapterTest(boolean initialSetupCheckPassed, boolean playCheckPassed, boolean permissionCheckPassed, int androidVersion, int expectedFragments) {
        this.initialSetupCheckPassed = initialSetupCheckPassed;
        this.playCheckPassed = playCheckPassed;
        this.permissionCheckPassed = permissionCheckPassed;
        this.androidVersion = androidVersion;
        this.expectedFragments = expectedFragments;
    }

    @Parameterized.Parameters(name = "setupCheckPassed={0}, playCheckPassed={1}, permissionCheckPassed={2}, androidVersion={3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {false, false, false, Build.VERSION_CODES.LOLLIPOP, 4},
                {false, false, false, Build.VERSION_CODES.M, 5},
                {false, true, false, Build.VERSION_CODES.LOLLIPOP, 3},
                {false, true, false, Build.VERSION_CODES.M, 4},
                {false, false, true, Build.VERSION_CODES.LOLLIPOP, 3},
                {false, false, true, Build.VERSION_CODES.M, 4},
                {false, true, true, Build.VERSION_CODES.LOLLIPOP, 2},
                {false, true, true, Build.VERSION_CODES.M, 3},
        });
    }


    @Mock
    private RequirementsChecker requirementsChecker;

    @InjectMocks
    private WelcomeAdapter welcomeAdapter;

    @Before
    public void setup() {
        initMocks(this); // Injects field-level mocks as well
    }

    @Test
    public void getCount() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), androidVersion);
        when(requirementsChecker.isInitialSetupCheckPassed()).thenReturn(initialSetupCheckPassed);
        when(requirementsChecker.isPlayCheckPassed()).thenReturn(playCheckPassed);
        when(requirementsChecker.isPermissionCheckPassed()).thenReturn(permissionCheckPassed);
        welcomeAdapter.setupFragments(new IntroFragment(), new VersionFragment(), new PlayFragment(), new PermissionFragment(), new FinishFragment());
        assertEquals(expectedFragments, welcomeAdapter.getCount());
    }

    /**
     * Helper method to override the value of a static final field on a class
     *
     * @param field    to be set
     * @param newValue to be set in the field
     * @throws NoSuchFieldException   if field doesn't exist
     * @throws IllegalAccessException if we're not able to set the value
     */
    private static void setFinalStatic(Field field, Object newValue) throws NoSuchFieldException, IllegalAccessException {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }
}

