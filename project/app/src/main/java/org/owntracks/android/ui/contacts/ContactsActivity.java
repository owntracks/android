package org.owntracks.android.ui.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import com.github.nitrico.lastadapter.LastAdapter;

import org.jetbrains.annotations.NotNull;
import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiActivityContactsBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.map.MapActivity;

import timber.log.Timber;


public class ContactsActivity extends BaseActivity<UiActivityContactsBinding, ContactsMvvm.ViewModel> implements ContactsMvvm.View, LastAdapter.OnClickListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Timber.v("onCreate");
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        setAndBindContentView(R.layout.ui_activity_contacts, savedInstanceState);

        setHasEventBus(false);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(getTitle());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigator.get().attachDrawer(binding.toolbar);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        LastAdapter.with(viewModel.getRecyclerItems(), BR.contact)
                .map(FusedContact.class, R.layout.ui_row_contact)
                //.onBindListener(this)       // Optional. 'this' is LastAdapter.OnBindListener
                .onClickListener(this)      // Optional. 'this' is LastAdapter.OnClickListener
                //.onLongClickListener(this)  // Optional. 'this' is LastAdapter.OnLongClickListener
                .into(binding.recyclerView);

        //binding.recyclerView.setHasFixedSize(true);
        //binding.setVariable(BR.recyclerAdapterFactory,  );
        //binding.setVariable(BR.recyclerItemView, ItemView.of(BR.contact, R.layout.ui_row_contact));
    }


    @Override
    public void onClick(@NotNull Object o, @NotNull View view, @LayoutRes int i, int i1) {
        activateContact(FusedContact.class.cast(o));
    }

    private void activateContact(FusedContact c) {
        if(!c.hasLocation())
            return;

        Timber.v("contadtId: %s", c.getId());

        Bundle b = new Bundle();
        b.putString(MapActivity.BUNDLE_KEY_CONTACT_ID, c.getId());
        navigator.get().startActivity(MapActivity.class, b);
    }
}
