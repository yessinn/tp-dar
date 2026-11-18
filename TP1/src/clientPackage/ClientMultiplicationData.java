package clientPackage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * Activité 1-2 (question 4) : le client demande au serveur de calculer x * 5.
 * Version 2 : DataInputStream / DataOutputStream (entier complet sur 4 octets).
 */
public class ClientMultiplicationData {

    public static void main(String[] args) {
        System.out.println("Je suis un client pas encore connecte...");
        try (Scanner clavier = new Scanner(System.in)) {
            Socket socket = new Socket("localhost", 1234);
            System.out.println("je suis un client connecte");

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            System.out.print("Donnez un entier x : ");
            int x = clavier.nextInt();

            dos.writeInt(x);
            dos.flush();

            int resultat = dis.readInt();
            System.out.println("Resultat recu du serveur : " + x + " * 5 = " + resultat);

            socket.close();
        } catch (IOException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}
