package dev.aether.script;
import dev.aether.ecs.*;import java.util.*;
public final class ScriptSystem implements WorldSystem {private final Set<Long> initialized=new HashSet<>();public void update(World w,double dt){w.view(ScriptComponent.class).forEach((e,c)->{if(initialized.add(e.id()))c.script().onCreate(w,e);c.script().onUpdate(w,e,dt);});}}
