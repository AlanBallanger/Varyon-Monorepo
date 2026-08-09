package fr.varyon.readablebooks.files;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.util.io.BlockingDiskFile;
import fr.varyon.readablebooks.data.BookEntry;
import fr.varyon.readablebooks.util.FileUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public final class BookFile extends BlockingDiskFile {
    private ConcurrentHashMap<String, BookEntry> books = new ConcurrentHashMap<>();

    public BookFile() {
        super(Path.of(FileUtils.BOOKS_PATH));
    }

    protected void read(BufferedReader reader) throws IOException {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        if (root != null) {
            this.books = new ConcurrentHashMap<>();
            JsonArray booksArray = root.getAsJsonArray("Books");
            if (booksArray != null) {
                booksArray.forEach(element -> {
                    JsonObject obj = element.getAsJsonObject();
                    String dimension = obj.get("Dimension").getAsString();
                    int x = obj.get("X").getAsInt();
                    int y = obj.get("Y").getAsInt();
                    int z = obj.get("Z").getAsInt();
                    String title = obj.has("Title") && !obj.get("Title").isJsonNull()
                        ? obj.get("Title").getAsString() : "";
                    String text = obj.has("Text") && !obj.get("Text").isJsonNull()
                        ? obj.get("Text").getAsString() : "";
                    long updatedTimestamp = obj.has("UpdatedTimestamp")
                        ? obj.get("UpdatedTimestamp").getAsLong() : System.currentTimeMillis();
                    String updatedBy = obj.has("UpdatedBy") && !obj.get("UpdatedBy").isJsonNull()
                        ? obj.get("UpdatedBy").getAsString() : null;
                    BookEntry entry = new BookEntry(dimension, x, y, z, title, text, updatedTimestamp, updatedBy);
                    this.books.put(entry.key(), entry);
                });
            }
        }
    }

    protected void write(BufferedWriter writer) throws IOException {
        JsonObject root = new JsonObject();
        JsonArray booksArray = new JsonArray();
        this.books.values().forEach(entry -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("Dimension", entry.dimension());
            obj.addProperty("X", entry.x());
            obj.addProperty("Y", entry.y());
            obj.addProperty("Z", entry.z());
            obj.addProperty("Title", entry.title());
            obj.addProperty("Text", entry.text());
            obj.addProperty("UpdatedTimestamp", entry.updatedTimestamp());
            if (entry.updatedBy() != null) {
                obj.addProperty("UpdatedBy", entry.updatedBy());
            }

            booksArray.add(obj);
        });
        root.add("Books", booksArray);
        writer.write(root.toString());
    }

    protected void create(BufferedWriter writer) throws IOException {
        JsonObject root = new JsonObject();
        root.add("Books", new JsonArray());
        writer.write(root.toString());
    }

    public ConcurrentHashMap<String, BookEntry> getBooks() {
        return this.books;
    }
}
