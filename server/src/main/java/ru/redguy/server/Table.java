package ru.redguy.server;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Table {
    //Существует условный сервер, на котором в оперативной памяти хранится очень длинная таблица (миллионы строк).
    //Строки отсортированы в некотором порядке, принцип сортировки может меняться, каждая строка имеет свой уникальный идентификатор.
    //Таблица живая, постоянно изменяется (несколько сотен/десятков изменений в секунду). В нее добавляются новые строки, обновляются и удаляются существующие.

    private static Random random = new Random();
    private static final int BATCH_SIZE = 1000000;
    private CopyOnWriteArrayList<CopyOnWriteArrayList<Person>> batches = new CopyOnWriteArrayList<>();
    private Set<Integer> ids = Collections.synchronizedSet(new HashSet<>());
    private ServerSocketThread serverSocketThread = null;

    public Table() {
        for (int i = 0; i < (Integer.MAX_VALUE / BATCH_SIZE) + 1; i++) {
            batches.add(new CopyOnWriteArrayList<>());
        }
    }

    public void setServerSocketThread(ServerSocketThread serverSocketThread) {
        this.serverSocketThread = serverSocketThread;
    }

    public List<Person> getRecords(int from, int length) {
        if (from < 0 || length < 0) {
            return new ArrayList<>();
        }

        int batchFrom = from / BATCH_SIZE;
        int batchTo = (from + length - 1) / BATCH_SIZE;
        int fromIndex = from % BATCH_SIZE;
        int toIndex = (from + length - 1) % BATCH_SIZE + 1;

        List<Person> result = new ArrayList<>();

        for (int i = batchFrom; i <= batchTo; i++) {
            int currentBatchSize = batches.get(i).size();

            if (currentBatchSize >= toIndex) {
                result.addAll(batches.get(i).subList(fromIndex, toIndex));
                break;
            }
            result.addAll(batches.get(i).subList(fromIndex, currentBatchSize));

            fromIndex = 0;
            toIndex -= currentBatchSize;
        }

        return result;
    }

    public void add(@NotNull Person person) {
        while (ids.contains(person.getId())) { //Protect from collisions
            person.setId(random.nextInt(Integer.MAX_VALUE));
        }

        int batch = person.getId() / BATCH_SIZE;
        CopyOnWriteArrayList<Person> batchList = batches.get(batch);
        int i = Collections.binarySearch(batchList, person, Comparator.comparing(Person::getId));
        if (i < 0) {
            batchList.add(-i - 1, person);
        }

        ids.add(person.getId());
        if (serverSocketThread != null) {
            serverSocketThread.distributePersonAdd(person);
        }
    }

    public Person getPerson(int id) {
        int batch = id / BATCH_SIZE;
        CopyOnWriteArrayList<Person> batchList = batches.get(batch);
        for (Person person : batchList) {
            if (person.getId() == id) {
                return person;
            }
        }
        return null;
    }

    public void remove(int id) {
        remove(getPerson(id));
    }

    public void remove(Person person) {
        if (person == null) {
            return;
        }
        int batch = person.getId() / BATCH_SIZE;
        CopyOnWriteArrayList<Person> batchList = batches.get(batch);
        int i = Collections.binarySearch(batchList, person, Comparator.comparing(Person::getId));
        if (i >= 0) {
            batchList.remove(i);
        }
        ids.remove(person.getId());
        if (serverSocketThread != null) {
            serverSocketThread.distributePersonDelete(person);
        }
    }

    public Person getRandomPerson() {
        int sum = 0;
        for (CopyOnWriteArrayList<Person> batch : batches) {
            sum += batch.size();
        }
        if (sum == 0) {
            return null;
        }
        Person p = null;
        while (p == null) {
            int batch = random.nextInt(batches.size());
            CopyOnWriteArrayList<Person> batchList = batches.get(batch);
            if (batchList.size() > 0) {
                p = batchList.get(random.nextInt(batchList.size()));
            }
        }
        return p;
    }

    public void update(Person person) {
        if (serverSocketThread != null) {
            serverSocketThread.distributePersonUpdate(person);
        }
    }
}
