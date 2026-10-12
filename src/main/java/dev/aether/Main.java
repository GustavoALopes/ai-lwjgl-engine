package dev.aether; public final class Main{public static void main(String[]args){try(var e=Aether.createEngine()){e.start();e.tick(0);e.stop();}}}
