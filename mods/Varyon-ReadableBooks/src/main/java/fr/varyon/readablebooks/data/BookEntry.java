package fr.varyon.readablebooks.data;

import javax.annotation.Nonnull;

public record BookEntry(
    @Nonnull String dimension,
    int x,
    int y,
    int z,
    @Nonnull String title,
    @Nonnull String text,
    long updatedTimestamp,
    String updatedBy
) {
    public static String key(@Nonnull String dimension, int x, int y, int z) {
        return dimension + ":" + x + ":" + y + ":" + z;
    }

    @Nonnull
    public String key() {
        return key(this.dimension, this.x, this.y, this.z);
    }

    @Nonnull
    public BookEntry withContent(@Nonnull String newTitle, @Nonnull String newText, String editor) {
        return new BookEntry(
            this.dimension, this.x, this.y, this.z,
            newTitle, newText, System.currentTimeMillis(), editor
        );
    }
}
