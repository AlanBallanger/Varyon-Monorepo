package fr.varyon.readablebooks;

import fr.varyon.readablebooks.data.BookEntry;
import fr.varyon.readablebooks.files.BookFile;
import fr.varyon.readablebooks.util.FileUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import javax.annotation.Nonnull;

public final class BookManager {
    private static final BookManager INSTANCE = new BookManager();

    private final BookFile bookFile = new BookFile();
    private final KeySetView<UUID, Boolean> editModePlayers = ConcurrentHashMap.newKeySet();

    private BookManager() {
    }

    public static BookManager getInstance() {
        return INSTANCE;
    }

    public void load() {
        FileUtils.ensureMainDirectory();
        this.bookFile.syncLoad();
    }

    public void shutdown() {
        this.bookFile.syncSave();
    }

    public boolean isInEditMode(@Nonnull UUID playerUuid) {
        return this.editModePlayers.contains(playerUuid);
    }

    public boolean toggleEditMode(@Nonnull UUID playerUuid) {
        if (this.editModePlayers.contains(playerUuid)) {
            this.editModePlayers.remove(playerUuid);
            return false;
        }
        this.editModePlayers.add(playerUuid);
        return true;
    }

    public void clearEditMode(@Nonnull UUID playerUuid) {
        this.editModePlayers.remove(playerUuid);
    }

    public BookEntry getBook(@Nonnull String dimension, int x, int y, int z) {
        return this.bookFile.getBooks().get(BookEntry.key(dimension, x, y, z));
    }

    public void saveBook(
        @Nonnull String dimension, int x, int y, int z,
        @Nonnull String title, @Nonnull String text, String editor
    ) {
        BookEntry entry = new BookEntry(
            dimension, x, y, z, title, text, System.currentTimeMillis(), editor
        );
        this.bookFile.getBooks().put(entry.key(), entry);
        this.bookFile.syncSave();
    }

    public boolean removeBook(@Nonnull String dimension, int x, int y, int z) {
        BookEntry removed = this.bookFile.getBooks().remove(BookEntry.key(dimension, x, y, z));
        if (removed != null) {
            this.bookFile.syncSave();
            return true;
        }
        return false;
    }

    @Nonnull
    public List<BookEntry> getAllBooks() {
        return new ArrayList<>(this.bookFile.getBooks().values());
    }

    @Nonnull
    public List<BookEntry> getBooksInDimension(@Nonnull String dimension) {
        List<BookEntry> result = new ArrayList<>();
        this.bookFile.getBooks().values().forEach(entry -> {
            if (entry.dimension().equals(dimension)) {
                result.add(entry);
            }
        });
        return result;
    }

    public int count() {
        return this.bookFile.getBooks().size();
    }

    public void reload() {
        this.bookFile.syncLoad();
    }
}
