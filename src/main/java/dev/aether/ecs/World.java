package dev.aether.ecs;
import java.util.*;
/** Ordered ECS world with safe deferred mutations while systems are executing. */
public final class World {
 private final Map<Long,String> entities=new LinkedHashMap<>(); private final Set<Long> allocated=new HashSet<>(); private final Map<Class<?>,Map<Long,Component>> components=new HashMap<>(); private final List<WorldSystem> systems=new ArrayList<>(); private final List<Runnable> pending=new ArrayList<>(); private long nextId=1; private boolean updating;
 public Entity create(String name){Objects.requireNonNull(name); Entity e=new Entity(nextId++); allocated.add(e.id()); mutate(()->entities.put(e.id(),name)); return e;}
 public void destroy(Entity e){requireKnown(e); mutate(()->{allocated.remove(e.id());entities.remove(e.id()); components.values().forEach(s->s.remove(e.id()));});}
 public <T extends Component> void add(Entity e,T c){requireKnown(e); Objects.requireNonNull(c); mutate(()->components.computeIfAbsent(c.getClass(),x->new LinkedHashMap<>()).put(e.id(),c));}
 public <T extends Component> Optional<T> get(Entity e,Class<T> type){return Optional.ofNullable(type.cast(components.getOrDefault(type,Map.of()).get(e.id())));}
 public <T extends Component> Map<Entity,T> view(Class<T> type){Map<Entity,T> r=new LinkedHashMap<>(); components.getOrDefault(type,Map.of()).forEach((id,c)->{if(entities.containsKey(id))r.put(new Entity(id),type.cast(c));});return Collections.unmodifiableMap(r);}
 public List<Entity> entities(){return List.copyOf(entities.keySet().stream().map(Entity::new).toList());} public String nameOf(Entity e){requireKnown(e);return entities.get(e.id());}
 public boolean exists(Entity e){return e!=null&&entities.containsKey(e.id());} public int entityCount(){return entities.size();} public void addSystem(WorldSystem s){systems.add(Objects.requireNonNull(s));}
 public void update(double dt){if(!Double.isFinite(dt)||dt<0)throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");if(updating)throw new IllegalStateException("Nested update");updating=true;try{for(WorldSystem s:List.copyOf(systems))s.update(this,dt);}finally{updating=false;pending.forEach(Runnable::run);pending.clear();}}
 private void mutate(Runnable r){if(updating)pending.add(r);else r.run();} private void requireKnown(Entity e){if(e==null||!allocated.contains(e.id()))throw new IllegalArgumentException("Unknown entity: "+e);}
}
