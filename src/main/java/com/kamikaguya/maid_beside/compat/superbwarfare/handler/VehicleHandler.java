package com.kamikaguya.maid_beside.compat.superbwarfare.handler;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.entity.Entity;

public class VehicleHandler {
    public VehicleHandler() {
    }

    public static int getMaxPassengers(Entity entity) {
        if (entity instanceof VehicleEntity vehicleEntity) {
            return vehicleEntity.getMaxPassengers();
        } else
            return 2;
    }
}