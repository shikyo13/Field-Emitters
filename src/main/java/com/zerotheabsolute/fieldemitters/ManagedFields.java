package com.zerotheabsolute.fieldemitters;
import com.zeromods.core.network.*;
import com.zeromods.core.neoforge.NetworkSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.*;
/** Legacy emitter fields migrate into the Core model the first time their chunks are observed. */
public final class ManagedFields {
    private static final String STORE="fieldemitters_core_networks";
    public static void refresh(ServerLevel level, List<EmitterEntity> loaded) {
        var data=NetworkSavedData.get(level,STORE);var directory=data.directory();var before=directory.snapshot();
        var components=new ArrayList<PhysicalNetworkReconciler.Component<BlockPos>>();var seen=new HashSet<BlockPos>();
        var ordered=loaded.stream().sorted(Comparator.comparingLong((EmitterEntity e)->e.placedAt).thenComparingLong(e->e.getBlockPos().asLong())).toList();
        for(var seed:ordered) {
            if(seen.contains(seed.getBlockPos())) continue;
            var parts=FieldNetwork.configurable(seed).stream().filter(e->Objects.equals(e.owner,seed.owner))
                .sorted(Comparator.comparingLong((EmitterEntity e)->e.placedAt).thenComparingLong(e->e.getBlockPos().asLong())).toList();
            var positions=parts.stream().map(EmitterEntity::getBlockPos).toList();seen.addAll(positions);
            var first=parts.getFirst();
            components.add(new PhysicalNetworkReconciler.Component<>(seed.owner,first.fieldName.isBlank()?"Field at "+first.getBlockPos().toShortString():first.fieldName,positions));
        }
        var observed=new HashSet<BlockPos>(seen);
        before.forEach(n->n.nodes().stream().filter(level::hasChunkAt).forEach(observed::add));
        var assignments=new PhysicalNetworkReconciler<>(directory,"fieldemitters:field").reconcile(components,observed);
        for(var emitter:loaded) emitter.managedNetwork=directory.get(assignments.get(emitter.getBlockPos())).orElse(null);
        if(!before.equals(directory.snapshot())) data.setDirty();
    }
    public static void rename(ServerLevel level, EmitterEntity emitter, String name) {
        if(emitter.managedNetwork==null) refresh(level,FieldNetwork.loaded(level));
        if(emitter.managedNetwork!=null) {
            emitter.managedNetwork.rename(name.isBlank()?"Field at "+emitter.managedNetwork.anchor().orElse(emitter.getBlockPos()).toShortString():name);
            NetworkSavedData.get(level,STORE).setDirty();
        }
    }
}
