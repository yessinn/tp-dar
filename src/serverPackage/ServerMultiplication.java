package serverPackage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Activité 1-2 (questions 1 à 3) : service de multiplication x * 5.
 * Version 1 : utilisation des flux bruts InputStream / OutputStream.
 * read() et write(int) ne transportent qu'UN SEUL OCTET (0..255) :
 * les entiers négatifs ou supérieurs à 255 sont donc mal transmis.
 */
public class ServerMultiplication {

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println("Je suis un serveur en attente de la connexion d'un client ");

            Socket socket = serverSocket.accept();
            System.out.println("un client est connecte");

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            // c) Le serveur attend puis reçoit le nombre envoyé par le client
            int x = is.read();
            System.out.println("Le serveur a recu x = " + x);

            // d) Le serveur calcule x * 5
            int resultat = x * 5;

            // e) Le serveur envoie le résultat au client
            os.write(resultat);
            System.out.println("Le serveur a envoye le resultat x * 5 = " + resultat);

            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }
}
