package serverPackage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Activité 1-2 (question 4) : service de multiplication x * 5.
 * Version 2 : DataInputStream / DataOutputStream.
 * readInt() / writeInt() transmettent l'entier complet sur 4 octets
 * (complément à deux), donc les négatifs et les grandes valeurs passent correctement.
 */
public class ServerMultiplicationData {

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println("Je suis un serveur en attente de la connexion d'un client ");

            Socket socket = serverSocket.accept();
            System.out.println("un client est connecte");

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            int x = dis.readInt();
            System.out.println("Le serveur a recu x = " + x);

            int resultat = x * 5;
            dos.writeInt(resultat);
            dos.flush();
            System.out.println("Le serveur a envoye le resultat x * 5 = " + resultat);

            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }
}
