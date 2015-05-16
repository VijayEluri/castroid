/*
   Copyright 2010 Christopher Kruse and Sean Mooney

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. 
 */
package com.cornerofseven.castroid;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cornerofseven.castroid.data.Item;
import com.cornerofseven.castroid.handlers.ChannelItemClickHandler;

/**
 * @author Sean Mooney
 *
 */
public class ItemInformationView extends Fragment{

    public static final String ARG_ITEM_URI="arg.item.uri";

	protected TextView mItemName;
	protected WebView mItemDesc;
	protected Button mPlay, mDownload;
	
	/**
	 * Create an intent that will dispatch to ItemInformationView
	 * to display the given item
	 * @param itemId
	 * @return an intent that will display item info for itemId.
	 */
	public static Bundle createArgs(long itemId){

        Bundle args = new Bundle();
        Uri contentUri = ContentUris.withAppendedId(Item.CONTENT_URI, itemId);
        args.putParcelable(ARG_ITEM_URI, contentUri);
        
        return args;
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.item_information_view, null, true);
        collectWidgets(v);


        Bundle args = getArguments();
        if(args != null){
            Uri dataUri = (Uri)args.get(ARG_ITEM_URI);
            if(dataUri != null){
                bindData(dataUri);

                try{
                    long itemId = Long.parseLong(dataUri.getPathSegments().get(1));
                    bindListeners(itemId);
                }catch(NumberFormatException nfe){
                    Toast.makeText(getActivity(), "Unknown item id in uri "
                            + dataUri.toString(), Toast.LENGTH_LONG).show();
                }

            }
        }

        return v;
    }

    /**
	 * Bind the elements from the view to the proper fields.
	 */
	protected void collectWidgets(View view){
		mItemName = (TextView)(view.findViewById(R.id.iiv_item_name));
		mItemDesc = (WebView)(view.findViewById(R.id.iiv_item_desc));
		
		mPlay = (Button)(view.findViewById(R.id.iiv_play));
		mDownload = (Button)(view.findViewById(R.id.iiv_download));
	}
	
	/**
	 * Bind the listeners to the buttons.
	 * @param itemId, the database identifier for the item.
	 */
	protected void bindListeners(final long itemId) {
		final FragmentActivity activity = getActivity();
		
		final int ITEM_CLICK_PLAY = 1;
        final int ITEM_CLICK_DOWNLOAD = 2;
        
        /**
         * Handler for ChannelItem clicks.  We don't need
         * to view this item (we already are) so make the VIEW_ID -1;
         * 
         * In this activity, the play on click handler is the
         * only thing that will dispatch events to the 
         * ChannelItemClickHandler. This means we can make the 
         * object a field of the inner class, and not contribute
         * to polluting the state of the overall activity. Locality FTW. 
         */
        final ChannelItemClickHandler itemClickHandler 
            = new ChannelItemClickHandler(activity, ITEM_CLICK_PLAY, -1, ITEM_CLICK_DOWNLOAD);
        
		
		mPlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public final void onClick(View v) {
				itemClickHandler.onItemClick(ITEM_CLICK_PLAY, itemId);
			}
		});
		
		mDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View paramView) {
                itemClickHandler.onItemClick(ITEM_CLICK_DOWNLOAD, itemId);
            }
        });
	}

	/**
	 * Bind the data we want to show into the text views.
	 * 
	 * @param dataLocation Uri of item to display.
	 */
	protected void bindData(final Uri dataLocation) {
		final String[] projection = {
				Item.TITLE,
				Item.DESC,
				Item.PUB_DATE,
		};
		Cursor c = getActivity().getContentResolver().query(dataLocation, projection, null, null, null);

        try {
            if (c.moveToFirst()) {
                String title = c.getString(c.getColumnIndex(Item.TITLE));
                String desc = c.getString(c.getColumnIndex(Item.DESC));

                //TODO: Display the publication date.
                String pubDate = c.getString(c.getColumnIndex(Item.PUB_DATE));

                mItemName.setText(title);

                mItemDesc.loadData(desc, "text/html", "utf-8");

            }/*
		      * else, didn't return anything, which implies the data location
		      * was invalid. TODO: Do we warn/inform or just not display anything?
		      */
        } finally {
            if (c != null) {
                c.close();
            }
        }
	}
}
