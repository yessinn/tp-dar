package clientPackage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * Activité 1-2 (questions 1 à 3) : le client demande au serveur de calculer x * 5.
 * Version 1 : flux bruts InputStream / OutputStream (un seul octet transmis).
 */
public class ClientMultiplication {

    public static void main(String[] args) {
        System.out.println("Je suis un client pas encore connecte...");
        try (Scanner clavier = new Scanner(System.in)) {
            Socket socket = new Socket("localhost", 1234);
            System.out.println("je suis un client connecte");

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            // a) Le client tape un entier x au clavier
            System.out.print("Donnez un entier x : ");
            int x = clavier.nextInt();

            // b) Le client envoie x au serveur (seul l'octet de poids faible part)
            os.write(x);

            // f) Le client reçoit le résultat
            int resultat = is.read();
            System.out.println("Resultat recu du serveur : " + x + " * 5 = " + resultat);

            socket.close();
        } catch (IOException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}
