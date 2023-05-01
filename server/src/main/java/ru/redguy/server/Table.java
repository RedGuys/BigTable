package ru.redguy.server;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Table {
    //Существует условный сервер, на котором в оперативной памяти хранится очень длинная таблица (миллионы строк).
    //Строки отсортированы в некотором порядке, принцип сортировки может меняться, каждая строка имеет свой уникальный идентификатор.
    //Таблица живая, постоянно изменяется (несколько сотен/десятков изменений в секунду). В нее добавляются новые строки, обновляются и удаляются существующие.

    private CopyOnWriteArrayList<Person> records = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Integer> ids = new CopyOnWriteArrayList<>();
    private ServerSocketThread serverSocketThread = null;

    public Table() {
    }

    public void setServerSocketThread(ServerSocketThread serverSocketThread) {
        this.serverSocketThread = serverSocketThread;
    }

    public List<Person> getRecords(int from, int length) {
        //check out of bounds
        if (from < 0 || from > records.size()) {
            return new ArrayList<>();
        }
        if (from + length > records.size()) {
            length = records.size() - from;
        }
        return records.subList(from, from + length);
    }

    public void add(@NotNull Person person) {
        while (ids.contains(person.getId())) { //Protect from collisions
            person.setId((int) (Math.random() * 100000));
        }
        int i = 0;
        boolean added = false;
        for (Person person1 : records) { // Changable sort
            if (person1.getId() > person.getId()) {
                records.add(i, person);
                added = true;
                break;
            }
            i++;
        }
        if (!added) {
            records.add(person);
        }
        this.ids.add(person.getId());
        if (serverSocketThread != null) {
            serverSocketThread.distributePersonAdd(person);
        }
    }

    public Person getPerson(int id) {
        for (Person person : records) {
            if (person.getId() == id) {
                return person;
            }
        }
        return null;
    }

    public void remove(int id) {
        Person person = getPerson(id);
        if (person == null) {
            return;
        }
        records.remove(person);
        ids.remove((Integer) id);
        if (serverSocketThread != null) {
            serverSocketThread.distributePersonDelete(person);
        }
    }

    public void remove(Person person) {
        if(person == null) {
            return;
        }
        records.remove(person);
        ids.remove((Integer) person.getId());
        if (serverSocketThread != null) {
            serverSocketThread.distributePersonDelete(person);
        }
    }

    public Person getRandomPerson() {
        if(records.size() == 0) {
            return null;
        }
        return records.get((int) (Math.random() * records.size()));
    }
}
