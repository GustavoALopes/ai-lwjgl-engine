package dev.aether.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** On-demand directory snapshots. Symbolic links are not followed by the asset browser. */
public final class AssetCatalog {
    public record Entry(Path path, boolean directory) {
        public Entry { Objects.requireNonNull(path, "path"); }
    }

    private final Path root;

    public AssetCatalog(Path root) throws IOException {
        this.root = Objects.requireNonNull(root, "root").toRealPath();
        if (!Files.isDirectory(this.root))
            throw new IOException("Asset root is not a directory: " + root);
    }

    public List<Entry> list(Path relativeDirectory) throws IOException {
        Path relative = ProjectPaths.relative(relativeDirectory, "asset directory", false);
        Path directory = root.resolve(relative).normalize();
        if (!directory.toRealPath().startsWith(root))
            throw new IOException("Asset directory resolves outside the project: " + relativeDirectory);
        try (var files = Files.list(directory)) {
            return files.filter(path -> !Files.isSymbolicLink(path))
                    .map(path -> new Entry(root.relativize(path),
                            Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)))
                    .sorted(Comparator.comparing(Entry::directory).reversed()
                            .thenComparing(entry -> ProjectPaths.portable(entry.path())))
                    .toList();
        }
    }
}
