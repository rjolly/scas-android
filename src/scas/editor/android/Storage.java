/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package scas.editor.android;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import de.aflx.sardine.DavResource;
import de.aflx.sardine.Sardine;
import de.aflx.sardine.SardineFactory;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jscl.editor.Code;

/**
 *
 * @author rjolly
 */
public class Storage {
    public static Storage instance = new Storage();
    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_NOTE = 2;
    private static final int COLUMN_INDEX_MODIFIED = 4;
    private final FileFilter filter = new FileFilter() {
        public boolean accept(File file) {
            return !file.isDirectory() && file.getName().endsWith(".txt");
        }
    };
    private final Sardine sardine;

    private Storage() {
        sardine = SardineFactory.begin();
    }

    public void exportNotes(final Cursor cursor, final File dir) {
        final Map<String, File> map = new HashMap<String, File>();
        if (cursor == null) {
            return;
        }
        dir.mkdir();
        for (final File file : dir.listFiles(filter)) {
            map.put(file.getName(), file);
        }
        if (!cursor.isAfterLast()) do {
            final int id = cursor.getInt(COLUMN_INDEX_ID);
            final String title = cursor.getString(COLUMN_INDEX_TITLE);
            final String text = cursor.getString(COLUMN_INDEX_NOTE);
            final String note = text.lastIndexOf("\n") < text.length() - 1?text + "\n":text;
            final long modified = cursor.getLong(COLUMN_INDEX_MODIFIED);
            final String filename = title.trim() + ".txt";
            final File file = new File(dir, filename);
            if (file.exists()) {
                final long m = file.lastModified();
                if (modified > m) {
                    write(file, note, modified);
                }
                map.remove(filename);
            } else {
                write(file, note, modified);
            }
        } while (cursor.moveToNext());
        for (final File file : map.values()) file.delete();
    }

    public void write(final File file, final String note, final long modified) {
        try {
            final Writer writer = new FileWriter(file);
            writer.write(note, 0, note.length());
            writer.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        file.setLastModified(modified);
    }

    public void importNotes(final ContentResolver resolver, final Uri uri, final Cursor cursor, final File dir) {
        final Map<String, ContentValues> map = new HashMap<String, ContentValues>();
        if (cursor == null) {
            return;
        }
        if (!cursor.isAfterLast()) do {
            final int id = cursor.getInt(COLUMN_INDEX_ID);
            final String title = cursor.getString(COLUMN_INDEX_TITLE);
            final long modified = cursor.getLong(COLUMN_INDEX_MODIFIED);
            final ContentValues values = new ContentValues();
            values.put(NotePad.Notes._ID, id);
            values.put(NotePad.Notes.MODIFIED_DATE, modified);
            map.put(title, values);
        } while (cursor.moveToNext());
        for (final File file : dir.listFiles(filter)) {
            final String name = file.getName();
            final String title = name.substring(0, name.lastIndexOf(".txt"));
            final long modified = file.lastModified();
            if (map.containsKey(title)) {
                final ContentValues values = map.get(title);
                final long m = values.getAsLong(NotePad.Notes.MODIFIED_DATE);
                if (modified > m) {
                    final String note = read(file);
                    final int id = values.getAsInteger(NotePad.Notes._ID);
                    final Uri noteUri = ContentUris.withAppendedId(uri, id);
                    values.put(NotePad.Notes.NOTE, note);
                    values.put(NotePad.Notes.MODIFIED_DATE, modified);
                    resolver.update(noteUri, values, null, null);
                }
                map.remove(title);
            } else {
                final String note = read(file);
                final ContentValues values = new ContentValues();
                values.put(NotePad.Notes.TITLE, title);
                values.put(NotePad.Notes.NOTE, note);
                values.put(NotePad.Notes.CREATED_DATE, modified);
                values.put(NotePad.Notes.MODIFIED_DATE, modified);
                resolver.insert(uri, values);
            }
        }
        for (final ContentValues values : map.values()) {
            final int id = values.getAsInteger(NotePad.Notes._ID);
            final Uri noteUri = ContentUris.withAppendedId(uri, id);
            resolver.delete(noteUri, null, null);
        }
    }

    public String read(final File file) {
        try {
            final Reader reader = new FileReader(file);
            final String note = Code.instance("mmltxt.xsl").apply(reader);
            reader.close();
            return note;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void importNotes(final ContentResolver resolver, final Uri uri, final Cursor cursor, final String url) {
        final Map<String, ContentValues> map = new HashMap<String, ContentValues>();
        if (cursor == null) {
            return;
        }
        List<DavResource> resources;
        try {
            resources = sardine.list(url + "/");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        if (!cursor.isAfterLast()) do {
            final int id = cursor.getInt(COLUMN_INDEX_ID);
            final String title = cursor.getString(COLUMN_INDEX_TITLE);
            final long modified = cursor.getLong(COLUMN_INDEX_MODIFIED);
            final ContentValues values = new ContentValues();
            values.put(NotePad.Notes._ID, id);
            values.put(NotePad.Notes.MODIFIED_DATE, modified);
            map.put(title, values);
        } while (cursor.moveToNext());
        for (final DavResource res : resources) if (filter(res)) {
            final String name = res.getName();
            final String title = name.substring(0, name.lastIndexOf(".txt"));
            final long modified = res.getModified().getTime();
            if (map.containsKey(title)) {
                final ContentValues values = map.get(title);
                final long m = values.getAsLong(NotePad.Notes.MODIFIED_DATE);
                if (modified > m) {
                    final String note = read(url, name);
                    final int id = values.getAsInteger(NotePad.Notes._ID);
                    final Uri noteUri = ContentUris.withAppendedId(uri, id);
                    values.put(NotePad.Notes.NOTE, note);
                    values.put(NotePad.Notes.MODIFIED_DATE, modified);
                    resolver.update(noteUri, values, null, null);
                }
                map.remove(title);
            } else {
                final String note = read(url, name);
                final ContentValues values = new ContentValues();
                values.put(NotePad.Notes.TITLE, title);
                values.put(NotePad.Notes.NOTE, note);
                values.put(NotePad.Notes.CREATED_DATE, modified);
                values.put(NotePad.Notes.MODIFIED_DATE, modified);
                resolver.insert(uri, values);
            }
        }
        for (final ContentValues values : map.values()) {
            final int id = values.getAsInteger(NotePad.Notes._ID);
            final Uri noteUri = ContentUris.withAppendedId(uri, id);
            resolver.delete(noteUri, null, null);
        }
    }

    public boolean filter(final DavResource res) {
        return !res.isDirectory() && res.getName().endsWith(".txt");
    }

    public String read(final String url, final String name) {
        try {
            final InputStream stream = sardine.get(url + "/" + name);
            final Reader reader = new InputStreamReader(stream);
            final String note = Code.instance("mmltxt.xsl").apply(reader);
            reader.close();
            return note;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
