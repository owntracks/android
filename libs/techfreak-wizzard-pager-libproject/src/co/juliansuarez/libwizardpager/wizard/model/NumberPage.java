package co.juliansuarez.libwizardpager.wizard.model;

import android.support.v4.app.Fragment;
import co.juliansuarez.libwizardpager.wizard.ui.NumberFragment;

public class NumberPage extends TextPage {

	public NumberPage(ModelCallbacks callbacks, String title) {
		super(callbacks, title);
	}

	@Override
	public Fragment createFragment() {
		return NumberFragment.create(getKey());
	}

}
