package ca.tyrannosaur.SMSBlacklist;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class Blacklist extends ListActivity implements OnItemClickListener {

   private static final String TAG = Blacklist.class.getName();

   private BlacklistApi api;

   /*
    * The handler for when a service connects and disconnects. Not much needs to
    * be done here except initialize the API for updating whether the blacklist
    * is enabled/disabled.
    */
   private ServiceConnection serviceConnection = new ServiceConnection() {

      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
         api = BlacklistApi.Stub.asInterface(service);
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
         Log.i(TAG, "Service connection closed");
      }

   };

   // TODO: fix this later
   public static final int MENU_DELETE = Menu.FIRST;

   private static final String[] PROJECTION = new String[] { BlacklistContract.Filters._ID, BlacklistContract.Filters.FILTER_TEXT, BlacklistContract.Filters.NOTE,
         BlacklistContract.Filters.FILTER_MATCH_AFFINITY, BlacklistContract.Filters.UNREAD };

   private StaticViewCursorAdapter listAdapter;
   private AlertDialog confirmClearAllDialog;
   private AlertDialog aboutDialog;

   @Override
   protected void onCreate(Bundle bundle) {
      super.onCreate(bundle);
      setContentView(R.layout.activity_main);

      // Create a cursor over all the filters and pass it to the ListAdapter
      Cursor cursor = managedQuery(BlacklistContract.buildFiltersListUri(), PROJECTION, null, null, BlacklistContract.Filters.DEFAULT_SORT_ORDER);

      listAdapter = new StaticViewCursorAdapter(this, R.layout.list_item_filter, cursor, PROJECTION, new int[] { android.R.id.text1, android.R.id.text2, R.id.filterAffinity });

      listAdapter.addView(R.layout.list_item_add_filter, StaticViewCursorAdapter.POSITION_TOP);
      setListAdapter(listAdapter);

      getListView().setOnItemClickListener(this);
      getListView().setOnCreateContextMenuListener(this);

      buildOrInitUI();

      // Start the service if it isn't running
      Intent startServiceIntent = new Intent(BlacklistService.ACTION_START_SERVICE);
      startService(startServiceIntent);
      bindService(startServiceIntent, serviceConnection, 0);
   }

   private void buildOrInitUI() {
      // Confirm clear all dialog
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(R.string.dialog_title_confirmClearAll).setCancelable(false).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
            clearBlacklist();
         }
      }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
         }
      });

      confirmClearAllDialog = builder.create();

      // About dialog
      String version;
      try {
         PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
         version = pInfo.versionName;
      }
      catch (NameNotFoundException e) {
         version = "n/a";
      }

      builder = new AlertDialog.Builder(this);
      builder.setMessage(String.format(getString(R.string.dialog_text_about), version)).setTitle(R.string.app_name).setCancelable(true);

      aboutDialog = builder.create();
   }

   private void addToBlacklist() {
      Intent i = new Intent(Intent.ACTION_INSERT, BlacklistContract.buildFiltersListUri());
      startActivity(i);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.main_menu, menu);
      return true;
   }

   @Override
   public boolean onPrepareOptionsMenu(final Menu menu) {
      final MenuItem toggle = menu.findItem(R.id.main_menu_toggle);

      try {
         if (api.getEnabled()) {
            toggle.setTitle(R.string.main_menu_enabled);
            toggle.setIcon(R.drawable.btn_toggle_on);
         }
         else {
            toggle.setTitle(R.string.main_menu_disabled);
            toggle.setIcon(R.drawable.btn_toggle_off);
         }
      }
      catch (RemoteException e) {
         Log.e(TAG, "Failed to contact the service");
      }

      return super.onPrepareOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case R.id.main_menu_add_entry:
            {
               addToBlacklist();
               return true;
            }
         case R.id.main_menu_toggle:
            {
               try {
                  api.setEnabled(!api.getEnabled());
               }
               catch (RemoteException e) {
                  Log.e(TAG, "Failed to contact the service");
               }
               return true;
            }
         case R.id.main_menu_clear_all:
            {
               confirmClearAllDialog.show();
               return true;
            }
         case R.id.main_menu_about:
            {
               aboutDialog.show();
               return true;
            }
         default:
            return super.onOptionsItemSelected(item);
      }
   }

   private void clearBlacklist() {
      getContentResolver().delete(BlacklistContract.buildFiltersListUri(), null, null);
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
      unbindService(serviceConnection);
   }

   /*
    * Creates the long-press context menu when clicking on a filter.
    * 
    * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
    * android.view.View, android.view.ContextMenu.ContextMenuInfo)
    */
   @Override
   public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

      try {
         AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
         Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

         // The filter isn't available
         if (cursor == null)
            return;

         // Set the title to be the filtered number
         menu.setHeaderTitle(cursor.getString(cursor.getColumnIndexOrThrow(BlacklistContract.Filters.FILTER_TEXT)));
         menu.add(0, MENU_DELETE, 0, R.string.main_context_menu_delete);

      }
      catch (ClassCastException e) {
      }
   }

   @Override
   public boolean onContextItemSelected(MenuItem item) {
      AdapterView.AdapterContextMenuInfo info;
      try {
         info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
      }
      catch (ClassCastException e) {
         return false;
      }

      switch (item.getItemId()) {
         case MENU_DELETE:
            {
               // Delete the note that the context menu is for
               Uri uri = ContentUris.withAppendedId(BlacklistContract.buildFiltersListUri(), info.id);
               getContentResolver().delete(uri, null, null);
               return true;
            }
      }
      return false;
   }

   @Override
   public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
      if (listAdapter.getItemViewType(position) == StaticViewCursorAdapter.TYPE_STATIC_VIEW)
         addToBlacklist();
   }

}
