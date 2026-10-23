package dev.aether.project;

import java.io.IOException;

/** Initializes a prepared session; register acquired resources with session.own immediately. */
@FunctionalInterface
public interface ProjectInitializer {
    void initialize(ProjectSession session) throws IOException;
}
