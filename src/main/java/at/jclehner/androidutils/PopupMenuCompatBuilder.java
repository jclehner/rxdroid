package at.jclehner.androidutils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v4.widget.PopupMenuCompat;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import at.jclehner.rxdroid.Version;

public class PopupMenuCompatBuilder
{
	public interface OnDismissListener
	{
		void onDismiss(int id, Object rawMenu);
	}

	public interface OnMenuItemClickListener
	{
		boolean onMenuItemClick(MenuItem item);
	}

	private Context mContext;
	private View mAnchor;
	private int mId;
	private int mGravity = -1;
	private int mMenuResId;
	private CharSequence mTitle;
	private OnDismissListener mDismissListener;
	private OnMenuItemClickListener mMenuItemListener;

	public PopupMenuCompatBuilder(Context context, View anchor)
	{
		mContext = context;
		mAnchor = anchor;
	}

	public PopupMenuCompatBuilder setId(int id)
	{
		mId = id;
		return this;
	}

	public PopupMenuCompatBuilder setGravity(int gravity)
	{
		mGravity = gravity;
		return this;
	}

	public PopupMenuCompatBuilder setMenuResId(int menuResId)
	{
		mMenuResId = menuResId;
		return this;
	}

	public PopupMenuCompatBuilder setTitle(CharSequence title)
	{
		mTitle = title;
		return this;
	}

	public PopupMenuCompatBuilder setOnDismissListener(OnDismissListener l)
	{
		mDismissListener = l;
		return this;
	}

	public PopupMenuCompatBuilder setOnMenuItemClickListener(OnMenuItemClickListener l)
	{
		mMenuItemListener = l;
		return this;
	}

	public void show()
	{
		if(Version.SDK_IS_JELLYBEAN_OR_NEWER)
			showAsPopupMenu();
		else
			showAsContextMenu();
	}

	@TargetApi(14)
	private void showAsPopupMenu()
	{
		final PopupMenu menu;

		if(mGravity != -1)
			menu = new PopupMenu(mContext, mAnchor, mGravity);
		else
			menu = new PopupMenu(mContext, mAnchor);

		menu.inflate(mMenuResId);

		menu.setOnDismissListener(new PopupMenu.OnDismissListener()
		{
			@Override
			public void onDismiss(PopupMenu menu)
			{
				if(mDismissListener != null)
					mDismissListener.onDismiss(mId, menu);
			}
		});

		menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
		{
			@Override
			public boolean onMenuItemClick(MenuItem item)
			{
				if(mMenuItemListener != null)
					return mMenuItemListener.onMenuItemClick(item);

				return false;
			}
		});

		menu.show();
	}

	private void showAsContextMenu()
	{
		final MenuItem.OnMenuItemClickListener menuListener = new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item)
			{
				if(mMenuItemListener != null)
					return mMenuItemListener.onMenuItemClick(item);

				return false;
			}
		};

		mAnchor.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener()
		{
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
			{
				new MenuInflater(mContext).inflate(mMenuResId, menu);
				menu.setHeaderTitle(mTitle);

				for(int i = 0; i != menu.size(); ++i)
					menu.getItem(i).setOnMenuItemClickListener(menuListener);
			}
		});

		mAnchor.showContextMenu();
	}
}


