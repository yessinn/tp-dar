package serverPackage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import commonPackage.Operation;
import commonPackage.Resultat;

/**
 * Activité 2-2 : échange d'objets.
 *
 * Le serveur reçoit un objet Operation, récupère ses données, effectue le
 * calcul et renvoie un objet Resultat. La communication s'arrête quand le
 * client ferme la connexion.
 */
public class ServerOperationObjet {

    private static final int PORT = 1234;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Je suis un serveur en attente de la connexion d'un client ");

            Socket socket = serverSocket.accept();
            System.out.println("un client est connecte");

            // L'ObjectOutputStream est créé (et vidé) AVANT l'ObjectInputStream :
            // le constructeur d'ObjectInputStream attend l'en-tête envoyé par
            // l'ObjectOutputStream de l'autre côté. Dans l'ordre inverse, client
            // et serveur s'attendraient mutuellement (interblocage).
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            while (true) {
                // c) réception de l'objet
                Object recu;
                try {
                    recu = ois.readObject();
                } catch (EOFException e) { // le client a fermé la connexion
                    System.out.println("Le client s'est deconnecte : fin de la communication.");
                    break;
                }

                if (!(recu instanceof Operation)) {
                    oos.writeObject(Resultat.erreur("objet recu inattendu"));
                    oos.flush();
                    continue;
                }
                Operation operation = (Operation) recu;
                System.out.println("Objet recu : " + recu.getClass().getName() + " [" + operation + "]");

                // d) récupération des données  -  e) calcul
                Resultat resultat = calculer(operation.getOperande1(), operation.getOperateur(),
                        operation.getOperande2());

                // f) envoi du résultat sous forme d'objet
                oos.writeObject(resultat);
                oos.flush();
                System.out.println("Objet Resultat envoye : " + resultat);
            }

            socket.close();
            System.out.println("Sockets du serveur fermees.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur cote serveur : " + e.getMessage());
        }
    }

    static Resultat calculer(double x, char operateur, double y) {
        switch (operateur) {
            case '+': return Resultat.ok(x + y);
            case '-': return Resultat.ok(x - y);
            case '*': return Resultat.ok(x * y);
            case '/':
                if (y == 0) {
                    return Resultat.erreur("division par zero impossible");
                }
                return Resultat.ok(x / y);
            default:
                return Resultat.erreur("operateur inconnu '" + operateur + "'");
        }
    }
}
