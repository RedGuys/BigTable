package ru.redguy.gui.gui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Person {
    private SimpleIntegerProperty id;
    private SimpleStringProperty firstName;
    private SimpleStringProperty lastName;
    private SimpleIntegerProperty age;

    public Person(int id, String firstName, String lastName, int age) {
        this.id = new SimpleIntegerProperty(null, "id", id);
        this.firstName = new SimpleStringProperty(null, "firstName", firstName);
        this.lastName = new SimpleStringProperty(null, "lastName", lastName);
        this.age = new SimpleIntegerProperty(null, "age", age);
    }

    public static @NotNull Person read(@NotNull DataInputStream inputStream) throws IOException {
        int id = inputStream.readInt();
        String firstName = inputStream.readUTF();
        String lastName = inputStream.readUTF();
        int age = inputStream.readInt();
        return new Person(id, firstName, lastName, age);
    }

    public void write(@NotNull DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(id.get());
        outputStream.writeUTF(firstName.get());
        outputStream.writeUTF(lastName.get());
        outputStream.writeInt(age.get());
    }

    public int getId() {
        return id.get();
    }

    public SimpleIntegerProperty idProperty() {
        return id;
    }

    public String getFirstName() {
        return firstName.get();
    }

    public SimpleStringProperty firstNameProperty() {
        return firstName;
    }

    public String getLastName() {
        return lastName.get();
    }

    public SimpleStringProperty lastNameProperty() {
        return lastName;
    }

    public int getAge() {
        return age.get();
    }

    public SimpleIntegerProperty ageProperty() {
        return age;
    }
}
