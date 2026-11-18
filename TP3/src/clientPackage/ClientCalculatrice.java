package clientPackage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

import commonPackage.Operation;
import commonPackage.Resultat;
import commonPackage.SyntaxeOperation;

/**
 * Activité 3-2 : client de la calculatrice multi-thread (communication par objets).
 *
 * Utilisation : java -cp out clientPackage.ClientCalculatrice [adresseIPServeur]
 */
public class ClientCalculatrice {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        String ip = (args.length > 0) ? args[0] : "localhost";

        try (Scanner clavier = new Scanner(System.in)) {
            InetSocketAddress adresseServeur = new InetSocketAddress(InetAddress.getByName(ip), PORT);
            Socket socket = new Socket();
            socket.connect(adresseServeur, 5000);

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            int numero = ois.readInt();
            System.out.println("Connecte au serveur " + adresseServeur.getAddress().getHostAddress() + ":" + PORT
                    + " : je suis le client numero " + numero);

            while (true) {
                System.out.print("Operation (ex. 55 * 25, 'fin' pour quitter) : ");
                if (!clavier.hasNextLine()) {
                    break;
                }
                String saisie = clavier.nextLine().trim();
                if (saisie.equalsIgnoreCase("fin")) {
                    break;
                }
                String[] elements = SyntaxeOperation.analyser(saisie);
                if (elements == null) {
                    System.out.println("Syntaxe invalide : \"" + saisie + "\"");
                    continue;
                }

                Operation operation = new Operation(Double.parseDouble(elements[0]),
                        elements[1].charAt(0), Double.parseDouble(elements[2]));
                oos.writeObject(operation);
                oos.flush();

                Resultat resultat = (Resultat) ois.readObject();
                System.out.println(">>> " + operation + " = " + resultat);
            }

            socket.close();
            System.out.println("Client numero " + numero + " : fin de la communication.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}
