package org.owntracks.android.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.nineoldandroids.view.ViewHelper;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceApplication;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Toasts;

public class ActivityWelcome extends ActivityBase {


    static final int TOTAL_PAGES = 4;
    private static final int PERMISSION_REQUEST_USER_LOCATION = 2;

    ViewPager pager;
    ScreenSlideAdapter pagerAdapter;
    LinearLayout circles;
    ImageButton btnDone;
    ImageButton btnNext;
    boolean isOpaque = true;


    private void startActivityMain() {
        Intent intent = new Intent(this, App.getRootActivityClass());
        startActivity(intent);
    }

    private boolean checkSetup() {
        if(Preferences.getSetupCompleted()) {
            return true;
        } else {
            return false;
        }
    }


    private boolean checkPlayServices() {

        if (ServiceApplication.checkPlayServices()) {
            Log.v(TAG, "check checkPlayServices ok");

            return true;
        } else {
            GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();


            int result = googleAPI.isGooglePlayServicesAvailable(this);
            if (googleAPI.isUserResolvableError(result)) {
                //googleAPI.getErrorDialog(this, result, RESULT_PLAY_SERVICES).show();
                googleAPI.showErrorDialogFragment(this, result, RESULT_PLAY_SERVICES);
            } else {
                showQuitError(GoogleApiAvailability.getInstance().getErrorString(result));
            }

            return false;
        }
    }


