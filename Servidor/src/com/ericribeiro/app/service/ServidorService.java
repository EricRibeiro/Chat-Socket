/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ericribeiro.app.service;

import com.ericribeiro.bean.ChatMessage;
import com.ericribeiro.bean.ChatMessage.Action;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ericribeiro
 */
public class ServidorService {

    private ServerSocket serverSocket;
    private Socket socket;
    private Map<String, ObjectOutputStream> mapOnline = new HashMap();

    public ServidorService() {
        try {
            int porta = 5555;

            serverSocket = new ServerSocket(porta);

            System.out.println("Servidor iniciado na porta " + porta + ".");

            while (true) {
                socket = serverSocket.accept();

                new Thread(new ListenerSocket(socket)).start();
            }

        } catch (IOException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class ListenerSocket implements Runnable {

        private ObjectOutputStream output;
        private ObjectInputStream input;

        public ListenerSocket(Socket socket) {
            try {
                this.output = new ObjectOutputStream(socket.getOutputStream());
                this.input = new ObjectInputStream(socket.getInputStream());
            } catch (IOException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void run() {
            ChatMessage message = null;

            try {
                while ((message = (ChatMessage) input.readObject()) != null) {
                    Action action = message.getAction();

                    if (action.equals(Action.CONNECT)) {
                        boolean isConnected = connect(message, output);

                        if (isConnected) {
                            mapOnline.put(message.getName(), output);
                        }

                        sendOnline();

                    } else if (action.equals(Action.DISCONNECT)) {
                        disconnect(message, output);
                        sendOnline();
                        return;

                    } else if (action.equals(Action.SEND_ALL)) {
                        sendAll(message);

                    } else if (action.equals(Action.SEND_ONE)) {
                        sendOne(message);

                    } 
                }

            } catch (IOException ex) {
                ChatMessage cm = new ChatMessage();
		cm.setName(message.getName());
		disconnect(cm, output);
		sendOnline();
		System.out.println(message.getName() + " foi desconectado.");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private boolean connect(ChatMessage message, ObjectOutputStream output) {
        if (mapOnline.size() == 0) {
            message.setText("YEP");
            send(message, output);
            return true;
        }

        if (mapOnline.containsKey(message.getName())) {
            message.setText("NOPE");
            send(message, output);
            return false;
        } else {
            message.setText("YEP");
            send(message, output);
            return true;
        }
    }

    private void disconnect(ChatMessage message, ObjectOutputStream output) {
        mapOnline.remove(message.getName());

        message.setText(message.getName() + " foi desconectado.");
        message.setAction(Action.SEND_ONE);

        sendAll(message);
    }

    private void send(ChatMessage message, ObjectOutputStream output) {
        try {
            output.writeObject(message);
        } catch (IOException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendOne(ChatMessage message) {
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnline.entrySet()) {
            if (kv.getKey().equals(message.getNameReserved())) {
                try {
                    kv.getValue().writeObject(message);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void sendAll(ChatMessage message) {
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnline.entrySet()) {
            if (!kv.getKey().equals(message.getName())) {
                message.setAction(Action.SEND_ONE);

                try {
                    kv.getValue().writeObject(message);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void sendOnline() {
        Set<String> setNames = new HashSet<>();

        for (Map.Entry<String, ObjectOutputStream> kv : mapOnline.entrySet()) {
            setNames.add(kv.getKey());
        }

        ChatMessage message = new ChatMessage();
        message.setAction(Action.USERS_ONLINE);
        message.setSetOnlines(setNames);

        for (Map.Entry<String, ObjectOutputStream> kv : mapOnline.entrySet()) {
            message.setName(kv.getKey());

            try {
                kv.getValue().writeObject(message);
            } catch (IOException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }
}
