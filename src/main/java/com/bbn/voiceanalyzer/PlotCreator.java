package com.bbn.voiceanalyzer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;

import java.io.IOException;
import java.util.ArrayList;

public class PlotCreator {

    public void createStat(JSONArray conversations) {
        // Create Chart
        final XYChart chart = new XYChartBuilder().width(1000).height(400).title("Connected Time Per Conversation").xAxisTitle("Conversations").yAxisTitle("Hours").build();

        // Customize Chart
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);
        chart.getStyler().setLegendVisible(false);

        chart.getStyler().setYAxisMin(0.0);

        // Series
        ArrayList<Double> doubles = new ArrayList<>();
        for (int i = 0; i < conversations.length(); i++) {
            JSONObject conversationobj = conversations.getJSONObject(i);
            Conversation conversation = new Conversation(conversationobj);
            if (conversationobj.has("startTime") && (conversationobj.has("endTime") || i == conversations.length() - 1)) {
                doubles.add((double) (Long.parseLong(conversation.getEndTime()) - Long.parseLong(conversation.getStartTime())) / 1000 / 60 / 60);
            }
        }
        chart.addSeries("Conversation", doubles.stream().mapToDouble(d -> d).toArray());

        chart.getStyler().setYAxisMax(doubles.stream().sorted().reduce((first, second) -> second).get());

        // Save it
        try {
            BitmapEncoder.saveBitmap(chart, "./Chart", BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createStatstop(JSONArray data) {
        // Create Chart
        final XYChart chart = new XYChartBuilder().width(1000).height(400).title("Statstop Graph").xAxisTitle("% of Starttime").yAxisTitle("% of Hours").build();

        // Customize Chart
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);

        // Series
        long higheststarttime = 0;
        long loweststarttime = 0;
        long highestsum = 0;
        for (Object memberobj : data) {
            JSONObject member = (JSONObject) memberobj;
            JSONArray conversations = new JSONArray(member.getString("conversations"));
            long sum = 0;
            for (int i = 0; i < conversations.length(); i++) {
                JSONObject conversationobj = conversations.getJSONObject(i);
                Conversation conversation = new Conversation(conversationobj);
                if (conversationobj.has("startTime") && (conversationobj.has("endTime") || i == conversations.length() - 1)) {
                    long start = Long.parseLong(conversation.getStartTime());
                    if (start < loweststarttime || loweststarttime == 0) loweststarttime = start;
                    long end = Long.parseLong(conversation.getEndTime());
                    if (higheststarttime < end) higheststarttime = end;
                    sum += end - start;
                }
            }
            if (highestsum < sum) highestsum = sum;
        }

        long totaltime = higheststarttime - loweststarttime;
        for (Object memberobj : data) {
            JSONObject member = (JSONObject) memberobj;
            JSONArray conversations = new JSONArray(member.getString("conversations"));
            ArrayList<Double> starttimes = new ArrayList<>();
            ArrayList<Double> sums = new ArrayList<>();
            long sum = 0;
            for (int i = 0; i < conversations.length(); i++) {
                JSONObject conversationobj = conversations.getJSONObject(i);
                Conversation conversation = new Conversation(conversationobj);
                if (conversationobj.has("startTime") && (conversationobj.has("endTime") || i == conversations.length() - 1)) {
                    long starttime = Long.parseLong(conversation.getStartTime()) - loweststarttime;
                    double timepercent = ((double) starttime / (double) totaltime) * 100;
                    starttimes.add(timepercent);

                    long endtime = Long.parseLong(conversation.getEndTime()) - loweststarttime;
                    double endtimepercent = ((double) endtime / (double) totaltime) * 100;
                    starttimes.add(endtimepercent);

                    sums.add(((double) sum / (double) highestsum) * 100);

                    long talktime = Long.parseLong(conversation.getEndTime())
                            - Long.parseLong(conversation.getStartTime())
                            - getSum(conversation.getSleepTimes(), conversation.getEndTime())
                            - getSum(conversation.getMuteTimes(), conversation.getEndTime())
                            - getSum(conversation.getDeafTimes(), conversation.getEndTime())
                            - getSum(conversation.getIdleTimes(), conversation.getEndTime());
                    sum += talktime;
                    double sumpercent = ((double) sum / (double) highestsum) * 100;
                    sums.add(sumpercent);
                }
            }
            chart.addSeries(member.getString("Tag"), starttimes.stream().mapToDouble(d -> d).toArray(), sums.stream().mapToDouble(d -> d).toArray());
        }

        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(100.0);
        chart.getStyler().setXAxisMin(0.0);
        chart.getStyler().setXAxisMax(100.0);

        // Save it
        try {
            BitmapEncoder.saveBitmap(chart, "./Chart", BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getSum(String[] data, String endtime) {
        long sum = 0;
        if (data == null) return 0;
        if (endtime == null) endtime = String.valueOf(System.currentTimeMillis());
        for (String dat : data) {
            if (dat.endsWith("-")) dat += endtime;
            sum += Long.parseLong(dat.split("-")[1]) - Long.parseLong(dat.split("-")[0]);
        }
        return sum;
    }

}
