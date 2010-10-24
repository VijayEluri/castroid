package com.cornerofseven.castroid.data;

import android.net.Uri;
import android.provider.BaseColumns;

public final class Feed implements BaseColumns{
	public static final String BASE_AUTH = "com.cornerofseven.castroid.data.podcastdataprovider";

	//Non-instantiable class.
	private Feed(){}

	/**
	 * URI used to retrieve the content.
	 */
	public static final Uri CONTENT_URI = Uri.parse("content://" + BASE_AUTH + "/feeds");

	/**
	 * Feed MIME type for Directory for Content Provider
	 */
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cornerofseven.feed";

	/**
	 * Feed MIME type for Item for Content Provider
	 */
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.dir/vnd.cornerofseven.feed";

	/**
	 * How Feeds are sorted by default
	 */
	public static final String DEFAULT_SORT = "createdDt DESC";


	/******************************
	 * Database Columns           *
	 * Column Name - SQL Datatype *
	 ******************************/

	/**
	 * Table name for the database
	 */
	public static final String TABLE_NAME = "feed";

	/**
	 * Feed title - TEXT
	 */
	public static final String TITLE = "title";

	/**
	 * Feed Link - TEXT
	 */
	public static final String LINK = "link";

	/**
	 * Feed Description - TEXT
	 */
	public static final String DESCRIPTION = "description";

	/**
	 * Feed Image Link - TEXT
	 */
	public static final String IMAGE = "image";

	/**
	 * Feed Creation Date - INTEGER
	 */
	public static final String CREATED_DATE = "created";

	/**
	 * Feed Last Modified Date - INTEGER
	 */
	public static final String MODIFIED_DATE = "modified";
}
