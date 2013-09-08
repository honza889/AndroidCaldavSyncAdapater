/**
 * Copyright (c) 2012-2013, Gerald Garcia, Timo Berger
 * 
 * This file is part of Andoid Caldav Sync Adapter Free.
 *
 * Andoid Caldav Sync Adapter Free is free software: you can redistribute 
 * it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 3 of the 
 * License, or at your option any later version.
 *
 * Andoid Caldav Sync Adapter Free is distributed in the hope that 
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Andoid Caldav Sync Adapter Free.  
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.gege.caldavsyncadapter.caldav.entities;

import java.net.URI;
import java.net.URISyntaxException;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CalendarContract.Calendars;
import android.util.Log;

public class Calendar {
	public enum CalendarSource {
		undefined, Android, CalDAV
	}
		
	private static final String TAG = "Calendar";
		
	/**
	 * stores the CTAG of a calendar
	 */
	public static String CTAG = Calendars.CAL_SYNC1;
	
	/**
	 * stores the URI of a calendar
	 * example: http://caldav.example.com/calendarserver.php/calendars/username/calendarname
	 */
	public static String URI = Calendars._SYNC_ID;
	
	private String strCalendarColor = "";

	/**
	 * the event transformed into ContentValues
	 */
	public ContentValues ContentValues = new ContentValues();
	
	private Account mAccount = null;
	private ContentProviderClient mProvider = null;
	
	public boolean foundServerSide = false;
	public boolean foundClientSide = false;
	public CalendarSource Source = CalendarSource.undefined;
	
	/**
	 * example: http://caldav.example.com/calendarserver.php/calendars/username/calendarname
	 */
	public URI getURI() {
		String strUri = this.getContentValueAsString(Calendar.URI);
		URI result = null;
		try {
			result = new URI(strUri);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * example: http://caldav.example.com/calendarserver.php/calendars/username/calendarname
	 */
	public void setURI(URI uri) {
		this.setContentValueAsString(Calendar.URI, uri.toString());
	}

	/**
	 * example: Cleartext Display Name 
	 */
	public String getCalendarDisplayName() {
		return this.getContentValueAsString(Calendars.CALENDAR_DISPLAY_NAME);
	}

	/**
	 * example: Cleartext Display Name 
	 */
	public void setCalendarDisplayName(String displayName) {
		this.setContentValueAsString(Calendars.CALENDAR_DISPLAY_NAME, displayName);
	}
	

	/**
	 * example: 1143
	 */
	public void setCTag(String cTag) {
		this.setContentValueAsString(Calendar.CTAG, cTag);
	}
	
	/**
	 * example: 1143
	 */
	public String getcTag() {
		return this.getContentValueAsString(Calendar.CTAG);
	}
	
	/**
	 * example: #FFCCAA 
	 */
	public void setCalendarColorAsString(String color) {
		int maxlen = 6;
		
		this.strCalendarColor = color;
		if (!color.equals("")) {
			String strColor = color.replace("#", "");
			if (strColor.length() > maxlen)
				strColor = strColor.substring(0, maxlen);
			int intColor = Integer.parseInt(strColor, 16);
			this.setContentValueAsInt(Calendars.CALENDAR_COLOR, intColor);
		}
	}

	/**
	 * example: #FFCCAA 
	 */
	public String getCalendarColorAsString() {
		return this.strCalendarColor;
	}

	/**
	 * example 12345 
	 */
	public int getCalendarColor() {
		return this.getContentValueAsInt(Calendars.CALENDAR_COLOR);
	}
	
	/**
	 * example 12345 
	 */
	public void setCalendarColor(int color) {
		this.setContentValueAsInt(Calendars.CALENDAR_COLOR, color);
	}

	/**
	 * example: 
	 * 		should be: calendarname
	 * 		but is:    http://caldav.example.com/calendarserver.php/calendars/username/calendarname/
	 */
	public String getCalendarName() {
		return this.getContentValueAsString(Calendars.NAME);
	}

	/**
	 * example: 
	 * 		should be: calendarname
	 * 		but is:    http://caldav.example.com/calendarserver.php/calendars/username/calendarname/
	 */
	public void setCalendarName(String calendarName) {
		this.setContentValueAsString(Calendars.NAME, calendarName);
	}

	/**
	 * example: 8
	 */
	public int getAndroidCalendarId() {
		return this.getContentValueAsInt(Calendars._ID);
	}

	/**
	 * example: 8
	 */
	public void setAndroidCalendarId(int androidCalendarId) {
		this.setContentValueAsInt(Calendars._ID, androidCalendarId);
	}

	/**
	 * example: content://com.android.calendar/calendars/8
	 */
	public Uri getAndroidCalendarUri() {
		return ContentUris.withAppendedId(Calendars.CONTENT_URI, this.getAndroidCalendarId());
	}
	
	/**
	 * empty constructor
	 */
	public Calendar(CalendarSource source) {
		this.Source = source;
	}
	
	/**
	 * creates an new instance from a cursor
	 * @param cur must be a cursor from "ContentProviderClient" with Uri Calendars.CONTENT_URI
	 */
	public Calendar(Account account, ContentProviderClient provider, Cursor cur, CalendarSource source) {
		this.mAccount = account;
		this.mProvider = provider;
		this.foundClientSide = true;
		this.Source = source;

		String strSyncID = cur.getString(cur.getColumnIndex(Calendars._SYNC_ID));
		String strName = cur.getString(cur.getColumnIndex(Calendars.NAME));
		String strDisplayName = cur.getString(cur.getColumnIndex(Calendars.CALENDAR_DISPLAY_NAME));
		String strCTAG = cur.getString(cur.getColumnIndex(Calendar.CTAG));
		int intAndroidCalendarId = cur.getInt(cur.getColumnIndex(Calendars._ID));

		this.setCalendarName(strName);
		this.setCalendarDisplayName(strDisplayName);
		this.setCTag(strCTAG);
		this.setAndroidCalendarId(intAndroidCalendarId);

		if (strSyncID == null) {
			this.correctSyncID(strName);
			strSyncID = strName;
		}
		URI uri = null;
		try {
			uri = new URI(strSyncID);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		this.setURI(uri);
		
		//this.Debug();
	}
	
	public void Debug() {
		Log.v(TAG, "new Calendar");
		for (String Key : this.ContentValues.keySet()) {
			Log.v(TAG, Key + "=" + ContentValues.getAsString(Key));
		}
	}
	
	public Uri getOrCreateAndroidCalendar(CalendarList androidCalList) throws RemoteException {
		Uri androidCalendarUri = null;
		boolean isCalendarExist = false;
		
		Calendar androidCalendar = androidCalList.getCalendarByURI(this.getURI());
		if (androidCalendar != null) {
			isCalendarExist = true;
			androidCalendar.foundServerSide = true;
		}
		

		if (!isCalendarExist) {
			Calendar newCal = androidCalList.createNewAndroidCalendar(this);
			//androidCalendarUri = androidCalList.createNewAndroidCalendar(this);
			androidCalendarUri = newCal.getAndroidCalendarUri();
		} else {
			androidCalendarUri = androidCalendar.getAndroidCalendarUri();
			if (!this.getCalendarColorAsString().equals("")) {
				//serverCalendar.updateCalendarColor(returnedCalendarUri, serverCalendar);
				this.updateAndroidCalendar(androidCalendarUri, Calendars.CALENDAR_COLOR, this.getCalendarColor());
			}
			if ((this.ContentValues.containsKey(Calendars.CALENDAR_DISPLAY_NAME)) && 
				(androidCalendar.ContentValues.containsKey(Calendars.CALENDAR_DISPLAY_NAME))) {
				String serverDisplayName = this.ContentValues.getAsString(Calendars.CALENDAR_DISPLAY_NAME);
				String clientDisplayName = androidCalendar.ContentValues.getAsString(Calendars.CALENDAR_DISPLAY_NAME);
				if (!serverDisplayName.equals(clientDisplayName))
					this.updateAndroidCalendar(androidCalendarUri, Calendars.CALENDAR_DISPLAY_NAME, serverDisplayName);
			}
		}
		
		return androidCalendarUri;
	}
	
	/**
	 * the calendar Uri was stored as calendar Name. this function updates the URI (_SYNC_ID)
	 * @param calendarUri the real calendarUri
	 * @return success of this function 
	 */
	private boolean correctSyncID(String calendarUri) {
		boolean Result = false;
		Log.v(TAG, "correcting calendar:" + this.getContentValueAsString(Calendars.CALENDAR_DISPLAY_NAME));
			
		ContentValues mUpdateValues = new ContentValues();
		mUpdateValues.put(Calendar.URI, calendarUri);
		
		try {
			mProvider.update(this.SyncAdapterCalendar(), mUpdateValues, null, null);
			Result = true;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		return Result;
	}
	
	/**
	 * there is no corresponding calendar on server side. time to delete this calendar on android side.
	 * @return 
	 */
	public boolean deleteAndroidCalendar() {
		boolean Result = false;
		
		String mSelectionClause = "(" + Calendars._ID + " = ?)";
		int calendarId  = this.getAndroidCalendarId();
		String[] mSelectionArgs = {Long.toString(calendarId)};
		
		int CountDeleted = 0;
		try {
			CountDeleted = mProvider.delete(this.SyncAdapter(), mSelectionClause, mSelectionArgs);
			Result = true;
		} catch (RemoteException e) {
			e.printStackTrace();
		}	
		Log.d(TAG, "Android Calendars deleted: " + Integer.toString(CountDeleted));
		
		return Result;
	}

	/**
	 * updates the android calendar
	 * @param calendarUri the uri of the androidCalendar
	 * @param target must be from android.provider.CalendarContract.Calendars
	 * @param value the new value for the target
	 * @throws RemoteException
	 */
	public void updateAndroidCalendar(Uri calendarUri, String target, int value) throws RemoteException {
		ContentValues mUpdateValues = new ContentValues();
		mUpdateValues.put(target, value);
		
		mProvider.update(asSyncAdapter(calendarUri, mAccount.name, mAccount.type), mUpdateValues, null, null);
	}

	/**
	 * updates the android calendar
	 * @param calendarUri the uri of the androidCalendar
	 * @param target must be from android.provider.CalendarContract.Calendars
	 * @param value the new value for the target
	 * @throws RemoteException
	 */
	public void updateAndroidCalendar(Uri calendarUri, String target, String value) throws RemoteException {
		ContentValues mUpdateValues = new ContentValues();
		mUpdateValues.put(target, value);
		
		mProvider.update(asSyncAdapter(calendarUri, mAccount.name, mAccount.type), mUpdateValues, null, null);
	}
	
	private Uri SyncAdapterCalendar() {
		return asSyncAdapter(this.getAndroidCalendarUri(), mAccount.name, mAccount.type);
	}
	private Uri SyncAdapter() {
		return asSyncAdapter(Calendars.CONTENT_URI, mAccount.name, mAccount.type);
	}
	private static Uri asSyncAdapter(Uri uri, String account, String accountType) {
	    return uri.buildUpon()
	        .appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER,"true")
	        .appendQueryParameter(Calendars.ACCOUNT_NAME, account)
	        .appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType).build();
	}
	
	public void setAccount(Account account) {
		this.mAccount = account;
	}
	public void setProvider(ContentProviderClient provider) {
		this.mProvider = provider;
	}
	
	/**
	 * general access function to ContentValues
	 * @param Item the item name from Calendars.*
	 * @return the value for the item
	 */
	private String getContentValueAsString(String Item) {
		String Result = "";
		if (this.ContentValues.containsKey(Item))
			Result = this.ContentValues.getAsString(Item);
		return Result;
	}
	/**
	 * general access function to ContentValues
	 * @param Item the item name from Calendars.*
	 * @return the value for the item
	 */
	private int getContentValueAsInt(String Item) {
		int Result = 0;
		if (this.ContentValues.containsKey(Item))
			Result = this.ContentValues.getAsInteger(Item);
		return Result;
	}
	
	/**
	 * general access function to ContentValues
	 * @param Item the item name from Calendars.*
	 * @param Value the value for the item
	 * @return success of this function
	 */
	private boolean setContentValueAsString(String Item, String Value) {
		boolean Result = false;
		
		if (this.ContentValues.containsKey(Item))
			this.ContentValues.remove(Item);
		this.ContentValues.put(Item, Value);
		
		return Result;
	}
	
	/**
	 * general access function to ContentValues
	 * @param Item the item name from Calendars.*
	 * @param Value the value for the item
	 * @return success of this function
	 */
	private boolean setContentValueAsInt(String Item, int Value) {
		boolean Result = false;
		
		if (this.ContentValues.containsKey(Item))
			this.ContentValues.remove(Item);
		this.ContentValues.put(Item, Value);
		
		return Result;
	}
}
