package ca.tyrannosaur.SMSBlacklist;

import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Common functions and classes for accessing a {@link BlacklistContentProvider}
 * .
 * 
 * @author charlie
 * 
 */
public class BlacklistContract {
   public static final String AUTHORITY = "ca.tyrannosaur.SMSBlacklist";
   public final static Uri ROOT_URI;

   static {
      ROOT_URI = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).build();
   }

   public static final Pattern buildFilterPattern(String filter, String affinity) {
      if (BlacklistContract.Filters.AFFINITY_EXACT.equals(affinity))
         return Pattern.compile(String.format("^%s$", filter));
      else if (BlacklistContract.Filters.AFFINITY_LEFT.equals(affinity))
         return Pattern.compile(String.format("^%s.*$", filter));
      else if (BlacklistContract.Filters.AFFINITY_RIGHT.equals(affinity))
         return Pattern.compile(String.format("^.*%s$", filter));
      else if (BlacklistContract.Filters.AFFINITY_SUBSTR.equals(affinity))
         return Pattern.compile(String.format("^.*%s.*$", filter));
      else
         return null;
   }

   public static final Uri buildFilterUri(long id) {
      return ROOT_URI.buildUpon().appendPath("filters").appendPath(String.valueOf(id)).build();
   }

   public static final Uri buildFiltersListUri() {
      return Uri.withAppendedPath(ROOT_URI, "filters");
   }

   public static final Uri buildLogsUri(long logId) {
      return ROOT_URI.buildUpon().appendPath("logs").appendPath(String.valueOf(logId)).build();
   }

   public static final Uri buildLogsListUri() {
      return ROOT_URI.buildUpon().appendPath("logs").build();
   }

   public static final Uri buildLogsForFilterUri(long filterId) {
      return ROOT_URI.buildUpon().appendPath("logsForFilter").appendPath(String.valueOf(filterId)).build();
   }

   /**
    * Constants for querying {@code blacklist} log entries.
    * 
    * @author charlie
    * 
    */
   public static class Logs implements BaseColumns {
      public static final String DEFAULT_SORT_ORDER = "dateReceived ASC";

      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.blacklist.log";
      public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.blacklist.log";

      // Column names for the ContentProvider
      public static final String MESSAGE_PDU = "messagePdu", DATE_RECEIVED = "dateReceived";

      // Column names
      public static final String FILTER_ID = "filterId", PATH = "path";
   }

   /**
    * Constants for querying {@code blacklist} filters.
    * 
    * @author charlie
    * 
    */
   public static class Filters implements BaseColumns {
      public static final String DEFAULT_SORT_ORDER = "filterText ASC";

      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.blacklist.filter", CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.blacklist.filter";

      public static final String AFFINITY_LEFT = "left", AFFINITY_RIGHT = "right", AFFINITY_EXACT = "exact", AFFINITY_SUBSTR = "substr";

      // Column names
      public static final String FILTER_TEXT = "filterText", NOTE = "note", FILTER_MATCH_AFFINITY = "filterMatchAffinity", UNREAD = "unread";
   }
}
