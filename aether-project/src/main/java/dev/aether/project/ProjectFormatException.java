package dev.aether.project;

import java.io.IOException;
import java.nio.file.Path;

/** Invalid JSON, unsupported format version, or invalid manifest fields. */
public final class ProjectFormatException extends IOException {
    public ProjectFormatException(Path file, Throwable cause) {
        super("Invalid project file " + file + ": " + cause.getMessage(), cause);
    }
}
