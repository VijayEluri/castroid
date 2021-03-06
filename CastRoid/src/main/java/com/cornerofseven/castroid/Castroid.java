/**
  Copyright 2010 Christopher Kruse and Sean Mooney

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License
 */

package com.cornerofseven.castroid;

import java.io.File;
import java.net.MalformedURLException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.SimpleCursorTreeAdapter;

import com.cornerofseven.castroid.data.DatabaseQuery;
import com.cornerofseven.castroid.data.Feed;
import com.cornerofseven.castroid.data.Item;
import com.cornerofseven.castroid.data.PodcastDAO;
import com.cornerofseven.castroid.data.UpdateChannel;
import com.cornerofseven.castroid.dialogs.DownloadDialog;
import com.cornerofseven.castroid.handlers.ChannelItemClickHandler;
import com.cornerofseven.castroid.network.DownloadService;
import com.cornerofseven.castroid.rss.MalformedRSSException;

public class Castroid extends FragmentActivity {
	public static final String TAG = "Castroid";

	// constants for the menus
	static final int MENU_FEED_DELETE   = 1;
	static final int MENU_ITEM_DOWNLOAD = 2;
	static final int MENU_FEED_UPDATE 	= 3;
	static final int MENU_FEED_VIEW 	= 4;
	static final int MENU_ITEM_PLAY		= 5;
	static final int MENU_ITEM_VIEW		= 6;
	
	// Referenced Widgets
	protected ExpandableListView mPodcastTree;

	// References to cursor columns
	static final String[] FEED_PROJECTION = { Feed._ID, Feed.TITLE,
			Feed.DESCRIPTION, Feed.LINK };
	static final String[] ITEM_PROJECTION = { Item._ID,
			Item.OWNER, Item.TITLE, Item.LINK, Item.DESC };

	// ExpandableListView formatting
	final int LAYOUT = android.R.layout.simple_expandable_list_item_1;
	final String[] FEED_FROM = { Feed.TITLE };
	final String[] CHILD_FROM = { Item.TITLE };
	final int[] TO = { android.R.id.text1 };

	// DIALOG IDs
	// TODO: Remove this, it shouldn't be needed.
	public static final int UPDATE_PROGRESS_DIALOG_ID = 2;
	public static final int ABOUT_DIALOG_ID = 3;


	/**
	 * Reference to the dialog create on showDialog(PROGRESS_DIALOG_ID).
	 * Set when the dialog is created, and lets us get a hold of
	 * the dialog to give as a reference to other objects that need
	 * to update progress on the main screen.
	 */
	//protected DownloadDialog mDownloadDialog;
	
	/**
	 * Dialog to use to display progress during feed updates.
	 */
	protected ProgressDialog mUpdateProgress;
	
	/**
	 * Used to update the feeds on a separate thread.
	 */
	protected AsyncFeedUpdater mFeedUpdater;
	
	/**
	 * Click handler for Channel items
	 */
	protected final ChannelItemClickHandler itemOnClickHandler = new ChannelItemClickHandler(this, MENU_ITEM_PLAY, MENU_ITEM_VIEW, MENU_ITEM_DOWNLOAD);
	
	//constants for any progress dialogs managed by handlers.
	public static final int WHAT_START = 1;
	public static final int WHAT_PREITEM = 2;
    public static final int WHAT_UPDATE = 3;
    public static final int WHAT_DONE = 4;
    public static final int WHAT_CANCELED = 5;
    
    //keys for the UpdateHandler data bundles
    public static final String PROGRESS_MAX = "MAX";
	public static final String PROGRESS_UPDATE = "UPDATE";
	public static final String PROGRESS_ITEMNAME = "NAME";
	
	// The media player to use for playing podcasts.
	// protected MediaPlayer mMediaPlayer;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mPodcastTree = ((ExpandableListView) findViewById(R.id.podcastList));

        CursorLoader loader = new CursorLoader(this,
                Feed.CONTENT_URI, FEED_PROJECTION, null, null, null);
        Cursor c = loader.loadInBackground();
		c.setNotificationUri(getContentResolver(), Feed.CONTENT_URI);

		mPodcastTree.setAdapter(new SimpleCursorTreeAdapter(this, c, LAYOUT,
				FEED_FROM, TO, LAYOUT, CHILD_FROM, TO) {

			@Override
			protected Cursor getChildrenCursor(Cursor groupCursor) {

				final String SELECT_ITEMS = Item.OWNER + " = ?";
				int feedId = groupCursor.getInt(groupCursor
						.getColumnIndex(Feed._ID));
				String[] selectionArgs = new String[] { Integer
						.toString(feedId) };
				return managedQuery(Item.CONTENT_URI, ITEM_PROJECTION,
						SELECT_ITEMS, selectionArgs, Item.DEFAULT_SORT);
			}

		});

