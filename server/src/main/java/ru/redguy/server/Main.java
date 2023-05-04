package ru.redguy.server;


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    public static Random random = new Random();

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(3562);
        Table table = new Table();

        ThreadPoolExecutor addPool = new ThreadPoolExecutor(2, 4, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        for (int i = 0; i < 25000; i++) { // add 2.5M records to table
            addPool.execute(() -> {
                for (int j = 0; j < 100; j++) {
                    table.add(new Person(random.nextInt(Integer.MAX_VALUE), generateString(), generateString(), (int) (Math.random() * 100)));
                }
            });
        }
        while (!addPool.getQueue().isEmpty()) {
            System.out.println(Instant.now().toString() + " " + addPool.getQueue().size());
            Thread.sleep(1000);
        }

        System.out.println("Data generated");

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
                case 2: {
                    int subAction = (int) (Math.random() * 3);
                    switch (subAction) {
                        case 0:
                            Person person2 = table.getRandomPerson();
                            if(person2 == null) break;
                            person2.setFirstName(generateString());
                            table.update(person2);
                            break;
                        case 1:
                            Person person3 = table.getRandomPerson();
                            if(person3 == null) break;
                            person3.setLastName(generateString());
                            table.update(person3);
                            break;
                        case 2:
                            Person person4 = table.getRandomPerson();
                            if(person4 == null) break;
                            person4.setAge((int) (Math.random() * 100));
                            table.update(person4);
                            break;
                    }
                    break;
                }
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    @Contract(" -> new")
    public static @NotNull String generateString() {
        int length = random.nextInt(10) + 5;
        String characters = "abcdefghijklmnopqrstuvwxyz";
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(random.nextInt(characters.length()));
        }
        return new String(text);
    }
}
