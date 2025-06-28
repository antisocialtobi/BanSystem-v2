package net.coalcube.bansystem.spigot.util;

import net.coalcube.bansystem.core.util.MetricsAdapter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.CustomChart;

public class SpigotMetrics implements MetricsAdapter {

    private final Metrics metrics;

    public SpigotMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void addCustomChart(CustomChart chart) {
        metrics.addCustomChart(chart);
    }
}