		mPodcastTree.setClickable(true);
		mPodcastTree
				.setOnCreateContextMenuListener(new PodcastTreeContextMenuListener());

		mPodcastTree
				.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
					@Override
					public boolean onChildClick(
							ExpandableListView paramExpandableListView,
							View paramView, int groupPos, int childPos,
							long itemId) {
						return itemOnClickHandler.onItemClick(MENU_ITEM_VIEW, itemId);
					}
				});
	}

	@Override
	public void onStop(){
		super.onStop();
		Log.i(TAG, "Stoping castroid");
	}
	
	
	protected void addFeed() {
		Intent intent = new Intent(this, NewFeed.class);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.addFeed:
                addFeed();
                return true;
            case R.id.about:
                showDialog(ABOUT_DIALOG_ID);
                return true;
            case R.id.updateAll:
                updateAllChannels();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		    case MENU_FEED_DELETE:
		    {
		    	ListAdapter list = mPodcastTree.getAdapter();
		        //TODO: Is there a better way that doesn't use a cursor?
		        Cursor cursor = (Cursor) list.getItem(item.getGroupId());
		        if (cursor == null) {
		            return false;
		        }
		        int feedID = cursor.getInt(cursor.getColumnIndex(Feed._ID));
		        Uri queryUri = ContentUris.withAppendedId(Feed.CONTENT_URI, feedID);
		        getContentResolver().delete(queryUri, null, null);
		        return true;
		    }
		    case MENU_ITEM_PLAY:
		    {
		    	ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item
		        .getMenuInfo();
		        long itemId = info.id;
		        //re-dispatch to the item click handler.
		        return itemOnClickHandler.onItemClick(MENU_ITEM_PLAY, itemId);
		    }
		    case MENU_ITEM_DOWNLOAD:
		    {
		        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item
		        .getMenuInfo();
		        long itemId = info.id;
		        
		        itemOnClickHandler.onItemClick(MENU_ITEM_DOWNLOAD, itemId);
		        
		        return true;
		    }
		    case MENU_FEED_UPDATE:
		    {
		        ListAdapter list = mPodcastTree.getAdapter();
		        Cursor cursor = (Cursor) list.getItem(item.getGroupId());
		        if (cursor == null) {
		            return false;
		        }
		        int feedID = cursor.getInt(cursor.getColumnIndex(Feed._ID));
		        updateChannel(feedID);
		        return true;
		    }
		    case MENU_FEED_VIEW:
		    {
		    	ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item
		        .getMenuInfo();
		    	long feedId = info.id;
		    	Uri contentURI = ContentUris.withAppendedId(Feed.CONTENT_URI, feedId);
		    	Intent viewFeedIntent = new Intent("android.intent.action.VIEW");
		    	viewFeedIntent.setData(contentURI);
		    	startActivity(viewFeedIntent);
		    	return true;
		    }
		    
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		
		switch (id) {
		case UPDATE_PROGRESS_DIALOG_ID:
		{
		    //stack ref to the type information.
		    ProgressDialog upProg = new ProgressDialog(this);
		    upProg.setCancelable(true);
		    upProg.setTitle("Updating Feeds");
		    upProg.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface paramDialogInterface) {
                    mFeedUpdater.cancel(true);
                }
            });
		   
		    //assign the reference to other places 
		    //that need aliases to the new dialog.
		    mUpdateProgress = upProg; //field reference to access this dialog from other places in the activity.
		    return upProg;
		}
		case ABOUT_DIALOG_ID:
		{
			Dialog aboutDialog = new Dialog(this);
			aboutDialog.setContentView(R.layout.about_dialog);
			aboutDialog.setTitle(R.string.aboutLabel);
			return aboutDialog;
		}
		default:
			return super.onCreateDialog(id);
		}		
	}

