package org.owntracks.android.support;

import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.tatarka.bindingcollectionadapter.BindingRecyclerViewAdapter;
import me.tatarka.bindingcollectionadapter.ItemViewArg;
import me.tatarka.bindingcollectionadapter.factories.BindingRecyclerViewAdapterFactory;


public class RecyclerViewAdapter<T> extends BindingRecyclerViewAdapter<T> implements View.OnClickListener, View.OnLongClickListener{


    public interface LongClickHandler<T>
    {
        void onLongClick(View v, T viewModel);
    }
    public interface ClickHandler<T>
    {
        void onClick(View v, T viewModel);
    }

    private static final String TAG = "RecyclerViewAdapter";
    private ClickHandler<T> clickHandler;
    private LongClickHandler<T> longClickHandler;
    private static final int ITEM_MODEL = -124;


    public RecyclerViewAdapter(ClickHandler ch, LongClickHandler lch, @NonNull ItemViewArg<T> arg) {
        super(arg);
        setClickHandler(ch);
        setLongClickHandler(lch);
    }
    @Override
    public void onClick(View v)
    {
        if (clickHandler != null)
        {
            T item = (T) v.getTag(ITEM_MODEL);
            clickHandler.onClick(v, item);
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        if (longClickHandler != null)
        {
            T item = (T) v.getTag(ITEM_MODEL);
            longClickHandler.onLongClick(v, item);
            return true;
        }
        return false;
    }


    @Override
    public ViewDataBinding onCreateBinding(LayoutInflater inflater, @LayoutRes int layoutId, ViewGroup viewGroup) {
        ViewDataBinding binding = super.onCreateBinding(inflater, layoutId, viewGroup);
        return binding;
    }

    @Override
    public void onBindBinding(ViewDataBinding binding, int bindingVariable, @LayoutRes int layoutId, int position, T item) {
        super.onBindBinding(binding, bindingVariable, layoutId, position, item);
        binding.getRoot().setTag(ITEM_MODEL, item);
        binding.getRoot().setOnClickListener(this);
        binding.getRoot().setOnLongClickListener(this);
    }

    public void setClickHandler(ClickHandler<T> clickHandler)
    {
        this.clickHandler = clickHandler;
    }

    public void setLongClickHandler(LongClickHandler<T> clickHandler)
    {
        this.longClickHandler = clickHandler;
    }

}
