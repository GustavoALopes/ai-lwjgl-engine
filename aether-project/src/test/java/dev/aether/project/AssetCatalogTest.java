package dev.aether.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AssetCatalogTest {
    @TempDir Path temporary;

    @Test void returnsImmutableDirectoryFirstSortedSnapshotsAndNavigatesRelativePaths() throws IOException {
        Files.createDirectory(temporary.resolve("z-dir"));
        Files.createDirectory(temporary.resolve("a-dir"));
        Files.writeString(temporary.resolve("z.txt"), "z");
        Files.writeString(temporary.resolve("a.txt"), "a");
        Files.writeString(temporary.resolve("a-dir/nested.txt"), "nested");
        var catalog = new AssetCatalog(temporary);
        var root = catalog.list(Path.of("."));
        assertEquals(List.of(Path.of("a-dir"), Path.of("z-dir"), Path.of("a.txt"), Path.of("z.txt")),
                root.stream().map(AssetCatalog.Entry::path).toList());
        assertTrue(root.getFirst().directory());
        assertFalse(root.getLast().directory());
        assertThrows(UnsupportedOperationException.class, () -> root.clear());
        assertEquals(Path.of("a-dir/nested.txt"), catalog.list(Path.of("a-dir")).getFirst().path());
        Files.writeString(temporary.resolve("new.txt"), "");
        assertEquals(4, root.size());
        assertEquals(5, catalog.list(Path.of(".")).size());
    }

    @Test void rejectsOutsidePathsMissingDirectoriesAndFileRoots() throws IOException {
        var catalog = new AssetCatalog(temporary);
        assertThrows(IllegalArgumentException.class, () -> catalog.list(Path.of("../outside")));
        assertThrows(IllegalArgumentException.class, () -> catalog.list(temporary.toAbsolutePath()));
        assertThrows(IOException.class, () -> catalog.list(Path.of("missing")));
        Path file = Files.writeString(temporary.resolve("file"), "");
        assertThrows(IOException.class, () -> new AssetCatalog(file));
    }
}
