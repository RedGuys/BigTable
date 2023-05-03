package ru.redguy.server;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerSocketThread extends Thread {
    private Table table;
    private final ServerSocket socket;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final CopyOnWriteArrayList<ClientSocketThread> clientSocketThreads = new CopyOnWriteArrayList<>();
    public boolean closing = false;

    public ServerSocketThread(@NotNull Table table, ServerSocket socket) {
        this.table = table;
        this.socket = socket;
        table.setServerSocketThread(this);
    }

    @Override
    public void run() {
        while (!closing) {
            try {
                ClientSocketThread cst = new ClientSocketThread(table,socket.accept());
                clientSocketThreads.add(cst);
                executorService.submit(cst);
            } catch (SocketException e) {
                break;
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public void close() throws IOException {
        socket.close();
        executorService.shutdown();
    }

    public void distributePersonAdd(Person person) {
        clientSocketThreads.removeIf((cst) -> cst.socket.isClosed());
        for (ClientSocketThread cst : clientSocketThreads) {
            cst.addPerson(person);
        }
    }

    public void distributePersonDelete(Person person) {
        clientSocketThreads.removeIf((cst) -> cst.socket.isClosed());
        for (ClientSocketThread cst : clientSocketThreads) {
            cst.deletePerson(person);
        }
    }

    public void distributePersonUpdate(Person person) {
        clientSocketThreads.removeIf((cst) -> cst.socket.isClosed());
        for (ClientSocketThread cst : clientSocketThreads) {
            cst.updatePerson(person);
        }
    }
}
