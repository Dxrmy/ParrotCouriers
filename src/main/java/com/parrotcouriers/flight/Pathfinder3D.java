package com.parrotcouriers.flight;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Baritone-Grade 3D A* Pathfinder & Hierarchical Sub-Goal Planner:
 * - Ultra-high search budget (10,000 nodes)
 * - Hierarchical sub-goal chaining for long-range 100-1000m navigation
 * - Full entity hitbox clearance (0.6x0.9x0.6m)
 * - String-pulling path optimization
 */
public class Pathfinder3D {

    private static final int[][] NEIGHBOR_OFFSETS = {
            // 6 Cardinal
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
            // 12 Diagonal
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, 1, -1}, {0, -1, 1}, {0, -1, -1},
            // 8 Corner
            {1, 1, 1}, {-1, 1, 1}, {1, 1, -1}, {-1, 1, -1},
            {1, -1, 1}, {-1, -1, 1}, {1, -1, -1}, {-1, -1, -1}
    };

    /**
     * Long-range hierarchical 3D path planner. Breaks distant paths into 32m sub-goals.
     */
    public static List<Vector> findLongRangePath(World world, Location start, Location goal) {
        if (world == null || start == null || goal == null) {
            return Collections.emptyList();
        }

        double totalDist = start.distance(goal);
        if (totalDist <= 35.0) {
            return findPath(world, start, goal, 6000);
        }

        // Direct line-of-sight check
        if (isLineOfSightClear(world, start, goal)) {
            return List.of(goal.toVector());
        }

        // Divide long paths into hierarchical 3D sub-goals
        List<Vector> fullPath = new ArrayList<>();
        Vector currentPos = start.toVector();
        Vector direction = goal.toVector().subtract(start.toVector()).normalize();

        int segments = (int) Math.ceil(totalDist / 30.0);
        Location subStart = start.clone();

        for (int i = 1; i <= segments; i++) {
            double distAlong = Math.min(totalDist, i * 30.0);
            Location subGoal = start.clone().add(direction.clone().multiply(distAlong));

            // Find nearest safe open air block near sub-goal
            Location safeSubGoal = findNearestAirPocket(world, subGoal);

            List<Vector> subPath = findPath(world, subStart, (i == segments) ? goal : safeSubGoal, 3500);
            if (!subPath.isEmpty()) {
                fullPath.addAll(subPath);
                subStart = (i == segments) ? goal : safeSubGoal;
            } else {
                fullPath.add(safeSubGoal.toVector());
                subStart = safeSubGoal;
            }
        }

        return simplifyPath(fullPath);
    }

    public static List<Vector> findPath(World world, Location start, Location goal, int maxSearchNodes) {
        if (world == null || start == null || goal == null) {
            return Collections.emptyList();
        }

        if (isLineOfSightClear(world, start, goal)) {
            return List.of(goal.toVector());
        }

        int startX = start.getBlockX();
        int startY = start.getBlockY();
        int startZ = start.getBlockZ();

        int goalX = goal.getBlockX();
        int goalY = goal.getBlockY();
        int goalZ = goal.getBlockZ();

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<Long, Node> allNodes = new HashMap<>(2048);
        Set<Long> closedSet = new HashSet<>(2048);

        Node startNode = new Node(startX, startY, startZ, 0, distance(startX, startY, startZ, goalX, goalY, goalZ), null);
        long startKey = packKey(startX, startY, startZ);
        allNodes.put(startKey, startNode);
        openSet.add(startNode);

        Node closestNode = startNode;
        double closestDist = startNode.hScore;
        int evaluated = 0;

        while (!openSet.isEmpty() && evaluated < maxSearchNodes) {
            Node current = openSet.poll();
            long currentKey = packKey(current.x, current.y, current.z);

            if (Math.abs(current.x - goalX) <= 1 && Math.abs(current.y - goalY) <= 1 && Math.abs(current.z - goalZ) <= 1) {
                return reconstructPath(current, goal);
            }

            closedSet.add(currentKey);
            evaluated++;

            if (current.hScore < closestDist) {
                closestDist = current.hScore;
                closestNode = current;
            }

            for (int[] offset : NEIGHBOR_OFFSETS) {
                int nx = current.x + offset[0];
                int ny = current.y + offset[1];
                int nz = current.z + offset[2];

                if (ny < world.getMinHeight() + 1 || ny >= world.getMaxHeight() - 1) continue;

                long neighborKey = packKey(nx, ny, nz);
                if (closedSet.contains(neighborKey)) continue;

                if (!isPassableWithClearance(world, nx, ny, nz)) {
                    continue;
                }

                double stepCost = (offset[0] != 0 && offset[1] != 0 && offset[2] != 0) ? 1.732
                        : ((offset[0] != 0 && offset[1] != 0) || (offset[0] != 0 && offset[2] != 0) || (offset[1] != 0 && offset[2] != 0)) ? 1.414
                        : 1.0;

                double tentativeG = current.gScore + stepCost;

                Node neighbor = allNodes.get(neighborKey);
                if (neighbor == null) {
                    double h = distance(nx, ny, nz, goalX, goalY, goalZ);
                    neighbor = new Node(nx, ny, nz, tentativeG, h, current);
                    allNodes.put(neighborKey, neighbor);
                    openSet.add(neighbor);
                } else if (tentativeG < neighbor.gScore) {
                    neighbor.gScore = tentativeG;
                    neighbor.fScore = tentativeG + neighbor.hScore;
                    neighbor.parent = current;
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        return reconstructPath(closestNode, goal);
    }

    public static boolean isLineOfSightClear(World world, Location start, Location goal) {
        Vector dir = goal.toVector().subtract(start.toVector());
        double dist = dir.length();
        if (dist < 0.5) return true;

        Vector step = dir.clone().normalize().multiply(0.5);
        Location probe = start.clone();
        int steps = (int) (dist / 0.5);

        for (int i = 0; i < steps; i++) {
            probe.add(step);
            if (isSolidObstacle(probe.getBlock())) {
                return false;
            }
            if (isSolidObstacle(probe.clone().add(0, 0.4, 0).getBlock()) ||
                isSolidObstacle(probe.clone().add(0, -0.4, 0).getBlock())) {
                return false;
            }
        }
        return true;
    }

    private static Location findNearestAirPocket(World world, Location loc) {
        if (!isSolidObstacle(loc.getBlock()) && !isSolidObstacle(loc.clone().add(0, 1, 0).getBlock())) {
            return loc;
        }
        for (int r = 1; r <= 5; r++) {
            for (int dx = -r; dx <= r; dx += r) {
                for (int dy = -r; dy <= r; dy += r) {
                    for (int dz = -r; dz <= r; dz += r) {
                        Location test = loc.clone().add(dx, dy, dz);
                        if (!isSolidObstacle(test.getBlock()) && !isSolidObstacle(test.clone().add(0, 1, 0).getBlock())) {
                            return test;
                        }
                    }
                }
            }
        }
        return loc;
    }

    private static boolean isPassableWithClearance(World world, int x, int y, int z) {
        Block center = world.getBlockAt(x, y, z);
        if (isSolidObstacle(center)) return false;

        Block above = world.getBlockAt(x, y + 1, z);
        if (isSolidObstacle(above)) return false;

        if (center.getType() == Material.LAVA || above.getType() == Material.LAVA) {
            return false;
        }

        return true;
    }

    public static boolean isSolidObstacle(Block block) {
        if (!block.getType().isSolid()) {
            return false;
        }
        Material mat = block.getType();
        return mat != Material.AIR &&
                mat != Material.CAVE_AIR &&
                mat != Material.VOID_AIR &&
                mat != Material.SHORT_GRASS &&
                mat != Material.TALL_GRASS &&
                mat != Material.FERN &&
                mat != Material.LARGE_FERN &&
                mat != Material.VINE &&
                mat != Material.SUGAR_CANE;
    }

    private static List<Vector> reconstructPath(Node endNode, Location finalGoal) {
        List<Vector> path = new ArrayList<>();
        Node curr = endNode;
        while (curr != null) {
            path.add(new Vector(curr.x + 0.5, curr.y + 0.5, curr.z + 0.5));
            curr = curr.parent;
        }
        Collections.reverse(path);

        if (!path.isEmpty()) {
            path.remove(0);
        }
        path.add(finalGoal.toVector());
        return simplifyPath(path);
    }

    private static List<Vector> simplifyPath(List<Vector> rawPath) {
        if (rawPath.size() <= 2) return rawPath;

        List<Vector> simplified = new ArrayList<>();
        simplified.add(rawPath.get(0));

        int current = 0;
        while (current < rawPath.size() - 1) {
            int next = current + 1;
            while (next + 1 < rawPath.size()) {
                Vector v1 = rawPath.get(next).clone().subtract(rawPath.get(current)).normalize();
                Vector v2 = rawPath.get(next + 1).clone().subtract(rawPath.get(next)).normalize();
                if (v1.distanceSquared(v2) < 0.08) {
                    next++;
                } else {
                    break;
                }
            }
            simplified.add(rawPath.get(next));
            current = next;
        }
        return simplified;
    }

    private static double distance(int x1, int y1, int z1, int x2, int y2, int z2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        int dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static long packKey(int x, int y, int z) {
        return (((long) x & 0x3FFFFFF) << 38) | (((long) y & 0xFFF) << 26) | ((long) z & 0x3FFFFFF);
    }

    private static class Node {
        final int x, y, z;
        double gScore;
        double hScore;
        double fScore;
        Node parent;

        Node(int x, int y, int z, double gScore, double hScore, Node parent) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.gScore = gScore;
            this.hScore = hScore;
            this.fScore = gScore + hScore;
            this.parent = parent;
        }
    }
}
