package ch.epfl.sweng.partyup.containers;

public class Tuple<T, K> {
    public T object1;
    public K object2;
    public Tuple(){}
    public Tuple(T object1, K object2){
        this.object1 = object1;
        this.object2 = object2;
    }
}
