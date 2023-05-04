package ru.redguy.gui.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

//1 - update client info
//2 - move view

public class ClientSocketThread extends Thread {
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    public ObservableList<Person> persons = FXCollections.observableArrayList();

    public ClientSocketThread(@NotNull Socket socket) throws IOException {
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            while (true) {
                int command = inputStream.readInt();
                System.out.println(command);
                switch (command) {
                    case 1: {
                        int size = inputStream.readInt();
                        persons.clear();
                        for (int i = 0; i < size; i++) {
                            persons.add(Person.read(inputStream));
                        }
                        break;
                    }
                    case 2: {
                        int index = inputStream.readInt();
                        persons.add(index, Person.read(inputStream));
                        break;
                    }
                    case 3: {
                        int index = inputStream.readInt();
                        if (index < persons.size())
                            persons.remove(index);
                        break;
                    }
                    case 4: {
                        int index = inputStream.readInt();
                        if (index < persons.size())
                            persons.set(index, Person.read(inputStream));
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendClientInfo() throws IOException {
        outputStream.writeInt(1);
        outputStream.writeInt(10); //client height set to 10
    }

    public void down() throws IOException {
        outputStream.writeInt(2);
        outputStream.writeInt(1);
    }

    public void up() throws IOException {
        outputStream.writeInt(2);
        outputStream.writeInt(-1);
    }
}
