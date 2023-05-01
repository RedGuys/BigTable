package ru.redguy.server;


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(3562);
        Table table = new Table();
        ServerSocketThread serverSocketThread = new ServerSocketThread(table, serverSocket);
        new Thread(serverSocketThread).start();

        //Every second make any action with table (add, remove, change) in random way in separate thread
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
        executor.scheduleAtFixedRate(() -> {
            int action = (int) (Math.random() * 3);
            switch (action) {
                case 0:
                    table.add(new Person((int) (Math.random() * 100000), generateString(), generateString(), (int) (Math.random() * 100)));
                    break;
                case 1:
                    Person person1 = table.getRandomPerson();
                    table.remove(person1);
                    break;
                case 2:
                    //TODO: change person
                    break;
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    static Random random = new Random();

    @Contract(" -> new")
    public static @NotNull String generateString()
    {
        int length = random.nextInt(10) + 5;
        String characters = "abcdefghijklmnopqrstuvwxyz";
        char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {
            text[i] = characters.charAt(random.nextInt(characters.length()));
        }
        return new String(text);
    }
}
