package ch.epfl.sweng.partyup.dbstore.listeners;

public interface CompletionListener<T> {
    void onCompleted(T result);
}
