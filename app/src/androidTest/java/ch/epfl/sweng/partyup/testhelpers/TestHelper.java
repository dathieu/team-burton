package ch.epfl.sweng.partyup.testhelpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.Collection;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.runner.lifecycle.Stage.RESUMED;

public class TestHelper {
    //don't realy care in test
    @SuppressLint("StaticFieldLeak")
    private static Activity currentActivity;

    /*Code taken from https://stackoverflow.com/questions/32387137/espresso-match-first-element-found-when-many-are-in-hierarchy#36866682
      Returns a matcher that matches the first view corresponding to a given matcher
     */
    public static <T> Matcher<T> first(final Matcher<T> matcher) {
        return new BaseMatcher<T>() {
            boolean isFirst = true;

            @Override
            public boolean matches(final Object item) {
                if (isFirst && matcher.matches(item)) {
                    isFirst = false;
                    return true;
                }

                return false;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("should return first matching item");
            }
        };
    }

    /*Code taken from http://qathread.blogspot.ch/2014/09/discovering-espresso-for-android-how-to.html
      Return activity currently active
     */
    public static Activity getActivityInstance(){
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                Collection resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(RESUMED);
                if (resumedActivities.iterator().hasNext()){
                    currentActivity = (Activity) resumedActivities.iterator().next();
                }
            }
        });

        return currentActivity;
    }


}
