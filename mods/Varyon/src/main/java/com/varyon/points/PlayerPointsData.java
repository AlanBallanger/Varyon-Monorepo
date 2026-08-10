package com.varyon.points;

import java.util.UUID;

public record PlayerPointsData(UUID uuid, String name, double points) {
}
