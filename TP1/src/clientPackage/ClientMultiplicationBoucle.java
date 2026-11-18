package clientPackage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * Activité 1-2 (question 5) : le client saisit plusieurs entiers successivement.
 * Après chaque saisie, il envoie x, reçoit x * 5 et l'affiche.
 * La saisie de 0 termine proprement la communication.
 */
public class ClientMultiplicationBoucle {

    public static void main(String[] args) {
        System.out.println("Je suis un client pas encore connecte...");
        try (Scanner clavier = new Scanner(System.in)) {
            Socket socket = new Socket("localhost", 1234);
            System.out.println("je suis un client connecte");

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            while (true) {
                System.out.print("Donnez un entier x (0 pour quitter) : ");
                int x = clavier.nextInt();

                dos.writeInt(x);
                dos.flush();

                if (x == 0) {
                    System.out.println("Fin de la communication demandee.");
                    break;
                }

                int resultat = dis.readInt();
                System.out.println("Resultat recu du serveur : " + x + " * 5 = " + resultat);
            }

            socket.close();
            System.out.println("Socket du client fermee.");
        } catch (IOException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}
