package dev.aether.project;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

final class ProjectPaths {
    private ProjectPaths() { }

    static Path relative(Path path, String field, boolean file) {
        Objects.requireNonNull(path, field);
        String portable = path.toString().replace('\\', '/');
        if (portable.isBlank() || portable.startsWith("/") || portable.contains(":"))
            throw new IllegalArgumentException(field + " must be a relative project path");
        Path normalized = Path.of(portable).normalize();
        if (normalized.isAbsolute() || normalized.startsWith(".."))
            throw new IllegalArgumentException(field + " must stay inside the project directory");
        if (file && (normalized.toString().isEmpty() || normalized.toString().equals(".")))
            throw new IllegalArgumentException(field + " must identify a file");
        return normalized;
    }

    static Path manifest(Path path) {
        Objects.requireNonNull(path, "manifestFile");
        Path absolute = path.toAbsolutePath().normalize();
        if (absolute.getFileName() == null
                || !absolute.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".aetherproj"))
            throw new IllegalArgumentException("Project file must have the .aetherproj extension");
        return absolute;
    }

    static String portable(Path path) {
        String value = path.toString().replace('\\', '/');
        return value.isEmpty() ? "." : value;
    }
}
