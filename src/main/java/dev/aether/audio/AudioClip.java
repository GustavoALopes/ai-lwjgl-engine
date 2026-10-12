package dev.aether.audio; import java.nio.file.Path; import java.util.Objects;
public record AudioClip(Path source){public AudioClip{Objects.requireNonNull(source);}}
