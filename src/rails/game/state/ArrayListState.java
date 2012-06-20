package rails.game.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.ImmutableList;

/**
 * A stateful version of an ArrayList
 * 
 * @author Erik Vos, Stefan Frey (V2.0)
 */

public final class ArrayListState<E> extends State implements Iterable<E>  {

    private final ArrayList<E> list;

    private ArrayListState() {
        list = new ArrayList<E>();
    }

    private ArrayListState(Collection<E> collection) {
        list = new ArrayList<E>(collection);
    }

    /** 
     * Creates empty ArrayListState 
     */
    public static <E> ArrayListState<E> create(){
        return new ArrayListState<E>();
    }
    
    /**
     * Creates a prefilled ArrayListState
     */
    public static <E> ArrayListState<E> create(Collection<E> collection){
        return new ArrayListState<E>(collection);
    }
    
    public void add(E element) {
        new ArrayListChange<E>(this, element);
    }

    public void add(int index, E element) {
        new ArrayListChange<E>(this, element, index);
    }

    public boolean remove(E element) {
        if (list.contains(element)) {
            new ArrayListChange<E>(this, list.indexOf(element));
            return true;
        } else {
            return false;
        }
    }

    public void move (E element, int toIndex) {
        if (remove (element)) add (toIndex, element);
    }
    
    public boolean contains (E element) {
        return list.contains(element);
    }

    public void clear() {
        for (E element:list) {
            remove(element);
        }
    }

    /**
     * returns immutable view of list
     */
    public ImmutableList<E> view() {
        return ImmutableList.copyOf(list);
    }

    public int size() {
        return list.size();
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public E get(int index) {
        return list.get(index);
    }
    
    public Iterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public String toString() {
        return list.toString();
    }

    void change(E object, int index, boolean addToList) {
        if (addToList) {
            list.add(index, object);
        } else {
            list.remove(index);
        }
    }

    
}