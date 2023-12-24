package ru.gb.lesson5.hw;

import lombok.Getter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class  Server {

  // message broker (kafka, redis, rabbitmq, ...)
  // client sent letter to broker

  // server sent to SMTP-server

  public static final int PORT = 8181;

  private static long clientIdCounter = 1L;
  private static Map<Long, SocketWrapper> clients = new HashMap<>();

  public static void main(String[] args) throws IOException {
    try (ServerSocket server = new ServerSocket(PORT)) {
      System.out.println("Сервер запущен на порту " + PORT);
      while (true) {
        final Socket client = server.accept();
        final long clientId = clientIdCounter++;

        SocketWrapper wrapper = new SocketWrapper(clientId, client);
        System.out.println("Подключился новый клиент[" + wrapper + "]");
        clients.put(clientId, wrapper);

        new Thread(() -> {
          try (Scanner input = wrapper.getInput(); PrintWriter output = wrapper.getOutput()) {
            output.println("Подключение успешно. Список всех клиентов: " + clients);

            while (true) {
              String clientInput = input.nextLine();
              if (Objects.equals("q", clientInput)) {
                // todo разослать это сообщение всем остальным клиентам
                clients.remove(clientId);
                clients.values().forEach(it -> it.getOutput().println("Клиент[" + clientId + "] отключился"));
                break;
              }
              if (clientInput.length() > 5 && Objects.equals("kick", clientInput.substring(0, 4)) && clients.get(clientId).isAdmin()) {
                // формат сообщения: "kick 4"
                long destinationId = Long.parseLong(clientInput.substring(5));
                if(clients.containsKey(destinationId)){
                  clients.get(destinationId).getSocket().close();
                  clients.remove(destinationId);
                  clients.values().forEach(it -> it.getOutput().println("Клиент[" + destinationId + "] отключен"));
                } else {
                  clients.get(clientId).getOutput().println("Такого клиента нет");
                }
              } else if (Objects.equals("@", clientInput.substring(0,1))){
                // формат сообщения: "@цифра сообщение"
                long destinationId = Long.parseLong(clientInput.substring(1, clientInput.indexOf(" ")));
                if(clients.containsKey(destinationId)){
                  SocketWrapper destination = clients.get(destinationId);
                  destination.getOutput().println(clientInput);
                } else {
                  clients.get(clientId).getOutput().println("Такого клиента нет");
                };
              } else if (clientInput.equals("admin")) {
                clients.get(clientId).setAdmin();
              } else {
                clients.values().forEach(it -> {
                  if (it.getId() != clientId) it.getOutput().println(clientInput);
                });
              };
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).start();
      }
    }
  }

}

@Getter
class SocketWrapper implements AutoCloseable {

  private final long id;
  private final Socket socket;
  private final Scanner input;
  private final PrintWriter output;
  private boolean admin;

  SocketWrapper(long id, Socket socket) throws IOException {
    this.id = id;
    this.socket = socket;
    this.input = new Scanner(socket.getInputStream());
    this.output = new PrintWriter(socket.getOutputStream(), true);
    this.admin = false;
  }

  public void setAdmin() {
    this.admin = true;
  }

  @Override
  public void close() throws Exception {
    socket.close();
  }

  @Override
  public String toString() {
    return String.format("%s", socket.getInetAddress().toString());
  }
}
