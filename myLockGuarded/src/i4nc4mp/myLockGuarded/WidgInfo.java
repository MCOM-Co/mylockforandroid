package i4nc4mp.myLockGuarded;

import android.net.Uri;
import android.provider.BaseColumns;


/**
 * Convenience definitions for NotePadProvider
 */
public final class WidgInfo {
    public static final String AUTHORITY = "i4nc4mp.mylockguarded.widgidprovider";

    // This class cannot be instantiated
    private WidgInfo() {}
    
    /**
     * Notes table - each entry just has the manager assigned ID, the row it belongs in, and the width/height
     */
    public static final class WidgTable implements BaseColumns {
        // This class cannot be instantiated
        private WidgTable() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/wID");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.i4nc4mp.wids";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.i4nc4mp.wid";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /**
         * Appwidget manager ID
         * <P>Type: INTEGER</P>
         */
        public static final String ID = "id";
        
        public static final String ROW = "row";
        //which row it was drawn in. we are using centering at the moment
        
        public static final String WIDTH = "width";
        
        public static final String HEIGHT = "height";
    }
}