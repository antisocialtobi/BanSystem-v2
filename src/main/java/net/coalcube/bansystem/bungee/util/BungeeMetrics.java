package net.coalcube.bansystem.bungee.util;

import net.coalcube.bansystem.core.util.MetricsAdapter;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.CustomChart;

public class BungeeMetrics implements MetricsAdapter {

    private final Metrics metrics;

    public BungeeMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void addCustomChart(CustomChart chart) {
        metrics.addCustomChart(chart);
    }
}
