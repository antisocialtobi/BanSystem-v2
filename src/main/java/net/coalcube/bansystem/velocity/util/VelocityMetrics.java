package net.coalcube.bansystem.velocity.util;

import net.coalcube.bansystem.core.util.MetricsAdapter;
import org.bstats.charts.CustomChart;
import org.bstats.velocity.Metrics;

public class VelocityMetrics implements MetricsAdapter {

    private final Metrics metrics;

    public VelocityMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void addCustomChart(CustomChart chart) {
        metrics.addCustomChart(chart);
    }
}
