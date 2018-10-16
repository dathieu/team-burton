package ch.epfl.sweng.partyup.SpotDB;

import android.annotation.SuppressLint;
import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class SpotDBRequestQueue {
    //static because singleton and realy need the context
    @SuppressLint("StaticFieldLeak")
    private static SpotDBRequestQueue instance;
    private RequestQueue queue;
    private Context ctx;

    /**
     * Constructor of this class -> private because this is a singleton
     *
     * @param ctx the context in which the queue is created
     */
    private SpotDBRequestQueue(Context ctx) {
        this.ctx = ctx;
        queue = getRequestQueue();
    }

    /**
     * @return the request queue
     */
    public RequestQueue getRequestQueue() {
        if (queue == null) {
            queue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return queue;
    }

    /**
     * Get the only instance of this class
     *
     * @param ctx the context in which this is called
     * @return the queue instance
     */
    public static SpotDBRequestQueue getInstance(Context ctx) {
        if (instance == null) {
            instance = new SpotDBRequestQueue(ctx);
        }
        return instance;
    }

    /**
     * Add a request to the queue
     *
     * @param req the request to be add
     */
    public void addToRequestQueue(Request req) {
        getRequestQueue().add(req);
    }
}
