package serverPackage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Activité 1-2 (question 5) : service de multiplication x * 5 en boucle.
 * Le serveur traite plusieurs entiers envoyés par le même client
 * et s'arrête proprement lorsqu'il reçoit 0.
 */
public class ServerMultiplicationBoucle {

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println("Je suis un serveur en attente de la connexion d'un client ");

            Socket socket = serverSocket.accept();
            System.out.println("un client est connecte");

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            while (true) {
                int x = dis.readInt();
                if (x == 0) {
                    System.out.println("Le client a envoye 0 : fin de la communication.");
                    break;
                }
                int resultat = x * 5;
                dos.writeInt(resultat);
                dos.flush();
                System.out.println("Recu x = " + x + " -> resultat envoye : " + resultat);
            }

            socket.close();
            serverSocket.close();
            System.out.println("Sockets du serveur fermees.");
        } catch (IOException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }
}