//	
//	protected void playStream(long itemId) {
//		Uri itemUri = ContentUris.withAppendedId(Item.CONTENT_URI, itemId);
//		Cursor c = managedQuery(itemUri, 
//				new String[]{Item.ENC_LINK, Item.ENC_TYPE},
//				null, null, null);
//		
//		if(c.getCount() > 0){
//			String type, dataUri;
//
//			c.moveToFirst();
//			dataUri = c.getString(c.getColumnIndex(Item.ENC_LINK));
//			type = c.getString(c.getColumnIndex(Item.ENC_TYPE));
//			
//			Intent systemDefault = new Intent(Intent.ACTION_VIEW);
//			systemDefault.setDataAndType(Uri.parse(dataUri), type);
//			startActivity(systemDefault);
//		}else{
//			Toast.makeText(this, "No media found to play", Toast.LENGTH_LONG).show();
//		}
//	}
//	
	/**
	 * Update all the channels in the database.
	 * Runs asynchronously.
	 */
	protected void updateAllChannels(){
	    final Activity activity = this; //bind the context for the thread.

	    DatabaseQuery feedIdsQ = PodcastDAO.getFeedIdsQuery();
	    Cursor c = activity.managedQuery(
	            feedIdsQ.getContentUri(), 
	            feedIdsQ.getProjection(), 
	            feedIdsQ.getSelection(), 
	            feedIdsQ.getSelectionArgs(), 
	            feedIdsQ.getSortOrder());

	    //marshal the data for the updateChannel method.
	    Integer[] feedIds = new Integer[c.getCount()];
	    int curIndex = 0;
	    int feedCol = c.getColumnIndex(Feed._ID);
	    while(c.moveToNext()){
	        feedIds[curIndex++] = c.getInt(feedCol);
	    }

	    updateChannel(feedIds);
	}
	
	/**
     * Update the selected feed(s).
     * 
     *  Can update multiple feeds on the same call.
     *  Use this if/when the "update all" feature is added.
     *  
     * @param feedId
     */
    protected void updateChannel(Integer... feedId){
        //TODO: Do we need to worry about mFeedUpdater still running?
        mFeedUpdater = new AsyncFeedUpdater(this);
        mFeedUpdater.execute(feedId);
    }

//    /**
//     * Assuming the download dialog has been created, return its reference.
//     * @return reference to the download dialog object.
//     */
//    public DownloadDialog getDownloadDialog(){
//        return mDownloadDialog;
//    }
    
    
    private class PodcastTreeContextMenuListener implements
			View.OnCreateContextMenuListener {
		/**
		 * Default Constructor
		 */
		public PodcastTreeContextMenuListener() {
		}

		/**
		 * @see android.view.View.OnCreateContextMenuListener#onCreateContextMenu(android.view.ContextMenu,
		 *      android.view.View, android.view.ContextMenu.ContextMenuInfo)
		 */
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			// Recover PodcastTree Menu Info.
			ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;

			switch (ExpandableListView
					.getPackedPositionType(info.packedPosition)) {
			case ExpandableListView.PACKED_POSITION_TYPE_CHILD:
				menu.setHeaderTitle("Item Options");
				menu.add(0, MENU_ITEM_DOWNLOAD, 0, R.string.menu_download);
				menu.add(0, MENU_ITEM_PLAY, 0, R.string.menu_play);
				break;
			case ExpandableListView.PACKED_POSITION_TYPE_GROUP:
				int group = ExpandableListView
						.getPackedPositionGroup(info.packedPosition);
				ExpandableListAdapter ela = ((ExpandableListView) v)
						.getExpandableListAdapter();
				Cursor groupCursor = (Cursor) ela.getGroup(group);
				if (groupCursor != null) {
					menu.setHeaderTitle(groupCursor.getString(groupCursor
							.getColumnIndex(Feed.TITLE)));
					menu.add(group, MENU_FEED_VIEW, 0, R.string.menu_view_feed);
					menu.add(group, MENU_FEED_UPDATE, 0, R.string.menu_update);
					menu.add(group, MENU_FEED_DELETE, 0, R.string.menu_delete);
					
				}
				break;
			default:
				Log.e(TAG, "Error in selecting packed position.");
			}
		}
	}
    
    private Handler mUpdateHandler = new Handler(){
        /*
         * (non-Javadoc)
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            
            switch (msg.what) {
                case WHAT_START:
                {
                    showDialog(UPDATE_PROGRESS_DIALOG_ID);
                    
                    int max = msg.getData().getInt(PROGRESS_MAX);
                    mUpdateProgress.setMax(max);
                    mUpdateProgress.setProgress(0);
                    break;
                }
                case WHAT_PREITEM:
                {
                    Bundle data = msg.getData();
                    String currentItem = (data != null) ? data.getString(PROGRESS_ITEMNAME) : "";
                    mUpdateProgress.setMessage(currentItem);
                    break;
                }
                case WHAT_UPDATE:
                {   
                    int total = msg.getData().getInt(PROGRESS_UPDATE);
                    mUpdateProgress.setProgress(total);
                    break;
                }
                case WHAT_DONE:
                {  
                    dismissDialog(UPDATE_PROGRESS_DIALOG_ID);
                    break;
                }
            }
        }    
    };
}
