package ch.epfl.sweng.partyup.dbstore;


public class ConnectionProvider {

    /**
     * the modes the connection can operate:
     * runtime: normal mode used in release
     * test: mock connection for test
     */
    public enum Mode{RUNTIME,TEST}

    static Mode mode =Mode.RUNTIME;

    /**
     * change the working mode of the provided connection
     * @param newMode the new mode of operation
     */
    public static void setMode(Mode newMode){
        mode = newMode;
    }

    /**
     * provide a connection
     *
     * @return the connection object
     */
    public static Connection getConnection(){
        if(mode==Mode.RUNTIME)
            return FirebaseConnection.getInstance();
        else
            return MockedConnection.getInstance();
    }
}
