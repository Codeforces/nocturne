package org.nocturne.collection;

import org.apache.commons.lang.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 04.06.13
 */
@SuppressWarnings({"NonSerializableFieldInSerializableClass", "CloneableClassInSecureContext", "DeserializableClassInSecureContext"})
@NotThreadSafe
public final class SingleEntryList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
    private boolean hasValue;

    @Nullable
    private E value;

    public SingleEntryList() {
        // No operations.
    }

    public SingleEntryList(@Nullable E value) {
        this.hasValue = true;
        this.value = value;
    }

    public SingleEntryList(Collection<E> collection) {
        addAll(collection);
    }

    @Override
    public int size() {
        return hasValue ? 1 : 0;
    }

    @Override
    public boolean isEmpty() {
        return !hasValue;
    }

    @Override
    public boolean contains(Object o) {
        return hasValue && (o == null ? value == null : o.equals(value));
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
        return new SingleEntryListIterator<>(this);
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return hasValue ? new Object[]{value} : ArrayUtils.EMPTY_OBJECT_ARRAY;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        int arrayLength = a.length;

        if (hasValue) {
            if (arrayLength > 0) {
                try {
                    a[0] = (T) value;
                } catch (ClassCastException e) {
                    throw new ArrayStoreException(e.getLocalizedMessage());
                }

                for (int arrayIndex = 1; arrayIndex < arrayLength; ++arrayIndex) {
                    a[arrayIndex] = null;
                }

                return a;
            } else {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), 1);

                try {
                    a[0] = (T) value;
                } catch (ClassCastException e) {
                    throw new ArrayStoreException(e.getLocalizedMessage());
                }

                return a;
            }
        } else {
            for (int arrayIndex = 0; arrayIndex < arrayLength; ++arrayIndex) {
                a[arrayIndex] = null;
            }
            return a;
        }
    }

    @Override
    public boolean add(E e) {
        if (hasValue) {
            throw new IllegalStateException("Can't add more than one element to the list.");
        } else {
            hasValue = true;
            value = e;
            return true;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (hasValue) {
            if (contains(o)) {
                hasValue = false;
                value = null;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> collection) {
        for (Object o : collection) {
            if (!contains(o)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> collection) {
        boolean changed = false;

        for (E e : collection) {
            changed |= add(e);
        }

        return changed;
    }

    @Override
    public boolean addAll(int index, @Nonnull Collection<? extends E> collection) {
        if (index == 0) {
            return addAll(collection);
        } else {
            throw new IllegalStateException("List size is limited to 1.");
        }
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> collection) {
        boolean changed = false;

        for (Object o : collection) {
            changed |= remove(o);
        }

        return changed;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> collection) {
        if (hasValue) {
            if (collection.contains(value)) {
                return false;
            } else {
                hasValue = false;
                value = null;
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        hasValue = false;
        value = null;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size() + '.');
        }

        return value;
    }

    @Override
    public E set(int index, E element) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size() + '.');
        }

        E previousValue = value;
        value = element;
        return previousValue;
    }

    @Override
    public void add(int index, E element) {
        if (index == 0) {
            if (hasValue) {
                throw new IllegalStateException("Can't add more than one element to the list.");
            } else {
                hasValue = true;
                value = element;
            }
        } else {
            throw new IllegalStateException("List size is limited to 1.");
        }
    }

    @Override
    public E remove(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size() + '.');
        }

        E previousValue = value;
        hasValue = false;
        value = null;
        return previousValue;
    }

    @Override
    public int indexOf(Object o) {
        return contains(o) ? 0 : -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        return contains(o) ? 0 : -1;
    }

    @Nonnull
    @Override
    public ListIterator<E> listIterator() {
        return new SingleEntryListIterator<>(this);
    }

    @Nonnull
    @Override
    public ListIterator<E> listIterator(int index) {
        return new SingleEntryListIterator<>(this, index);
    }

    @Nonnull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override
    public SingleEntryList clone() {
        return new SingleEntryList<>(this);
    }

    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        outputStream.defaultWriteObject();
    }

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
    }

    private static final class SingleEntryListIterator<E> implements ListIterator<E> {
        private final SingleEntryList<E> singleEntryList;

        private int currentIndex;
        private int lastReturnedIndex = -1;

        private SingleEntryListIterator(SingleEntryList<E> singleEntryList) {
            this.singleEntryList = singleEntryList;
        }

        private SingleEntryListIterator(SingleEntryList<E> singleEntryList, int index) {
            if (index != 0) {
                throw new IllegalStateException("List size is limited to 1.");
            }

            this.singleEntryList = singleEntryList;
            this.currentIndex = index;
        }

        @Override
        public boolean hasNext() {
            return currentIndex != singleEntryList.size();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException("There is no next element.");
            }

            E element = singleEntryList.get(currentIndex);
            lastReturnedIndex = currentIndex;
            ++currentIndex;
            return element;
        }

        @Override
        public boolean hasPrevious() {
            return currentIndex != 0;
        }

        @Override
        public E previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException("There is no previous element.");
            }

            --currentIndex;
            E element = singleEntryList.get(currentIndex);
            lastReturnedIndex = currentIndex;
            return element;
        }

        @Override
        public int nextIndex() {
            return currentIndex;
        }

        @Override
        public int previousIndex() {
            return currentIndex - 1;
        }

        @Override
        public void remove() {
            if (lastReturnedIndex == -1) {
                throw new IllegalStateException("There is no element to remove.");
            }

            singleEntryList.remove(lastReturnedIndex);

            if (lastReturnedIndex < currentIndex) {
                --currentIndex;
            }

            lastReturnedIndex = -1;
        }

        @Override
        public void set(E e) {
            if (lastReturnedIndex == -1) {
                throw new IllegalStateException("There is no element to set.");
            }

            singleEntryList.set(lastReturnedIndex, e);
        }

        @Override
        public void add(E e) {
            if (singleEntryList.isEmpty() && currentIndex == 0) {
                singleEntryList.add(currentIndex++, e);
                lastReturnedIndex = -1;
            } else {
                throw new IllegalStateException("Can't add more than one element to the list.");
            }
        }
    }
}
