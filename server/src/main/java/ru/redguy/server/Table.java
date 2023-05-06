package ru.redguy.server;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Table {
    //Существует условный сервер, на котором в оперативной памяти хранится очень длинная таблица (миллионы строк).
    //Строки отсортированы в некотором порядке, принцип сортировки может меняться, каждая строка имеет свой уникальный идентификатор.
    //Таблица живая, постоянно изменяется (несколько сотен/десятков изменений в секунду). В нее добавляются новые строки, обновляются и удаляются существующие.

    private static Random random = new Random();
    private Set<Integer> ids = Collections.synchronizedSet(new HashSet<>());
    private ServerSocketThread serverSocketThread = null;
    private boolean lock = false;
    private SortedMap<Object, List<Person>> sort = new TreeMap<>(Comparator.comparing((p) -> {
        if (p instanceof Person)
            return ((Person) p).getId();
        return (Integer) p;
    }));
    private String sortKey = "id";

    public Table() {
    }

    public void setServerSocketThread(ServerSocketThread serverSocketThread) {
        this.serverSocketThread = serverSocketThread;
    }

    public List<Person> getRecords(int from, int length) {
        //wait for unlock
        while (lock) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
        if (from < 0 || length < 0) {
            return new ArrayList<>();
        }

        List<Person> result = new ArrayList<>();

        // Calculate the key that corresponds to the element at the given index
        Object fromKey = null;
        int i = 0;
        for (Object key : sort.keySet()) {
            List<Person> list = sort.get(key);
            int listSize = list.size();
            if (i + listSize > from) {
                fromKey = key;
                break;
            }
            i += listSize;
        }

        // If fromKey is null, there are no elements to retrieve
        if (fromKey == null) {
            return result;
        }

        // Calculate the end index of the sublist
        int to = from + length;

        // Iterate over the values of the submap and add the elements to the result list
        int j = 0;
        for (List<Person> list : sort.subMap(fromKey, sort.lastKey()).values()) {
            for (Person person : list) {
                if (i + j >= from && i + j < to) {
                    result.add(person);
                }
                j++;
            }
            if (i + j >= to) {
                break;
            }
        }

        return result;
    }

    public void add(@NotNull Person person) {
        //wait for unlock
        while (lock) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
        while (ids.contains(person.getId())) { //Protect from collisions
            person.setId(random.nextInt(Integer.MAX_VALUE));
        }

        //add to sort
        Object key = switch (sortKey) {
            case "id" -> person.getId();
            case "firstName" -> person.getFirstName();
            case "lastName" -> person.getLastName();
            case "age" -> person.getAge();
            default -> null;
        };
        synchronized (sort) {
            List<Person> list = sort.get(key);
            if (list == null) {
                list = new ArrayList<>();
                sort.put(key, list);
            }
            list.add(person);
        }

        ids.add(person.getId());
        if (serverSocketThread != null) {
            serverSocketThread.distributePersonAdd(person);
        }
    }

    public void remove(Person person) {
        //wait for unlock
        while (lock) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
        if (person == null) {
            return;
        }

        //remove from sort
        Object key = switch (sortKey) {
            case "id" -> person.getId();
            case "firstName" -> person.getFirstName();
            case "lastName" -> person.getLastName();
            case "age" -> person.getAge();
            default -> null;
        };
        List<Person> list = sort.get(key);
        if (list != null) {
            list.remove(person);
        }

        ids.remove(person.getId());
        if (serverSocketThread != null) {
            serverSocketThread.distributePersonDelete(person);
        }
    }

    public Person getRandomPerson() {
        //wait for unlock
        while (lock) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
        Object key = sort.keySet().toArray()[random.nextInt(sort.size())];
        List<Person> list = sort.get(key);
        return list.get(random.nextInt(list.size()));
    }

    public void update(Person person) {
        //wait for unlock
        while (lock) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
        if (person == null) {
            return;
        }
        //remove from sort, without use key
        for (List<Person> list : sort.values()) {
            list.remove(person);
        }

        Object key = switch (sortKey) {
            case "id" -> person.getId();
            case "firstName" -> person.getFirstName();
            case "lastName" -> person.getLastName();
            case "age" -> person.getAge();
            default -> null;
        };
        synchronized (sort) {
            List<Person> list = sort.get(key);
            if (list == null) {
                list = new ArrayList<>();
                sort.put(key, list);
            }
            list.add(person);
        }
        if (serverSocketThread != null) {
            serverSocketThread.distributePersonUpdate(person);
        }
    }

    /**
     * Sorts table by property
     *
     * @param property id, firstName, lastName, age
     * @param order    asc, desc
     */
    public void sort(@NotNull String property, String order) {
        Comparator<Object> comparator = switch (property) {
            case "id" -> Comparator.comparing((p) -> {
                if (p instanceof Person)
                    return ((Person) p).getId();
                return (Integer) p;
            });
            case "firstName" -> Comparator.comparing((p) -> {
                if (p instanceof Person)
                    return ((Person) p).getFirstName();
                return (String) p;
            });
            case "lastName" -> Comparator.comparing((p) -> {
                if (p instanceof Person)
                    return ((Person) p).getLastName();
                return (String) p;
            });
            case "age" -> Comparator.comparing((p) -> {
                if (p instanceof Person)
                    return ((Person) p).getAge();
                return (Integer) p;
            });
            default -> null;
        };
        if (comparator == null) {
            return;
        }
        if (order.equals("desc")) {
            comparator = comparator.reversed();
        }
        lock = true;
        sortKey = property;
        List<Person> persons = new ArrayList<>();
        for (List<Person> list : sort.values()) {
            persons.addAll(list);
        }
        sort = new TreeMap<>(comparator);
        for (Person person : persons) {
            Object key = switch (property) {
                case "id" -> person.getId();
                case "firstName" -> person.getFirstName();
                case "lastName" -> person.getLastName();
                case "age" -> person.getAge();
                default -> null;
            };
            if (!sort.containsKey(key)) {
                sort.put(key, new ArrayList<>());
            }
            sort.get(key).add(person);
        }
        lock = false;
    }
}