    boolean checkSetup = false;
    boolean checkPlay = false;
    boolean checkPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);




        runChecks();

        if(checkSetup ||  checkPlay || checkPermission) {
            recoverChecks();
        } else {
            startActivityMain();
        }
    }

    private void runChecks() {
        if(!checkSetup())
            checkSetup = true;
        if(!checkPlayServices())
            checkPlay = true;
        if(!checkPermissions())
            checkPermission = true;
    }

    private void recoverChecks() {
        if(checkSetup) {
            recoverSetup();
            return;
        }

        if(checkPlay) {
            recoverPlay();
            return;
        }
        if(checkPermission) {
            recoverPermission();
            return;
        }

    }

    private void recoverPermission() {
        //TODO
    }

    private void recoverPlay() {
        //TODO
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void recoverSetup() {


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_welcome);

        btnNext = ImageButton.class.cast(findViewById(R.id.btn_next));
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pagerAdapter.getItem(pager.getCurrentItem()).onNextClicked();
                pager.setCurrentItem(pager.getCurrentItem() + 1, true);
            }
        });

        btnDone = ImageButton.class.cast(findViewById(R.id.done));
        btnDone.setEnabled(false);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endTutorial();
            }
        });
        pager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new ScreenSlideAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        // pager.setPageTransformer(true, new CrossfadePageTransformer());
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == TOTAL_PAGES - 2 && positionOffset > 0) {
                    if (isOpaque) {
                        pager.setBackgroundColor(Color.TRANSPARENT);
                        isOpaque = false;
                    }
                } else {
                    if (!isOpaque) {
                        pager.setBackgroundColor(getResources().getColor(R.color.primary_material_light));
                        isOpaque = true;
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
                setIndicator(position);
                if (position == TOTAL_PAGES - 2) {
                    btnNext.setVisibility(View.GONE);
                    btnDone.setVisibility(View.VISIBLE);
                } else if (position < TOTAL_PAGES - 2) {
                    btnNext.setVisibility(View.VISIBLE);
                    btnDone.setVisibility(View.GONE);
                } else if (position == TOTAL_PAGES - 1) {
                    endTutorial();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        buildCircles();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pager != null) {
            pager.clearOnPageChangeListeners();
        }
    }

    private void buildCircles() {
        circles = LinearLayout.class.cast(findViewById(R.id.circles));

        float scale = getResources().getDisplayMetrics().density;
        int padding = (int) (5 * scale + 0.5f);

        for (int i = 0; i < TOTAL_PAGES - 1; i++) {
            ImageView circle = new ImageView(this);
            circle.setImageResource(R.drawable.ic_fiber_manual_record_white_18dp);
            circle.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            circle.setAdjustViewBounds(true);
            circle.setPadding(padding, 0, padding, 0);
            circles.addView(circle);
        }

        setIndicator(0);
    }

    private void setIndicator(int index) {
        if (index < TOTAL_PAGES) {
            for (int i = 0; i < TOTAL_PAGES - 1; i++) {
                ImageView circle = (ImageView) circles.getChildAt(i);
                if (i == index) {
                    circle.setAlpha(1f);
                } else {
                    circle.setAlpha(0.5f);
                }
            }
        }
    }


    private void endTutorial() {
        Preferences.setSetupCompleted();
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() == 0) {
            setResult(2);
            finish();
        } else {
            pager.setCurrentItem(pager.getCurrentItem() - 1);
        }
    }

    private class ScreenSlideAdapter extends FragmentStatePagerAdapter {

        public ScreenSlideAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public ScreenFragment getItem(int position) {
            ScreenFragment welcomeScreenFragment = null;
            switch (position) {
                case 0:
                    welcomeScreenFragment = WelcomeFragment.getInstance();
                    break;
                case 1:
                    welcomeScreenFragment = ModeFragment.getInstance();
                    break;
                case 2:
                    welcomeScreenFragment = PermissionFragment.getInstance();
                    break;
                case 3:
                    welcomeScreenFragment = FinishFragment.getInstance();
                    break;
            }

            return welcomeScreenFragment;
        }

        @Override
        public int getCount() {
            return TOTAL_PAGES;
        }
    }

    public class CrossfadePageTransformer implements ViewPager.PageTransformer {

        @Override
        public void transformPage(View page, float position) {
            int pageWidth = page.getWidth();

            View backgroundView = page.findViewById(R.id.welcome_fragment);
            View text_head = page.findViewById(R.id.screen_heading);
            View text_content = page.findViewById(R.id.screen_desc);

            if (0 <= position && position < 1) {
                ViewHelper.setTranslationX(page, pageWidth * -position);
            }
            if (-1 < position && position < 0) {
                ViewHelper.setTranslationX(page, pageWidth * -position);
            }

            if (position <= -1.0f || position >= 1.0f) {

            } else if (position == 0.0f) {
            } else {
                if (backgroundView != null) {
                    ViewHelper.setAlpha(backgroundView, 1.0f - Math.abs(position));

                }

                if (text_head != null) {
                    ViewHelper.setTranslationX(text_head, pageWidth * position);
                    ViewHelper.setAlpha(text_head, 1.0f - Math.abs(position));
                }

                if (text_content != null) {
                    ViewHelper.setTranslationX(text_content, pageWidth * position);
                    ViewHelper.setAlpha(text_content, 1.0f - Math.abs(position));
                }
            }
        }
    }
    public static class WelcomeFragment extends ScreenFragment {
        private static WelcomeFragment instance;

        public static WelcomeFragment getInstance() {
            if(instance == null)
                instance =  new WelcomeFragment();

            return instance;

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ViewGroup v = (ViewGroup) inflater.inflate(R.layout.fragment_welcome_1, container, false);
            return v;
        }
    }

    public static class FinishFragment extends ScreenFragment {
        private static FinishFragment instance;

        public static FinishFragment getInstance() {
            if(instance == null)
                instance =  new FinishFragment();

            return instance;

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ViewGroup v = (ViewGroup) inflater.inflate(R.layout.fragment_welcome_4, container, false);
            return v;
        }
    }

    public static class ModeFragment extends ScreenFragment {
        private static ModeFragment modeInstance;

        public static ModeFragment getInstance() {
            if(modeInstance == null)
                modeInstance =  new ModeFragment();

            return modeInstance;

        }


        RadioGroup rg;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ViewGroup v = (ViewGroup) inflater.inflate(R.layout.fragment_welcome_2, container, false);
            rg = (RadioGroup)v.findViewById(R.id.radioMode);

            return v;
        }

        public void onNextClicked() {


            int checkedId = rg.getCheckedRadioButtonId();

            if(checkedId == R.id.radioModeMqttPrivate) {
                Preferences.setMode(App.MODE_ID_MQTT_PRIVATE);
            } else if(checkedId == R.id.radioModeMqttPublic){
                Preferences.setMode(App.MODE_ID_MQTT_PUBLIC);
            }
        }


    }

    public static class PermissionFragment extends ScreenFragment {
        private static PermissionFragment instance;

        public static PermissionFragment getInstance() {
            if(instance == null)
                instance =  new PermissionFragment();

            return instance;

        }

        private Button button;
        private TextView success;
        

        public void permissionOK() {
            ActivityWelcome.class.cast(getActivity()).btnDone.setEnabled(true);

            button.setVisibility(View.GONE);
            success.setVisibility(View.VISIBLE);

        }

        public void permissionFail() {
            ActivityWelcome.class.cast(getActivity()).btnDone.setEnabled(false);

            button.setVisibility(View.VISIBLE);
            success.setVisibility(View.GONE);


        }

        @SuppressLint("NewApi")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            ViewGroup v = (ViewGroup) inflater.inflate(R.layout.fragment_welcome_3, container, false);

            button = (Button) v.findViewById(R.id.permissionRequest);
            success = (TextView) v.findViewById(R.id.permissionGranted);



            if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionFail();
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityWelcome.class.cast(getActivity()).runActionWithLocationPermissionCheck(PERMISSION_REQUEST_USER_LOCATION);

                    }
                });
            } else {
                permissionOK();

            }

            return v;
        }

    }

    void onRunActionWithPermissionCheck(int action, boolean granted) {
        switch (action) {
            case PERMISSION_REQUEST_USER_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (granted) {
                    PermissionFragment.getInstance().permissionOK();
                } else {
                    PermissionFragment.getInstance().permissionFail();
                }


                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    public static abstract class ScreenFragment extends Fragment {


        public ScreenFragment() {
            super();

        }

        @Override
        public abstract View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

        public void onNextClicked() {

        }
}

}
