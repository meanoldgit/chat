package com.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Peer extends TerminalHandler {
    private final int PORT = 3000;
    private String ipAddress = "";
    private boolean loop = true;
    private boolean connected = false;
    private boolean msgMode = false;
    private boolean searchMode = false;
    private boolean loadingAnimationStarted = false;
    private ArrayList<String> msgList = new ArrayList<>();


    public void connect() {
        System.out.println(Cursor.SHOW_CURSOR);
        ipAssign();
        startClient();
        startServer();
    }


    // IP ADDRESS

    private void ipAssign() {
        System.out.print("Enter the IP: ");
        boolean valid = false;
        while (!valid) {
            ipAddress = readKeys();
            valid = ipValidation();
            if (valid) return;
            System.out.print("Invalid IP, try again: ");
        }
    }

    private boolean ipValidation() {
        String regex = "^((25[0-5]|(2[0-4]|1[0-9]|[1-9]|)[0-9])(\\.(?!$)|$)){4}$";
        if (ipAddress.matches(regex) || ipAddress.isEmpty()) {
            return true;
        }
        return false;
    }


    // SERVER

    private void startServer() {
        Thread thread = new Thread(() -> {
            try {
                ServerSocket server = new ServerSocket(PORT);
                while (loop) {
                    try {
                        Socket client = server.accept();
                        PrintWriter output = new PrintWriter(client.getOutputStream(), true);
                        displayArrows();
                        String msg = readKeys();

                        if (!keywordDetected(msg)) {
                            if (!(msgMode || searchMode)) {
                                msgList.add(msg);
                                output.println(msg);
                            }
                        }
                        
                        client.close();
                        threadSleep(100);
                    }
                    catch (Exception e) {
                        System.err.println("Error. " + e.getMessage() + ".");
                    }
                }
                close(server);
            }
            catch (IOException e) {
                System.err.println("Couldn't connect to client. " + e.getMessage() + ".");
            }
        });
        thread.start();
    }

    private void displayArrows() {
        String arrows = ">> ";
        if (msgMode) {
            arrows = "...";
        } else if (searchMode) {
            arrows= "?> ";
        }
        System.out.print(arrows);
    }

    private void close(ServerSocket server) {
        try {
            server.close();
        } catch (IOException e) {
            System.err.println(e.getMessage() + ".");
        }
        System.out.print(Cursor.SHOW_CURSOR);
    }


    // CLIENT

    private void startClient() {
        Thread thread = new Thread(() -> {
            while (loop) {
                try {
                    // TODO: upon reconnecting after someone exits, the 1st message is lost
                    Socket socket = new Socket(ipAddress, PORT);
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msg = input.readLine();
                    if (msg != null) {
                        msgList.add(msg);
                        System.out.println(msg);
                    }
                    socket.close();
                    connected = true;
                    threadSleep(100);
                }
                catch (Exception e) {
                    if (connected) System.err.println("Couldn't connect to server. " + e.getMessage() + ".");
                    else if (!loadingAnimationStarted) startLoadingAnimation();
                }
            }
        });
        thread.start();
    }


    // KEYWORDS

    private boolean keywordDetected(String str) {
        String line = str.toLowerCase().replaceAll("\\s", "");
        switch (line) {
            case "quit"  : loop = false;        break;
            case "msg"   : msgMode = true;      break;
            case "send"  : msgMode = false;     break;
            case "del"   : msgMode = false;     break;
            case "find"  : searchMode = true;   break;
            case "end"   : searchMode = false;  break;
            case "cancel": searchMode = false;  break;
            case "print" : print();             break;
            case "count" : displayCount();      break;
            default:
                if (line.matches("^(goto|up|down|dwn|rm)\\d*$")) {
                    splitAndExecuteCommand(line);
                    return true;
                }
                return false;
        }
        return true;
    }

    private void splitAndExecuteCommand(String line) {
        String[] splits = line.split("(?<=\\D)(?=\\d)");
        int num = 1;
        int id = count(); // defaults to last message

        if (splits.length > 1) {
            num = Integer.parseInt(splits[1]);
            id = num;
        }

        if (line.contains("goto")) {
            goTo(id);
        } else if (line.contains("rm")) {
            remove(id);
        } else {
            String keyword = splits[0];
            if (keyword.equals("up")) {
                scrollUpBy(num);
            } else {
                scrollDownBy(num);
            }
        }
    }


    // COMMANDS

    private void displayCount() {
        System.out.println(count());
    }

    private int count() {
        return msgList.size();
    }

    private void print() {
    }

    

    private void goTo(int id) {
    }

    private void remove(int id) {
    }


    // ANIMATION

    private void startLoadingAnimation() {
        loadingAnimationStarted = true;
        Thread loadingAnimation = new Thread(() -> {
            int index = 0;
            System.out.print(Cursor.HIDE_CURSOR);
            while (!connected) {
                System.out.print(Cursor.CHANGE_COLUMN);
                if (index > 3) index = 0;
                System.out.print("Couldn't connect to server. Retrying" + ".".repeat(index));
                System.out.print(Cursor.CLEAR_LINE_AFTER_CURSOR);
                index++;
                threadSleep(300);
            }
            System.out.print(Cursor.SHOW_CURSOR);
        });
        loadingAnimation.start();
    }

    private void threadSleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            System.err.println(e.getMessage() + ".");
        }
    }
}
