/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scas.editor.android;

import scas.editor.android.NotePad.Notes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import jscl.editor.Code;
import java.io.File;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the intent if there is one, otherwise defaults to displaying the
 * contents of the {@link NotePadProvider}
 */
public class NotesList extends ListActivity {
    private static final String TAG = "NotesList";
    private static final int EXPORT_DIALOG = 1;
    private static final int IMPORT_DIALOG = 2;
    private static final int MISSING_DIALOG = 3;
    private static final int CREATE_DIALOG = 4;
    private String url;
    private File dir;
    private Code code;
    private Storage storage;

    // Menu item ids
    public static final int MENU_ITEM_DELETE = Menu.FIRST;
    public static final int MENU_ITEM_INSERT = Menu.FIRST + 1;
    public static final int MENU_ITEM_EXPORT = Menu.FIRST + 2;
    public static final int MENU_ITEM_IMPORT = Menu.FIRST + 3;
    public static final int MENU_ITEM_PREFERENCES = Menu.FIRST + 4;

    /**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[] {
            Notes._ID, // 0
            Notes.TITLE, // 1
            Notes.NOTE, // 2
            Notes.CREATED_DATE, // 3
            Notes.MODIFIED_DATE // 4
    };

    private static final int COLUMN_INDEX_TITLE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        // If no data was given in the intent (because we were started
        // as a MAIN activity), then use our default content provider.
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Notes.CONTENT_URI);
        }

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);

        // Perform a managed query. The Activity will handle closing and requerying the cursor
        // when needed.
        Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null, null,
                Notes.DEFAULT_SORT_ORDER);

        // Used to map notes entries from the database to views
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.noteslist_item, cursor,
                new String[] { Notes.TITLE }, new int[] { android.R.id.text1 });
        setListAdapter(adapter);
        initialize();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initialize();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // This is our one standard application action -- inserting a
        // new note into the list.
        menu.add(0, MENU_ITEM_INSERT, 0, R.string.menu_insert)
                .setShortcut('3', 'a')
                .setIcon(android.R.drawable.ic_menu_add);

        menu.add(0, MENU_ITEM_EXPORT, 0, R.string.menu_export)
                .setIcon(R.drawable.ic_menu_export);

        menu.add(0, MENU_ITEM_IMPORT, 0, R.string.menu_import)
                .setIcon(R.drawable.ic_menu_import);

        menu.add(0, MENU_ITEM_PREFERENCES, 0, R.string.menu_preferences)
                .setIcon(android.R.drawable.ic_menu_preferences);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {
            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Build menu...  always starts with the EDIT action...
            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);
            MenuItem[] items = new MenuItem[1];

            // ... is followed by whatever other actions are available...
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, null, specifics, intent, 0,
                    items);

            // Give a shortcut to the edit action.
            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_INSERT:
            // Launch activity to insert a new item
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        case MENU_ITEM_EXPORT:
            showDialog(dir.exists()?EXPORT_DIALOG:CREATE_DIALOG);
            return true;
        case MENU_ITEM_IMPORT:
            showDialog(dir.exists()?IMPORT_DIALOG:MISSING_DIALOG);
            return true;
        case MENU_ITEM_PREFERENCES:
            editPreferences();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case EXPORT_DIALOG:
                return new AlertDialog.Builder(this)
                        .setMessage(R.string.dialog_export)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                storage.exportNotes((Cursor)getListAdapter().getItem(0), dir);
                            }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();

            case IMPORT_DIALOG:
                return new AlertDialog.Builder(this)
                        .setMessage(R.string.dialog_import)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                storage.importNotes(getContentResolver(), getIntent().getData(), (Cursor)getListAdapter().getItem(0), dir, code);
                            }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();

            case MISSING_DIALOG:
                return new AlertDialog.Builder(this)
                        .setMessage(R.string.dialog_missing)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                storage.importNotes(getContentResolver(), getIntent().getData(), (Cursor)getListAdapter().getItem(0), url, code);
                            }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();

            case CREATE_DIALOG:
                return new AlertDialog.Builder(this)
                        .setMessage(R.string.dialog_create)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                storage.exportNotes((Cursor)getListAdapter().getItem(0), dir);
                            }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();
        }
        return null;
    }

    public void editPreferences() {
        Intent settingsActivity = new Intent(getBaseContext(), Settings.class);
        startActivity(settingsActivity);
    }

    public void initialize() {
        PreferenceManager.setDefaultValues(getBaseContext(), R.xml.preferences, false);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        final String location = prefs.getString("locationPref", "");
        final int id = Integer.parseInt(prefs.getString("enginePref", "0"));
        url = prefs.getString("urlPref", "");
        dir = new File(location);
        switch(id) {
        case 0:
        case 1:
            code = Code.instance("mmltxt.xsl");
            break;
        case 2:
            code = Code.instance("mmljava.xsl");
        }
        storage = new Storage(this);
   }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
                // Delete the note that the context menu is for
                Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
                getContentResolver().delete(noteUri, null, null);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            // The caller is waiting for us to return a note selected by
            // the user.  The have clicked on one, so return it now.
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
