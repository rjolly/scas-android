package scas.editor.android;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import jscl.editor.Code;
import jscl.editor.Files;

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

    private Storage() {}

    public void exportNotes(final Cursor cursor, final File dir) {
        final Map<String, File> map = new HashMap<String, File>();
        if (cursor == null) {
            return;
        }
        dir.mkdir();
        for (final File file : dir.listFiles(filter)) {
            map.put(file.getName(), file);
        }
        if (!cursor.isAfterLast()) do try {
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
        } catch (final IOException ex) {
            ex.printStackTrace();
        } while (cursor.moveToNext());
        for (final File file : map.values()) file.delete();
    }

    public void write(final File file, final String note, final long modified) throws IOException {
        final Writer writer = new FileWriter(file);
        writer.write(note, 0, note.length());
        writer.close();
        file.setLastModified(modified);
    }

    public void importNotes(final ContentResolver resolver, final Uri uri, final Cursor cursor, final File dir, final Code code) {
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
        for (final File file : dir.listFiles(filter)) try {
            final String name = file.getName();
            final String title = name.substring(0, name.lastIndexOf(".txt"));
            final long modified = file.lastModified();
            if (map.containsKey(title)) {
                final ContentValues values = map.get(title);
                final long m = values.getAsLong(NotePad.Notes.MODIFIED_DATE);
                if (modified > m) {
                    final String note = read(file, code);
                    final int id = values.getAsInteger(NotePad.Notes._ID);
                    final Uri noteUri = ContentUris.withAppendedId(uri, id);
                    values.put(NotePad.Notes.NOTE, note);
                    values.put(NotePad.Notes.MODIFIED_DATE, modified);
                    resolver.update(noteUri, values, null, null);
                }
                map.remove(title);
            } else {
                final String note = read(file, code);
                final ContentValues values = new ContentValues();
                values.put(NotePad.Notes.TITLE, title);
                values.put(NotePad.Notes.NOTE, note);
                values.put(NotePad.Notes.CREATED_DATE, modified);
                values.put(NotePad.Notes.MODIFIED_DATE, modified);
                resolver.insert(uri, values);
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        for (final ContentValues values : map.values()) {
            final int id = values.getAsInteger(NotePad.Notes._ID);
            final Uri noteUri = ContentUris.withAppendedId(uri, id);
            resolver.delete(noteUri, null, null);
        }
    }

    public String read(final File file, final Code code) throws IOException {
        try (final Reader reader = new FileReader(file)) {
            return code.apply(reader);
        }
    }

    public void importNotes(final ContentResolver resolver, final Uri uri, final Cursor cursor, final String url, final Code code) {
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
	try {
        for (final URL res : Files.instance.list(url + "/")) {
            final String name = new File(res.getFile()).getName();
            final String title = name.substring(0, name.lastIndexOf(".txt"));
            final URLConnection conn = res.openConnection();
            final long modified = conn.getLastModified();
            if (map.containsKey(title)) {
                final ContentValues values = map.get(title);
                final long m = values.getAsLong(NotePad.Notes.MODIFIED_DATE);
                if (modified > m) {
                    final String note = read(conn, code);
                    final int id = values.getAsInteger(NotePad.Notes._ID);
                    final Uri noteUri = ContentUris.withAppendedId(uri, id);
                    values.put(NotePad.Notes.NOTE, note);
                    values.put(NotePad.Notes.MODIFIED_DATE, modified);
                    resolver.update(noteUri, values, null, null);
                }
                map.remove(title);
            } else {
                final String note = read(conn, code);
                final ContentValues values = new ContentValues();
                values.put(NotePad.Notes.TITLE, title);
                values.put(NotePad.Notes.NOTE, note);
                values.put(NotePad.Notes.CREATED_DATE, modified);
                values.put(NotePad.Notes.MODIFIED_DATE, modified);
                resolver.insert(uri, values);
            }
        }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        for (final ContentValues values : map.values()) {
            final int id = values.getAsInteger(NotePad.Notes._ID);
            final Uri noteUri = ContentUris.withAppendedId(uri, id);
            resolver.delete(noteUri, null, null);
        }
    }

    public String read(final URLConnection conn, final Code code) throws IOException {
        try (final Reader reader = new InputStreamReader(conn.getInputStream())) {
            return code.apply(reader);
        }
    }
}
