package org.owntracks.android.ui.base;

import android.support.annotation.LayoutRes;


public final class BaseAdapterItemView {
    /**
     * Use this constant as the {@code bindingVariable} to not bind any variable to the layout. This
     * is useful if no data is needed for that layout, like a static footer or loading indicator for
     * example.
     */
    public static final int BINDING_VARIABLE_NONE = 0;

    private int bindingVariable;
    @LayoutRes
    private int layoutRes;

    /**
     * Constructs a new {@code BaseAdapterItemView} with the given binding variable and layout res.
     *
     * @see #setBindingVariable(int)
     * @see #setLayoutRes(int)
     */
    public static BaseAdapterItemView of(int bindingVariable, @LayoutRes int layoutRes) {
        return new BaseAdapterItemView()
                .setBindingVariable(bindingVariable)
                .setLayoutRes(layoutRes);
    }

    /**
     * A convenience method for {@code BaseAdapterItemView.setBindingVariable(int).setLayoutRes(int)}.
     *
     * @return the {@code BaseAdapterItemView} for chaining
     */
    public BaseAdapterItemView set(int bindingVariable, @LayoutRes int layoutRes) {
        this.bindingVariable = bindingVariable;
        this.layoutRes = layoutRes;
        return this;
    }

    /**
     * Sets the binding variable. This is one of the {@code BR} constants that references the
     * variable tag in the item layout file.
     *
     * @return the {@code BaseAdapterItemView} for chaining
     */
    public BaseAdapterItemView setBindingVariable(int bindingVariable) {
        this.bindingVariable = bindingVariable;
        return this;
    }

    /**
     * Sets the layout resource of the item.
     *
     * @return the {@code BaseAdapterItemView} for chaining
     */
    public BaseAdapterItemView setLayoutRes(@LayoutRes int layoutRes) {
        this.layoutRes = layoutRes;
        return this;
    }

    public int bindingVariable() {
        return bindingVariable;
    }

    @LayoutRes
    public int layoutRes() {
        return layoutRes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseAdapterItemView itemView = (BaseAdapterItemView) o;

        if (bindingVariable != itemView.bindingVariable) return false;
        return layoutRes == itemView.layoutRes;
    }

    @Override
    public int hashCode() {
        int result = bindingVariable;
        result = 31 * result + layoutRes;
        return result;
    }
}
