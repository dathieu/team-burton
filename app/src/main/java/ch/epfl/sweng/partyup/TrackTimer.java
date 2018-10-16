package ch.epfl.sweng.partyup;

import android.widget.SeekBar;

import java.util.Timer;
import java.util.TimerTask;

public class TrackTimer {
    private long oldElapsed;
    private long elapsed;
    private long start;
    private Timer timer;
    private SeekBar progressBar;
    private boolean isRunning;
    private final static int SECINMS = 1000;

    /**
     * constructor of the tracktimer
     *
     * @param bar the seekbar to use
     */
    public TrackTimer(SeekBar bar) {
        oldElapsed = 0;
        elapsed = 0;
        start = 0;
        progressBar = bar;
        isRunning = false;
    }

    /**
     * Reset the trackbar
     */
    public void reInitialize() {
        if (timer != null) {
            timer.cancel();
        }
        oldElapsed = 0;
        elapsed = 0;
        start = 0;
        progressBar.setProgress(0);
        isRunning = false;
    }

    /**
     * Start the trackbar
     */
    public void start() {
        if (!isRunning) {
            start = System.currentTimeMillis();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    progressBar.setProgress(progressBar.getProgress() + SECINMS);
                }
            }, 0, SECINMS);
            isRunning = true;
        }
    }

    /**
     * Pause the trackbar
     */
    public void pause() {
        if (timer != null) {
            timer.cancel();
        }
        oldElapsed = getElapsedTime();
        isRunning = false;
    }

    /**
     * Get the time elapsed from the start
     *
     * @return this time
     */
    private long getElapsedTime() {
        elapsed = System.currentTimeMillis() - start;
        return oldElapsed + elapsed;
    }
}