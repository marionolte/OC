/*
 * KeePass 2.x (KDBX) facade for the MarioHelp Core IO library.
 *
 * Backed by KeePassJava2 (org.linguafranca.pwdb) which reads and writes BOTH
 * the KDBX 3.1 (AES + Salsa20) and KDBX 4.x (Argon2 + ChaCha20 + HMAC block)
 * formats, i.e. every database any KeePass 2.x release can produce.
 *
 * @author SuMario
 */
package com.macmario.io.crypt;

import com.macmario.general.Version;

import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.KdbxHeader;
import org.linguafranca.pwdb.kdbx.KdbxStreamFormat;
import org.linguafranca.pwdb.kdbx.jackson.JacksonDatabase;
import org.linguafranca.pwdb.kdbx.jackson.JacksonEntry;
import org.linguafranca.pwdb.kdbx.jackson.JacksonGroup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


/**
 * High level facade around a single KeePass 2.x database.
 *
 * <p>Supported operations:
 * <ul>
 *   <li>create a new database ({@link #create(String)} + {@link #save(File, String)})</li>
 *   <li>open an existing database in either KDBX 3.1 or KDBX 4.x format
 *       ({@link #open(File, String)})</li>
 *   <li>change the master password ({@link #changeMasterPassword(File, String, String)}
 *       or {@link #save(File, String)} with a new password)</li>
 *   <li>insert / update / delete entries in the root group or any sub-folder
 *       addressed by a {@code "/Folder/Sub"} path</li>
 *   <li>create / delete sub-folders (groups)</li>
 *   <li>add / read / extract / delete file attachments on an entry</li>
 * </ul>
 *
 * <p>The underlying KeePassJava2 model is mutable by design: mutations are
 * applied to the in-memory database and only become durable when one of the
 * {@code save}/{@code changeMasterPassword} methods is invoked.
 *
 * <p>Paths are forward-slash separated and rooted at the implicit root group,
 * e.g. {@code "Internet/Banking"} or {@code "/Internet/Banking"}. An empty
 * path or {@code null} addresses the root group itself.
 */
public class KeePass extends Version {
    
    private static final long serialVersionUID = 4711L;

    /** KDBX format version used when writing. */
    public enum Format {
        /** KeePass 2.20 .. 2.35 native format: AES key derivation, Salsa20 inner stream. */
        KDBX31,
        /** KeePass 2.36+ default format: Argon2 key derivation, ChaCha20, HMAC block authentication. */
        KDBX4
    }

    private static final String PATH_SEPARATOR = "/";

    /** Raw KDBX header version numbers (see {@link KdbxHeader}). */
    private static final int KDBX_VERSION_31 = 3;
    private static final int KDBX_VERSION_4  = 4;

    private final JacksonDatabase database;
    private Format format;

    private KeePass(JacksonDatabase database, Format format) {
        this.database = Objects.requireNonNull(database, "database");
        this.format = format == null ? Format.KDBX4 : format;
    }

    // ------------------------------------------------------------------
    // Factory methods
    // ------------------------------------------------------------------

    /**
     * Creates a new, empty in-memory database. Nothing is written to disk until
     * {@link #save(File, String)} is called. Defaults to the modern KDBX 4 format.
     *
     * @param databaseName human readable database name (may be {@code null})
     * @return a new facade wrapping an empty database
     */
    public static KeePass create(String databaseName) {
        return create(databaseName, Format.KDBX4);
    }

    /**
     * Creates a new, empty in-memory database in the requested format.
     *
     * @param databaseName human readable database name (may be {@code null})
     * @param format       KDBX format to use when this database is saved
     * @return a new facade wrapping an empty database
     */
    public static KeePass create(String databaseName, Format format) {
        try {
            JacksonDatabase db = new JacksonDatabase();
            if (databaseName != null) {
                db.setName(databaseName);
            }
            return new KeePass(db, format);
        } catch (IOException e) {
            throw new KeePassException("Could not create a new KeePass database", e);
        }
    }

