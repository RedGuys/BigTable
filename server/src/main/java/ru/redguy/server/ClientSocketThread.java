package ru.redguy.server;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//1 - set person list
//2 - add person to position
//3 - delete person
//4 - replace person

public class ClientSocketThread extends Thread {
    private final Table table;
    protected Socket socket;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;

    private int index = 0;
    private int height = 0;

    private CopyOnWriteArrayList<Person> clientPersons = new CopyOnWriteArrayList<>();

    public ClientSocketThread(Table table, @NotNull Socket socket) throws IOException {
        this.table = table;
        this.socket = socket;
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            while (true) {
                int command = inputStream.readInt();
                switch (command) {
                    case 1: {
                        height = inputStream.readInt();
                        List<Person> persons = table.getRecords(index, height);
                        outputStream.writeInt(1);
                        outputStream.writeInt(persons.size());
                        for (Person person : persons) {
                            person.write(outputStream);
                        }
                        clientPersons.addAll(persons);
                        break;
                    }
                    case 2: {
                        int direction = inputStream.readInt();
                        index += direction;
                        List<Person> persons = table.getRecords(index, height);
                        outputStream.writeInt(1);
                        outputStream.writeInt(persons.size());
                        for (Person person : persons) {
                            person.write(outputStream);
                        }
                        clientPersons = new CopyOnWriteArrayList<>(persons);
                        break;
                    }
                    case 3: {
                        String sort = inputStream.readUTF();
                        String order = inputStream.readUTF();
                        table.sort(sort, order);
                        List<Person> persons = table.getRecords(index, height);
                        outputStream.writeInt(1);
                        outputStream.writeInt(persons.size());
                        for (Person person : persons) {
                            person.write(outputStream);
                        }
                        clientPersons = new CopyOnWriteArrayList<>(persons);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void addPerson(Person person) {
        List<Person> viewPersons = table.getRecords(index, height);
        if (viewPersons.contains(person)) {
            //delete last person on screen
            try {
                if (clientPersons.size() >= height) {
                    outputStream.writeInt(3);
                    outputStream.writeInt(height - 1);
                    clientPersons.remove(height - 1);
                }
                outputStream.writeInt(2);
                outputStream.writeInt(viewPersons.indexOf(person));
                person.write(outputStream);
                clientPersons.add(viewPersons.indexOf(person), person);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void deletePerson(Person person) {
        if (clientPersons.contains(person)) {
            try {
                List<Person> viewPersons = table.getRecords(index, height);
                outputStream.writeInt(3);
                outputStream.writeInt(clientPersons.indexOf(person));
                clientPersons.remove(person);
                if (viewPersons.size() >= height) {
                    outputStream.writeInt(2);
                    outputStream.writeInt(height - 1);
                    viewPersons.get(height - 1).write(outputStream);
                    clientPersons.add(height - 1, viewPersons.get(height - 1));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updatePerson(Person person) {
        if (clientPersons.contains(person)) {
            try {
                outputStream.writeInt(4);
                outputStream.writeInt(clientPersons.indexOf(person));
                person.write(outputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
