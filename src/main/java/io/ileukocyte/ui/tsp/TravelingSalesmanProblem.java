package io.ileukocyte.ui.tsp;

import java.util.*;
import java.util.stream.Collectors;

public class TravelingSalesmanProblem {
    public static void main(String[] args) {
        var cities = new ArrayList<City>();
        var cityCount = new Random()
                .ints(20, 41)
                .findFirst()
                .orElseThrow();

        for (int i = 0; i < cityCount; i++) {
            var x = (int) (Math.random() * 200);
            var y = (int) (Math.random() * 200);

            cities.add(new City(x, y));
        }

        System.out.printf("The following city sequence (%d cities) has been generated: %s\n", cities.size(), cities);

        var tabu = tabuSearch(cities, 50_000, 50, 15);
        var sa = simulatedAnnealing(cities, 100,  0.1, 0.05);

        System.out.printf("Tabu search result (%d ms, %d iterations, path length: %f): %s\nIndices: %s\n", tabu.timeMs(), tabu.iterationCount(), tabu.totalSolutionDistance(), tabu.solution(), tabu.indices());
        System.out.printf("Simulated annealing result (%d ms, %d iterations, path length: %f): %s\nIndices: %s\n", sa.timeMs(), sa.iterationCount(), sa.totalSolutionDistance(), sa.solution(), sa.indices());
    }

    public static Result tabuSearch(
            List<City> cities,
            int maxIterations,
            int maxWithoutImprovement,
            int maxTabuStates
    ) {
        var time = System.currentTimeMillis();
        var iterations = 0;
        var withoutImprovement = 0;

        var tabu = new ArrayList<>();

        var current = initialSolution(cities);
        var best = new ArrayList<>(current);

        // The terminating conditions
        while (iterations < maxIterations && withoutImprovement < maxWithoutImprovement) {
            var neighborhood = swapNeighbors(current);

            // Aspiration criteria (i.e., even a tabu state is allowed in case it improves the current solution)
            if (!tabu.contains(neighborhood) || totalDistance(neighborhood) < totalDistance(best)) {
                current = new ArrayList<>(neighborhood);

                if (totalDistance(current) < totalDistance(best)) {
                    best = new ArrayList<>(current);
                }

                tabu.add(neighborhood);

                if (tabu.size() > maxTabuStates) {
                    tabu.remove(0);
                }

                withoutImprovement = 0;
            } else {
                withoutImprovement++;
            }

            iterations++;
        }

        time = System.currentTimeMillis() - time;

        return new Result(cities, best, iterations, time);
    }

    public static Result simulatedAnnealing(
            List<City> cities,
            double initialTemperature,
            double minTemperature,
            double coolingRate
    ) {
        var time = System.currentTimeMillis();
        var iterations = 0;

        var current = initialSolution(cities);
        var best = new ArrayList<>(current);

        while (initialTemperature > minTemperature) {
            var neighborhood = swapNeighbors(current);

            // If negative, then `neighborhood` is a better solution than the current one
            var delta = totalDistance(neighborhood) - totalDistance(current);

            // If the new solution isn't better, then the probability of accepting the worse solution is checked
            if (delta < 0 || Math.random() < Math.exp(-delta / initialTemperature)) {
                current = neighborhood;

                if (totalDistance(neighborhood) < totalDistance(best)) {
                    best = new ArrayList<>(current);
                }
            }

            // The temperature is decreased by ((1 - coolingRate) * 100) percent
            initialTemperature *= 1 - coolingRate;

            iterations++;
        }

        time = System.currentTimeMillis() - time;

        return new Result(cities, best, iterations, time);
    }

    // The nearest neighbor heuristic
    // A random city is picked as a starting point
    private static List<City> initialSolution(List<City> cities) {
        var unvisited = new ArrayList<>(cities);
        var current = unvisited.remove(new Random().nextInt(unvisited.size()));

        var solution = new ArrayList<City>();

        solution.add(current);

        while (!unvisited.isEmpty()) {
            City nearest = null;

            var minDistance = Double.MAX_VALUE;

            for (var city : unvisited) {
                var distance = city.euclidDistance(current);

                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = city;
                }
            }

            if (nearest != null) {
                unvisited.remove(nearest);
                solution.add(nearest);

                current = nearest;
            }
        }

        return solution;
    }

    // Successors are generated as vectors in which a random pair of cities is swapped
    private static List<City> swapNeighbors(List<City> currentSolution) {
        var neighbor = new ArrayList<>(currentSolution);

        var first = new Random().nextInt(neighbor.size());
        var second = (first + 1) % neighbor.size();

        Collections.swap(neighbor, first, second);

        return neighbor;
    }

    // The length of the entire path
    public static double totalDistance(List<City> path) {
        var total = 0.0;

        for (int i = 0; i < path.size() - 1; i++) {
            var first = path.get(i);
            var second = path.get(i + 1);

            total += first.euclidDistance(second);
        }

        return total + path.get(0).euclidDistance(path.get(path.size() - 1));
    }

    public record Result(
            List<City> input,
            List<City> solution,
            int iterationCount,
            long timeMs
    ) {
        public List<Integer> indices() {
            return solution.stream().map(input::indexOf).collect(Collectors.toList());
        }

        public double totalSolutionDistance() {
            return totalDistance(solution);
        }
    }
}
