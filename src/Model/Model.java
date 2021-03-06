package Model;

import javafx.beans.property.*;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.Socket;
import java.util.*;

import Algorithms.CorrelatedFeatures;
import Algorithms.SimpleAnomalyDetector;
import Algorithms.TimeSeries;
import Algorithms.TimeSeriesAnomalyDetector;

public class Model extends Observable {


    public TimeSeries timeSeries;

    public TimeSeries timeSeriesTest;
    public TimeSeriesAnomalyDetector anomalyDetector;

    public Timer timer = null;
    public IntegerProperty timestep;
    public IntegerProperty m_speed;
    public StringProperty trainPath, algoPath, testPath;
    public FloatProperty aileron, elevators, rudder, throttle;
    public FloatProperty altimeterValue, headingValue, pitchValue, rollValue, speedValue, yawValue;
    public LineChart<Number, Number> attGraph, corGraph;
    public IntegerProperty index;
    public FloatProperty valueLinear, valueCor;
    public StringProperty name2v;
    protected Thread playThread;


    public Model(IntegerProperty timestep) {
        this.timestep = timestep;

        m_speed = new SimpleIntegerProperty(1);
        trainPath = new SimpleStringProperty();
        algoPath = new SimpleStringProperty();
        testPath = new SimpleStringProperty();
        altimeterValue = new SimpleFloatProperty();
        headingValue = new SimpleFloatProperty();
        pitchValue = new SimpleFloatProperty();
        rollValue = new SimpleFloatProperty();
        speedValue = new SimpleFloatProperty();
        yawValue = new SimpleFloatProperty();
        aileron = new SimpleFloatProperty();
        elevators = new SimpleFloatProperty();
        rudder = new SimpleFloatProperty();
        throttle = new SimpleFloatProperty();

        index = new SimpleIntegerProperty();

        XYChart.Series<Number, Number> atSries = new XYChart.Series<>();
        XYChart.Series<Number, Number> corSries = new XYChart.Series<>();

        attGraph = new LineChart<>(new NumberAxis(), new NumberAxis());
        corGraph = new LineChart<>(new NumberAxis(), new NumberAxis());
        attGraph.getData().add(atSries);
        corGraph.getData().add(corSries);
        valueLinear = new SimpleFloatProperty();
        valueCor = new SimpleFloatProperty();
        name2v = new SimpleStringProperty();


    }


    public void play() {

        int x = 20;
        int s = 1000;
        int lenght = 0;
        if (timer == null)
        {
            timer = new Timer();

            if (timeSeries != null) {
                lenght = timeSeries.Lines_num;
                int finalLenght = lenght;
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("timestep = " + timestep.get());
                        if (finalLenght <= timestep.intValue()) {
                            stop();
                        }
                        float[] sArr = timeSeries.data[timestep.getValue()];

                        altimeterValue.setValue(sArr[25]);
                        headingValue.setValue(sArr[36]);
                        pitchValue.setValue(sArr[29]);
                        rollValue.setValue(sArr[28]);
                        speedValue.setValue(sArr[24]);
                        yawValue.setValue(sArr[21]);

                        aileron.setValue(sArr[0]);
                        elevators.setValue(sArr[1]);
                        rudder.setValue(sArr[2]);
                        throttle.setValue(sArr[6]);

                        if (index.get() != -1) {
                            valueLinear.setValue(sArr[index.get()]);
                            int i = getIndexFromTS(name2v.getName());
                            valueCor.setValue(sArr[i]);
                        }


                        timestep.set(timestep.get() + 1);

                    }
                }, 0, s / (x * m_speed.get()));

            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(null);
                alert.setContentText("You did not upload a CSV file");
                alert.showAndWait();

            }


        }

        setChanged();
        notifyObservers();

    }

    public void pause() {
        timer.cancel();
        timer = null;
        setChanged();
        notifyObservers();
    }

    public void stop() {
        timer.cancel();
        timer = null;
        timestep.set(0);
        setChanged();
        notifyObservers();
    }

    public void forward() {

        timestep.set(timestep.get() + 20);

        setChanged();
        notifyObservers();
    }

    public void backward() {
        int st = timestep.get();
        if (st > 20) {
            timestep.set(st - 20);
        } else {
            timestep.set(0);
        }
        setChanged();
        notifyObservers();
    }

    public void fastforward() {

        timestep.set(timestep.get() + 50);
        setChanged();
        notifyObservers();
    }

    public void fastbackward() {
        int st = timestep.get();
        if (st > 50) {
            timestep.set(st - 50);
        } else {
            timestep.set(0);
        }
        setChanged();
        notifyObservers();
    }

    public String[] loadCSV() {
        timeSeries = new TimeSeries(this.trainPath.getValue());
        return timeSeries.name;
    }

    public CorrelatedFeatures getCorroletedFeatur(String s) {

        int flag = 0;
        for (String s1 : timeSeries.name) {
            if (s1.equals(s))
                flag++;
        }
        if (flag == 0)
            return null;

        SimpleAnomalyDetector d = new SimpleAnomalyDetector();
        d.learnNormal(timeSeries);
        List<CorrelatedFeatures> corList = d.getNormalModel();

        CorrelatedFeatures mos = null;
        for (CorrelatedFeatures c : corList) {
            if ((c.feature1.equals(s)) || (c.feature2.equals(s))) {

                if (mos == null)
                    mos = c;
                else if (mos.corrlation < c.corrlation)
                    mos = c;
            }
        }

        return mos;
    }

    public int getIndexFromTS(String name) {
        int index = 0;
        for (int i = 0; i < timeSeries.name.length; i++) {
            if (timeSeries.name[i].equals(name))
                index = i;
        }
        return index;
    }


//-----------------------------------------------------------------------------------------------//


    public void connect() {
        playThread = new Thread(() -> {
         /*
        PUT THIS IN FLIGHTGEAR SETTINGS
        --generic=socket,in,10,127.0.0.1,5400,tcp,playback_small
        --fdm=null
         */
        Socket fg = null;
            try {
                fg = new Socket("localhost", 5400);
                BufferedReader in = new BufferedReader(new FileReader("C:/JetBrains/intellij/JavaFxLastProjectAMEN/collection/reg_flight.csv"));
                PrintWriter out = new PrintWriter(fg.getOutputStream());
                String line;
                if (out == null || fg == null)
                    System.out.println("hi");

                while ((line = in.readLine()) != null) {
                    out.println(line);
                    out.flush();
                    System.out.println(line);
                    Thread.sleep(100);
                }
                out.close();
                in.close();
                fg.close();

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });playThread.start();
    }

}
