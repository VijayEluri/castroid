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
package com.cornerofseven.castroid.data;

import java.util.Iterator;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import com.cornerofseven.castroid.rss.feed.RSSChannel;
import com.cornerofseven.castroid.rss.feed.RSSItem;

/**
 * A class to abstract out manipulating the database to
 * add or query RSSChannels (a Data Access Object, or at least most of one). 
 * Any action that adds a channel to the database should use these methods.
 * <p>
 * This class isolates clients from needing to know exactly how to insert 
 * items into the the database and provides a single, common point for database
 * manipulation, thus simplifying testing.
 * 
 * @author Sean Mooney
 *
 */
public class PodcastDAO {

	/**
	 * Add a new rss/podcast channel to the database.
	 * @param contentResolver
	 * @param channel
	 * @return
	 * 
	 * TODO: is any reason this should ever return false?
	 */
	public static boolean addRSS(ContentResolver contentResolver, RSSChannel channel){
		
		
		final int channelID = addRSSChannel(contentResolver, channel);
		
		Iterator<RSSItem> itemsIter = channel.itemsIterator();
		while(itemsIter.hasNext()){
			RSSItem item = itemsIter.next();
			addRSSItem(contentResolver, channelID, item);
		}
		
		return true;
	}
	
	/**
	 * Add an RSSChannel (feed) to the database.  
	 * @param contentResolver
	 * @param channel
	 * @return the Feed._ID primary key of the feed in the database.
	 */
	public static int addRSSChannel(ContentResolver contentResolver, RSSChannel channel){
		ContentValues values = new ContentValues();
		values.put(Feed.TITLE, channel.getmTitle());
		values.put(Feed.LINK, channel.getmLink());
		values.put(Feed.DESCRIPTION, channel.getmDesc());
		Uri feedUri = contentResolver.insert(Feed.CONTENT_URI, values);
		
		
		String frag = feedUri.getLastPathSegment();
		int feedId = Integer.parseInt(frag);
		return feedId;
	}
	
	/**
	 * Add a single item to the database.
	 * @param contentResolver
	 * @param channelID the primary key of the channel this item belongs to.
	 * @param item
	 * @return the Item._ID for the item.
	 */
	public static int addRSSItem(ContentResolver contentResolver, int channelID, RSSItem item){
		ContentValues values = new ContentValues();
		values.put(Item.OWNER, channelID);
		values.put(Item.DESC, item.getDesc());
		values.put(Item.TITLE, item.getTitle());
		values.put(Item.LINK, item.getLink());
		values.put(Item.ENC_LINK, item.getEnclosure());
		values.put(Item.ENC_SIZE, item.getEnclosureLength());
		values.put(Item.ENC_TYPE, item.getEnclosureType());
		Uri itemUri = contentResolver.insert(Item.CONTENT_URI, values);
		
		return Integer.parseInt(itemUri.getLastPathSegment());
	}
}