package ru.redguy.server;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Person {
    int id;
    String firstName;
    String lastName;
    int age;

    public Person(int id, String firstName, String lastName, int age) {
        this.id = id;
        this.firstName = firstName;
        this.lastName= lastName;
        this.age = age;
    }

    public static @NotNull Person read(@NotNull DataInputStream inputStream) throws IOException {
        int id = inputStream.readInt();
        String firstName = inputStream.readUTF();
        String lastName = inputStream.readUTF();
        int age = inputStream.readInt();
        return new Person(id, firstName, lastName, age);
    }

    public void write(@NotNull DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(id);
        outputStream.writeUTF(firstName);
        outputStream.writeUTF(lastName);
        outputStream.writeInt(age);
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getAge() {
        return age;
    }

    protected void setId(int id) {
        this.id = id;
    }
}