    /**
     * Opens an existing KDBX file. The format (KDBX 3.1 or 4.x) is auto-detected
     * and preserved, so a subsequent {@link #save(File, String)} round-trips the
     * original format unless {@link #setFormat(Format)} is called.
     *
     * @param file           the {@code .kdbx} file to open
     * @param masterPassword the master password
     * @return a facade wrapping the loaded database
     * @throws KeePassException if the file is missing, corrupt or the password is wrong
     */
    public static KeePass open(File file, String masterPassword) {
        requireFile(file);
        requirePassword(masterPassword);
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            JacksonDatabase db = JacksonDatabase.load(creds(masterPassword), in);
            Format detected = detectFormat(db);
            return new KeePass(db, detected);
        } catch (IOException e) {
            throw new KeePassException("Could not open KeePass database: " + file
                    + " (wrong password or corrupt/unsupported file)", e);
        }
    }

    private static Format detectFormat(JacksonDatabase db) {
        Object sf = db.getStreamFormat();
        if (sf instanceof KdbxStreamFormat ksf) {
            // KdbxHeader version: 3 -> KDBX 3.1, 4 -> KDBX 4
            int version = ksf.getStreamConfiguration().getVersion();
            return version >= 4 ? Format.KDBX4 : Format.KDBX31;
        }
        return Format.KDBX4;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    /**
     * Writes the database to {@code file} encrypted with {@code masterPassword}
     * using the current {@link #getFormat() format}. Writing with a different
     * password than the one used to open the file effectively changes the master
     * password.
     *
     * @param file           destination file
     * @param masterPassword master password to encrypt with
     * @throws KeePassException on any I/O or encryption error
     */
    public void save(File file, String masterPassword) {
        Objects.requireNonNull(file, "file");
        requirePassword(masterPassword);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            database.save(streamFormat(), creds(masterPassword), out);
        } catch (IOException e) {
            throw new KeePassException("Could not save KeePass database: " + file, e);
        }
    }

    /**
     * Convenience round-trip that opens {@code file} with {@code oldPassword} and
     * re-writes it with {@code newPassword}, preserving the original KDBX format
     * and the entire contents (groups, entries, history, attachments).
     *
     * @param file        the database file to re-key
     * @param oldPassword the current master password
     * @param newPassword the new master password
     * @return the re-opened facade backed by the new password
     */
    public static KeePass changeMasterPassword(File file, String oldPassword, String newPassword) {
        requirePassword(newPassword);
        KeePass kp = open(file, oldPassword);
        kp.save(file, newPassword);
        return kp;
    }

    // ------------------------------------------------------------------
    // Group (folder) operations
    // ------------------------------------------------------------------

    /**
     * Returns the group addressed by {@code path}, creating any missing
     * intermediate folders along the way. An empty/{@code null} path returns the
     * root group.
     *
     * @param path forward-slash separated folder path
     * @return the (possibly newly created) group
     */
    public JacksonGroup ensureGroup(String path) {
        return resolveGroup(path, true);
    }

    /**
     * Creates a sub-folder (and any missing parents) and returns it.
     *
     * @param path forward-slash separated folder path, e.g. {@code "Internet/Banking"}
     * @return the created (or already existing) leaf group
     */
    public JacksonGroup addGroup(String path) {
        if (isBlank(path)) {
            throw new IllegalArgumentException("Group path must not be empty");
        }
        return resolveGroup(path, true);
    }

    /**
     * Deletes the folder addressed by {@code path} together with all entries and
     * sub-folders it contains. The root group cannot be deleted.
     *
     * @param path forward-slash separated folder path
     * @return {@code true} if a matching group existed and was removed
     */
    public boolean deleteGroup(String path) {
        JacksonGroup group = resolveGroup(path, false);
        if (group == null || group.isRootGroup()) {
            return false;
        }
        JacksonGroup parent = group.getParent();
        if (parent == null) {
            return false;
        }
        parent.removeGroup(group);
        return true;
    }

    /**
     * Resolves a group by path without creating anything.
     *
     * @param path forward-slash separated folder path
     * @return the group, or {@code null} if it does not exist
     */
    public JacksonGroup findGroup(String path) {
        return resolveGroup(path, false);
    }

    private JacksonGroup resolveGroup(String path, boolean create) {
        JacksonGroup current = database.getRootGroup();
        for (String name : splitPath(path)) {
            JacksonGroup next = childGroupByName(current, name);
            if (next == null) {
                if (!create) {
                    return null;
                }
                next = current.addGroup(database.newGroup(name));
            }
            current = next;
        }
        return current;
    }

    private JacksonGroup childGroupByName(JacksonGroup parent, String name) {
        for (JacksonGroup g : parent.getGroups()) {
            if (name.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Entry operations (root group or any sub-folder)
    // ------------------------------------------------------------------

    /**
     * Inserts a new entry into the folder addressed by {@code groupPath},
     * creating intermediate folders as needed.
     *
     * @param groupPath folder path ({@code null}/empty = root group)
     * @param title     entry title
     * @param username  user name (may be {@code null})
     * @param password  password (may be {@code null})
     * @param url       URL (may be {@code null})
     * @param notes     notes (may be {@code null})
     * @return the created entry
     */
    public JacksonEntry addEntry(String groupPath, String title, String username,
                                 String password, String url, String notes) {
        if (isBlank(title)) {
            throw new IllegalArgumentException("Entry title must not be empty");
        }
        JacksonGroup group = resolveGroup(groupPath, true);
        JacksonEntry entry = database.newEntry();
        entry.setTitle(title);
        if (username != null) entry.setUsername(username);
        if (password != null) entry.setPassword(password);
        if (url != null) entry.setUrl(url);
        if (notes != null) entry.setNotes(notes);
        return group.addEntry(entry);
    }

    /**
     * Updates an existing entry identified by its UUID. {@code null} arguments
     * leave the corresponding field untouched.
     *
     * @param uuid     entry UUID
     * @param title    new title or {@code null}
     * @param username new user name or {@code null}
     * @param password new password or {@code null}
     * @param url      new URL or {@code null}
     * @param notes    new notes or {@code null}
     * @return {@code true} if the entry existed and was updated
     */
    public boolean updateEntry(UUID uuid, String title, String username,
                               String password, String url, String notes) {
        JacksonEntry entry = database.findEntry(uuid);
        if (entry == null) {
            return false;
        }
        if (title != null) entry.setTitle(title);
        if (username != null) entry.setUsername(username);
        if (password != null) entry.setPassword(password);
        if (url != null) entry.setUrl(url);
        if (notes != null) entry.setNotes(notes);
        return true;
    }

    /**
     * Deletes an entry by UUID, wherever it lives in the tree.
     *
     * @param uuid entry UUID
     * @return {@code true} if an entry was removed
     */
    public boolean deleteEntry(UUID uuid) {
        return database.deleteEntry(uuid);
    }

    /**
     * Deletes the first entry with the given title found directly inside the
     * folder addressed by {@code groupPath}.
     *
     * @param groupPath folder path ({@code null}/empty = root group)
     * @param title     entry title to remove
     * @return {@code true} if a matching entry was removed
     */
    public boolean deleteEntry(String groupPath, String title) {
        JacksonEntry entry = findEntry(groupPath, title);
        if (entry == null) {
            return false;
        }
        return database.deleteEntry(entry.getUuid());
    }

    /**
     * Finds the first entry with the given title directly inside the folder
     * addressed by {@code groupPath}.
     *
     * @param groupPath folder path ({@code null}/empty = root group)
     * @param title     entry title
     * @return the entry, or {@code null} if not found
     */
    public JacksonEntry findEntry(String groupPath, String title) {
        JacksonGroup group = resolveGroup(groupPath, false);
        if (group == null) {
            return null;
        }
        for (JacksonEntry e : group.getEntries()) {
            if (title != null && title.equals(e.getTitle())) {
                return e;
            }
        }
        return null;
    }

    /**
     * Looks up an entry by UUID anywhere in the database.
     *
     * @param uuid entry UUID
     * @return the entry, or {@code null} if not found
     */
    public JacksonEntry findEntry(UUID uuid) {
        return database.findEntry(uuid);
    }

    // ------------------------------------------------------------------
    // Attachment (file) operations
    // ------------------------------------------------------------------

    /**
     * Attaches a file to an entry. The bytes are stored in the database's shared
     * binary pool and referenced from the entry; compression is handled by the
     * library according to the target KDBX format.
     *
     * @param entryUuid target entry
     * @param name      attachment name (the file name shown in KeePass)
     * @param data      file contents
     * @throws KeePassException if the entry does not exist
     */
    public void addAttachment(UUID entryUuid, String name, byte[] data) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("Attachment name must not be empty");
        }
        Objects.requireNonNull(data, "data");
        JacksonEntry entry = requireEntry(entryUuid);
        entry.setBinaryProperty(name, data);
    }

    /**
     * Attaches an on-disk file to an entry, using the file's name as the
     * attachment name.
     *
     * @param entryUuid target entry
     * @param file      file to attach
     * @throws KeePassException if the entry does not exist or the file cannot be read
     */
    public void addAttachment(UUID entryUuid, File file) {
        requireFile(file);
        addAttachment(entryUuid, file.getName(), file);
    }

    /**
     * Attaches an on-disk file to an entry under an explicit name.
     *
     * @param entryUuid target entry
     * @param name      attachment name
     * @param file      file to attach
     * @throws KeePassException if the entry does not exist or the file cannot be read
     */
    public void addAttachment(UUID entryUuid, String name, File file) {
        requireFile(file);
        try {
            addAttachment(entryUuid, name, Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new KeePassException("Could not read attachment file: " + file, e);
        }
    }

    /**
     * Returns the raw bytes of an attachment.
     *
     * @param entryUuid entry holding the attachment
     * @param name      attachment name
     * @return the attachment bytes, or {@code null} if no such attachment exists
     */
    public byte[] getAttachment(UUID entryUuid, String name) {
        JacksonEntry entry = requireEntry(entryUuid);
        return entry.getBinaryProperty(name);
    }

    /**
     * Writes an attachment's bytes to {@code dest} on disk.
     *
     * @param entryUuid entry holding the attachment
     * @param name      attachment name
     * @param dest      destination file
     * @return {@code true} if the attachment existed and was written
     * @throws KeePassException on I/O error
     */
    public boolean extractAttachment(UUID entryUuid, String name, File dest) {
        Objects.requireNonNull(dest, "dest");
        byte[] data = getAttachment(entryUuid, name);
        if (data == null) {
            return false;
        }
        try {
            Files.write(dest.toPath(), data);
            return true;
        } catch (IOException e) {
            throw new KeePassException("Could not write attachment to: " + dest, e);
        }
    }

    /**
     * Lists the attachment names on an entry.
     *
     * @param entryUuid entry to inspect
     * @return the attachment names (never {@code null})
     */
    public List<String> listAttachments(UUID entryUuid) {
        JacksonEntry entry = requireEntry(entryUuid);
        return new ArrayList<>(entry.getBinaryPropertyNames());
    }

    /**
     * Removes an attachment from an entry.
     *
     * @param entryUuid entry holding the attachment
     * @param name      attachment name
     * @return {@code true} if an attachment was removed
     */
    public boolean deleteAttachment(UUID entryUuid, String name) {
        JacksonEntry entry = requireEntry(entryUuid);
        if (entry.getBinaryProperty(name) == null) {
            return false;
        }
        return entry.removeBinaryProperty(name);
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    /** @return the KDBX format this database will be written in */
    public Format getFormat() {
        return format;
    }

    /**
     * Sets the KDBX format used for the next {@link #save(File, String)}.
     *
     * @param format target format
     */
    public void setFormat(Format format) {
        this.format = Objects.requireNonNull(format, "format");
    }

    /** @return the database name (may be {@code null}) */
    public String getDatabaseName() {
        return database.getName();
    }

    /**
     * Sets the database name.
     *
     * @param name new name
     */
    public void setDatabaseName(String name) {
        database.setName(name);
    }

    /** @return the root group of the database */
    public JacksonGroup getRootGroup() {
        return database.getRootGroup();
    }

    /**
     * Exposes the underlying KeePassJava2 database for advanced use.
     *
     * @return the wrapped {@link JacksonDatabase}
     */
    public JacksonDatabase getDatabase() {
        return database;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private KdbxStreamFormat streamFormat() {
        int version = (format == Format.KDBX31) ? KDBX_VERSION_31 : KDBX_VERSION_4;
        return new KdbxStreamFormat(new KdbxHeader(version));
    }

    private static KdbxCreds creds(String password) {
        return new KdbxCreds(password.getBytes(StandardCharsets.UTF_8));
    }

    private JacksonEntry requireEntry(UUID entryUuid) {
        Objects.requireNonNull(entryUuid, "entryUuid");
        JacksonEntry entry = database.findEntry(entryUuid);
        if (entry == null) {
            throw new KeePassException("No entry with UUID " + entryUuid);
        }
        return entry;
    }

    private static List<String> splitPath(String path) {
        List<String> parts = new ArrayList<>();
        if (path == null) {
            return parts;
        }
        for (String segment : path.split(PATH_SEPARATOR)) {
            if (!segment.isEmpty()) {
                parts.add(segment);
            }
        }
        return parts;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void requireFile(File file) {
        Objects.requireNonNull(file, "file");
        if (!file.isFile() || !file.canRead()) {
            throw new KeePassException("File does not exist or is not readable: " + file);
        }
    }

    private static void requirePassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Master password must not be null");
        }
    }

    /** Unchecked exception for all KeePass facade failures. */
    public static class KeePassException extends RuntimeException {
        public KeePassException(String message) {
            super(message);
        }

        public KeePassException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
