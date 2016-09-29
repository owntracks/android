package org.owntracks.android.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.nineoldandroids.view.ViewHelper;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.support.unfree.GoogleApiAvailabilityResponder;
import org.owntracks.android.support.widgets.PausableViewPager;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.map.MapActivity;

import java.util.ArrayList;

import timber.log.Timber;

public class ActivityWelcome extends ActivityBase implements ViewPager.OnPageChangeListener {
    private static final int PERMISSION_REQUEST_USER_LOCATION = 2;
    private static final int RECOVER_PLAY = 1001;
    private static final java.lang.String BUNDLE_KEY_SETUP = "s";
    private static final java.lang.String BUNDLE_KEY_PLAY = "pl";
    private static final java.lang.String BUNDLE_KEY_PERMISSIONS = "pe";

    ViewPager pager;
    FragmentAdapter pagerAdapter;
    LinearLayout circles;
    ImageButton btnDone;
    ImageButton btnNext;
    boolean checkSetup = false;
    boolean checkPlay = false;
    boolean checkPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Bundle b = getIntent().getExtras();
        if(b == null) {  //first start
            b = runChecks();
        }
        if(hasFailedChecks(b)) {
            Timber.v("one or more earlier failed checks discovered");

            checkSetup = b.getBoolean(BUNDLE_KEY_SETUP);
            checkPlay = b.getBoolean(BUNDLE_KEY_PLAY);
            checkPermission = b.getBoolean(BUNDLE_KEY_PERMISSIONS);

            recoverChecks();
        } else {
            startActivityMain();
        }
    }

    public static boolean hasFailedChecks(Bundle b) {
        return b.getBoolean(BUNDLE_KEY_SETUP) ||  b.getBoolean(BUNDLE_KEY_PLAY) || b.getBoolean(BUNDLE_KEY_PERMISSIONS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pager != null) {
            pager.clearOnPageChangeListeners();
        }
    }


    private void startActivityMain() {
        App.enableForegroundBackgroundDetection();
        Intent intent = new Intent(this, MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void startActivityPreferences() {
        App.enableForegroundBackgroundDetection();
        Intent intent = new Intent(this, ActivityPreferences.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private static void startActivityWelcome(Context c, Bundle b) {
        Intent intent = new Intent(c, ActivityWelcome.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtras(b);
        c.startActivity(intent);
    }
    private static boolean checkSetup() {
       return Preferences.getSetupCompleted();
    }

    private static boolean checkPlayServices() {
        return  org.owntracks.android.support.unfree.GoogleApiAvailability.checkPlayServicesWithOverride();
        //return  GoogleApiAvailability.checkPlayServices(App.getContext(), true);
    }

    private static boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(App.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static Bundle runChecks() {
        Timber.v("running checks");
        Bundle b = new Bundle();
        if(!checkSetup()) {
            b.putBoolean(BUNDLE_KEY_SETUP, true);
        }
        if(!checkPlayServices()) {
            b.putBoolean(BUNDLE_KEY_PLAY, true);
        }
        if(!checkPermissions()) {
            b.putBoolean(BUNDLE_KEY_PERMISSIONS, true);
        }
        return b;
    }


    public static boolean runChecks(Context c) {
        Bundle b = runChecks();
        if(hasFailedChecks(b)) {
            startActivityWelcome(c, b);
            return true;
        }
        return false;
    }
    private void recoverChecks() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        setContentView(R.layout.activity_welcome);
        pagerAdapter = new FragmentAdapter(getSupportFragmentManager());

        if(checkSetup) {
            pagerAdapter.addItemId(WelcomeFragment.ID);
            pagerAdapter.addItemId(ModeFragment.ID);
        }

        if(checkPlay) {
            pagerAdapter.addItemId(PlayFragment.ID);
        }
        if(checkPermission) {
            pagerAdapter.addItemId(PermissionFragment.ID);
        }

        pagerAdapter.addItemId(FinishFragment.ID);




        btnNext = ImageButton.class.cast(findViewById(R.id.btn_next));
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnNextClick();
            }
        });

        btnDone = ImageButton.class.cast(findViewById(R.id.done));
        btnDone.setEnabled(false);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnDoneClick();
            }
        });
        pager = PausableViewPager.class.cast(findViewById(R.id.pager));
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(this);

        buildPagerCircles();
        Timber.v("recoverChecks finished");

        //onPageSelected(0);


    }



    private void buildPagerCircles() {
        circles = LinearLayout.class.cast(findViewById(R.id.circles));

        float scale = getResources().getDisplayMetrics().density;
        int padding = (int) (5 * scale + 0.5f);

        for (int i = 0; i < pagerAdapter.getCount(); i++) {
            ImageView circle = new ImageView(this);
            circle.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fiber_manual_record_white_18dp));
            circle.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            circle.setAdjustViewBounds(true);
            circle.setPadding(padding, 0, padding, 0);
            circles.addView(circle);
        }

        setIndicator(0);
    }

    private void setIndicator(int index) {
        if (index < pagerAdapter.getCount()) {
            for (int i = 0; i < pagerAdapter.getCount(); i++) {
                ImageView circle = (ImageView) circles.getChildAt(i);
                if (i == index) {
                    circle.setAlpha(1f);
                } else {
                    circle.setAlpha(0.5f);
                }
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() == 0) {
            finish();
        } else {
            pager.setCurrentItem(pager.getCurrentItem() - 1);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        Timber.v("onPageSelected");
        setIndicator(position);
        if (position == pagerAdapter.getCount()-1) {

            btnNext.setVisibility(View.GONE);
            btnDone.setVisibility(View.VISIBLE);
        } else {
            btnNext.setVisibility(View.VISIBLE);
            btnDone.setVisibility(View.GONE);
        }

        if(pagerAdapter.getItem(position).canEnablePagerNext())
            enablePagerNext();
        else
            disablePagerNext();

    }


    private void enablePagerNext() {
        btnNext.setEnabled(true);
        btnDone.setEnabled(true);
        btnNext.setAlpha(1.0f);
        btnDone.setAlpha(1.0f);
    }

    private void disablePagerNext() {
        btnNext.setEnabled(false);
        btnDone.setEnabled(false);
        btnNext.setAlpha(0.5f);
        btnDone.setAlpha(0.5f);

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        //Unused
    }

    private void onBtnNextClick() {
        pagerAdapter.getItem(pager.getCurrentItem()).onNextClicked();
        pager.setCurrentItem(pager.getCurrentItem() + 1, true);
    }

    private void onBtnDoneClick() {
        if(checkSetup) {
            Preferences.setSetupCompleted();
            if(!Preferences.isModeMqttPublic())
                startActivityPreferences();
            else
                startActivityMain();
        } else {
            startActivityMain();
        }
    }


    void onRunActionWithPermissionCheck(int action, boolean granted) {
        switch (action) {
            case PERMISSION_REQUEST_USER_LOCATION: {
                if (granted) {
                    PermissionFragment.getInstance().onPermissionGranted();
                } else {
                    PermissionFragment.getInstance().onPermissionDenied();
                }
            }
        }
    }



    private class FragmentAdapter extends FragmentStatePagerAdapter {
        private ArrayList<Integer> pagerAdapterIds ;

        public FragmentAdapter(FragmentManager fm) {
            super(fm);
            this.pagerAdapterIds = new ArrayList<>();
        }

        public void addItemId(int id) {
            pagerAdapterIds.add(id);
        }

        public int getLastItemId() {
            return pagerAdapterIds.get(pagerAdapterIds.size()-1);
        }

        @Override
        public ScreenFragment getItem(int position) {
            ScreenFragment fragment = null;
            int fragmentId = pagerAdapterIds.get(position);
            Timber.v("getItem %s / fragmentId: %s", position, fragmentId);
            switch (fragmentId) {
                case WelcomeFragment.ID:
                    fragment = WelcomeFragment.getInstance();
                    break;
                case ModeFragment.ID:
                    fragment = ModeFragment.getInstance();
                    break;
                case PlayFragment.ID:
                    fragment = PlayFragment.getInstance();
                    break;
                case PermissionFragment.ID:
                    fragment = PermissionFragment.getInstance();
                    break;
                case FinishFragment.ID:
                    fragment = FinishFragment.getInstance();
                    break;

            }

            return fragment;
        }

        @Override
        public int getCount() {
            return pagerAdapterIds.size();
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

        // Override to control if page can be skipped initially
        public boolean canEnablePagerNext() {
            return true;
        }

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            super.setUserVisibleHint(isVisibleToUser);

            // Make sure that we are currently visible
            if (this.isVisible()) {
                // If we are becoming invisible, then...
                if (!isVisibleToUser) {
                    Timber.d("%s isVisible && !isVisibleToUser", this);
                    onHide();
                } else {
                    Timber.d("%s isVisible && isVisibleToUser", this);
                    onShow();

                }
            } else {
                Timber.d("!isVisible");

            }
        }

        protected void onHide() {
        }

        protected void onShow() {
        }

    }

    public static class WelcomeFragment extends ScreenFragment {
        public static final int ID = 1;
        private static WelcomeFragment instance;

        public static WelcomeFragment getInstance() {
            if(instance == null)
                instance =  new WelcomeFragment();

            return instance;

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_welcome_intro, container, false);
        }
    }

    public static class ModeFragment extends ScreenFragment {
        public static final int ID = 2;
        private static ModeFragment modeInstance;
        private TextView modeDesc;

        public static ModeFragment getInstance() {
            if(modeInstance == null)
                modeInstance =  new ModeFragment();

            return modeInstance;

        }


        RadioGroup rg;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ViewGroup v = (ViewGroup) inflater.inflate(R.layout.fragment_welcome_mode, container, false);
            modeDesc = (TextView)v.findViewById(R.id.fragment_welcome_mode_description);

            rg = (RadioGroup)v.findViewById(R.id.radioMode);
            rg.clearCheck();

            rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {

                    switch (rg.getCheckedRadioButtonId()) {
                        case R.id.radioModeHttpPrivate:
                        case R.id.radioModeMqttPrivate:
                            modeDesc.setText(R.string.mqtt_private_description);
                            break;
                        case R.id.radioModeMqttPublic:
                            modeDesc.setText(R.string.mqtt_public_description);
                            break;

                    }
                }
            });
            rg.check(R.id.radioModeMqttPublic);

            return v;
        }

        public void onNextClicked() {

            int checkedId = rg.getCheckedRadioButtonId();

            if(checkedId == R.id.radioModeMqttPrivate) {
                Preferences.setMode(App.MODE_ID_MQTT_PRIVATE);
            } else if(checkedId == R.id.radioModeMqttPublic){
                Preferences.setMode(App.MODE_ID_MQTT_PUBLIC);
            } else if(checkedId == R.id.radioModeHttpPrivate) {
                Preferences.setMode(App.MODE_ID_HTTP_PRIVATE);
            }


        }


    }

    public static class PermissionFragment extends ScreenFragment {
        public static final int ID = 4;
        private static PermissionFragment instance;
        private ImageView img;

        public static PermissionFragment getInstance() {
            if(instance == null)
                instance =  new PermissionFragment();

            return instance;

        }

        private Button button;
        private TextView success;

        public PermissionFragment() {
            super();
        }

        // Override to control if page can be skipped initially
        @Override
        public boolean canEnablePagerNext() {
            return permissionsGranted();
        }

        public void onPermissionGranted() {
            success.setVisibility(View.VISIBLE);
            button.setVisibility(View.GONE);
            img.setImageResource(R.drawable.ic_assignment_turned_in_white_48dp);

            img.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_assignment_turned_in_white_48dp));

            ActivityWelcome.class.cast(getActivity()).enablePagerNext();
        }

        public void onPermissionDenied() {
            success.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
            img.setImageResource(R.drawable.ic_assignment_late_white_48dp);

            ActivityWelcome.class.cast(getActivity()).disablePagerNext();
        }

        private boolean permissionsGranted() {
            return ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        @SuppressLint("NewApi")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            ViewGroup v = (ViewGroup) inflater.inflate(R.layout.fragment_welcome_permissions, container, false);

            button = (Button) v.findViewById(R.id.button);
            success = (TextView) v.findViewById(R.id.message);
            img = (ImageView) v.findViewById(R.id.img);
            if(!permissionsGranted()) {
                onPermissionDenied();
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_USER_LOCATION);
                    }
                });
            } else {
                onPermissionGranted();
            }

            return v;
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RECOVER_PLAY: {
                Timber.v("%s %s", requestCode, resultCode);
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //public static class PlayFragment extends ScreenFragment implements DialogInterface.OnCancelListener, GoogleApiAvailabilityResponder {
    public static class PlayFragment extends ScreenFragment implements DialogInterface.OnCancelListener, GoogleApiAvailabilityResponder {

        public static final int ID = 3;
        private static PlayFragment instance;
        private Button button;
        private TextView message;
        private ImageView img;
        private static GoogleApiAvailability googleAPI;
        public static PlayFragment getInstance() {
            if(instance == null)
                instance =  new PlayFragment();

            return instance;
        }


        public PlayFragment() {
            super();
            googleAPI = GoogleApiAvailability.getInstance();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ViewGroup v = (ViewGroup) inflater.inflate(R.layout.fragment_welcome_play, container, false);

            img = (ImageView) v.findViewById(R.id.img);
            button = (Button) v.findViewById(R.id.recover);
            message = (TextView) v.findViewById(R.id.message);
            img = (ImageView) v.findViewById(R.id.img);
            return v;
        }

        protected void onHide() {
        }


        @Override
        public void onResume() {
            super.onResume();

            //GoogleApiAvailability.checkPlayServices(this);
            final GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
            final int resultCode = googleAPI.isGooglePlayServicesAvailable(getActivity());
            if(resultCode == ConnectionResult.SUCCESS) {
                onPlayServicesAvailable();
            } else {
                if (googleAPI.isUserResolvableError(resultCode)) {
                    onPlayServicesUnavailableRecoverable(resultCode);
                } else {
                    onPlayServicesUnavailableNotRecoverable(resultCode);
                }
            }
        }

        @Override
        public void onPlayServicesUnavailableNotRecoverable(int resultCode) {
            Timber.v("onPlayServicesUnavailableNotRecoverable()");

            message.setText(getString(R.string.play_services_not_available_not_recoverable));
            button.setVisibility(View.VISIBLE);
            button.setText(R.string.welcomeFixQuit);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().finish();
                    System.exit(0);
                }
            });
            img.setImageResource(R.drawable.ic_assignment_late_white_48dp);

            ActivityWelcome.class.cast(getActivity()).disablePagerNext();
        }


        @Override
        public void onPlayServicesAvailable() {
            Timber.v("onPlayServicesAvailable()");
            message.setText(getString(R.string.play_services_now_available));
            button.setVisibility(View.GONE);
            img.setImageResource(R.drawable.ic_assignment_turned_in_white_48dp);
            ActivityWelcome.class.cast(getActivity()).enablePagerNext();
        }

        @Override
        public void onPlayServicesUnavailableRecoverable(final int resultCode) {
           // Timber.v("onPlayServicesUnavailableRecoverable()");
           // message.setText(getString(R.string.play_services_not_available_recoverable));
           // button.setVisibility(View.VISIBLE);
           // button.setText(R.string.welcomeFixIssue);
           // GoogleApiAvailability.provisionRecoveryButton(button, getActivity(), resultCode, RECOVER_PLAY, this);
//
           // img.setImageResource(R.drawable.ic_assignment_late_white_48dp);
//
           // ActivityWelcome.class.cast(getActivity()).disablePagerNext();

            message.setText(getString(R.string.play_services_not_available_recoverable));
            button.setVisibility(View.VISIBLE);
            button.setText(R.string.welcomeFixIssue);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    googleAPI.getErrorDialog(getActivity(), resultCode, RECOVER_PLAY).show();
                    PendingIntent p = googleAPI.getErrorResolutionPendingIntent(getActivity(), resultCode, RECOVER_PLAY);
                    try {
                        if(p != null)
                            p.send();
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                }
            });
            img.setImageResource(R.drawable.ic_assignment_late_white_48dp);
            ActivityWelcome.class.cast(getActivity()).disablePagerNext();
        }



        public boolean canEnablePagerNext() {
            return false;
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

    public static class FinishFragment extends ScreenFragment {
        public static final int ID = 5;
        private static FinishFragment instance;

        public static FinishFragment getInstance() {
            if(instance == null)
                instance =  new FinishFragment();

            return instance;

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_welcome_done, container, false);
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



}
