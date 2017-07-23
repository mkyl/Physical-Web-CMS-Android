package org.physical_web.cms.exhibits;

import android.net.Uri;

/**
 * This interface is used by fragments to receive the results of the ACTION_OPEN_DOCUMENT intent
 */
public interface ContentPickerListener {
    int FILE_PICKER_ROUTING_CODE = 1032;

    void onContentReturned(Uri uri);
}
