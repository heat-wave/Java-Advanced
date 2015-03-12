package ru.ifmo.ctddev.malimonov.arrayset;

import java.util.*;

/**
 * Created by heat_wave on 25.02.15.
 */
public class ArraySet<E extends Comparable<E>> implements SortedSet<E> {

    private List<E> items;
    private Comparator<? super E> comparator;
    private boolean sorted;

    private class SetIterator<T> implements Iterator<E> {
        private Iterator<E> iterator = items.iterator();

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public E next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("ArraySet class is immutable!");
        }
    }

    public ArraySet() {
        this.comparator = Comparator.naturalOrder();
        this.items = new ArrayList<>();
    }

    public ArraySet(Comparator<E> comparator) {
        this.comparator = comparator;
        this.items = new ArrayList<>();
    }

    public ArraySet(Collection<E> items) {
        this(items, Comparator.<E>naturalOrder());
    }

    public ArraySet(Collection<E> items, Comparator<? super E> comparator) {
        this.items = new ArrayList<E>();
        this.comparator = comparator;
        Set<E> set = new TreeSet<>(comparator);
        Iterator<E> iterator = items.iterator();
        while (iterator.hasNext()) {
            E current = iterator.next();
            if (!set.contains(current)) {
                this.items.add(current);
                set.add(current);
            }
        }
        Collections.sort(this.items, comparator);
    }

    private ArraySet(List<E> items, Comparator<? super E> comparator, boolean sorted) {

        if (sorted) {
            this.items = items;
            this.comparator = comparator;
        }
        else {
            throw new UnsupportedOperationException(
                    "Calling a private constructor on an unsorted subset is unacceptable!");
        }
    }

    @Override
    public Comparator<? super E> comparator() {
        if (this.comparator == Comparator.naturalOrder()) {
            return null;
        }
        else {
            return comparator;
        }
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        int head = expectedPosition(fromElement);
        int tail = expectedPosition(toElement);

        return partialSet(head, tail);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        int tail = expectedPosition(toElement);

        return partialSet(0, tail);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        int head = expectedPosition(fromElement);
        return partialSet(head, size());
    }

    private int expectedPosition(E element) {
        int pos = Collections.binarySearch(items, element, comparator);

        if (pos < 0) {
            return (-pos - 1);
        }
        return pos;
    }

    private SortedSet<E> partialSet(int head, int tail) {
        return new ArraySet<E>(items.subList(head, tail), comparator, true);
    }

    @Override
    public E first() {
        if (!this.isEmpty()) {
            return items.get(0);
        }
        else {
            throw new NoSuchElementException("No first element for you here!");
        }
    }

    @Override
    public E last() {
        if (!this.isEmpty()) {
            return items.get(this.size() - 1);
        }
        else {
            throw new NoSuchElementException("No last element for you here!");
        }
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return (items.size() == 0);
    }

    @Override
    @SuppressWarnings("unchecked") // Checking if a collection of type T contains
    // an item of type T should not fail here.
    public boolean contains(Object o) {
        return (Collections.binarySearch(items, ((E) o), comparator) >= 0);
    }

    @Override
    public Iterator<E> iterator() {
        return new SetIterator<E>();
    }

    @Override
    public Object[] toArray() {
        return items.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return items.toArray(a);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("ArraySet class is immutable!");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("ArraySet class is immutable!");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Iterator<E> iterator = this.iterator();
        while (iterator.hasNext()) {
            if (!this.contains(iterator.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException("ArraySet class is immutable!");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("ArraySet class is immutable!");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("ArraySet class is immutable!");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("ArraySet class is immutable!");
    }
}
