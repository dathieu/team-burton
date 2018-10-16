package ch.epfl.sweng.partyup.dbstore.listeners;

public interface SchemaListener<T> {
    void onItemAdded(T item);
    void onItemChanged(T item);
    void onItemDeleted(T item);
}
