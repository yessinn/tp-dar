package clientPackage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import commonPackage.Operation;
import commonPackage.Resultat;
import commonPackage.SyntaxeOperation;

/**
 * Activité 2-2 : échange d'objets.
 *
 * Le client lit une opération au clavier, la valide, l'encapsule dans un objet
 * Operation (Serializable) qu'il envoie au serveur, puis lit l'objet Resultat
 * renvoyé. Taper "fin" pour quitter.
 *
 * Utilisation : java -cp out clientPackage.ClientOperationObjet [adresseServeur]
 */
public class ClientOperationObjet {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";

        try (Scanner clavier = new Scanner(System.in)) {
            System.out.println("Je suis un client pas encore connecte...");
            Socket socket = new Socket(host, PORT);
            System.out.println("je suis un client connecte au serveur " + host + ":" + PORT);

            // Même ordre que le serveur : flux de sortie d'abord, pour éviter l'interblocage
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

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
                    System.out.println("Syntaxe invalide : \"" + saisie
                            + "\" (format attendu : nombre operateur nombre, operateurs + - * /)");
                    continue;
                }

                // b) création et envoi de l'objet contenant les arguments de l'opération
                Operation operation = new Operation(Double.parseDouble(elements[0]),
                        elements[1].charAt(0), Double.parseDouble(elements[2]));
                oos.writeObject(operation);
                oos.flush();

                // g) lecture et affichage du résultat (objet Resultat)
                Resultat resultat = (Resultat) ois.readObject();
                if (resultat.isSucces()) {
                    System.out.println(">>> Resultat recu du serveur : " + operation + " = "
                            + SyntaxeOperation.formater(resultat.getValeur()));
                } else {
                    System.out.println(">>> Erreur recue du serveur : " + resultat.getMessage());
                }
            }

            socket.close();
            System.out.println("Fin de la communication. Socket du client fermee.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur cote client : " + e.getMessage());
        }
    }
}
