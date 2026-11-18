package serverPackage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import commonPackage.Calculateur;
import commonPackage.Operation;
import commonPackage.Resultat;

/**
 * Activité 3-2 : thread dédié à un client de la calculatrice.
 *
 * Reçoit des objets Operation, renvoie des objets Resultat, et incrémente le
 * compteur global partagé à chaque opération calculée.
 */
public class ClientProcess extends Thread {

    private final Socket socket;
    private final int numero;
    private final CompteurOperations compteur;

    public ClientProcess(Socket socket, int numero, CompteurOperations compteur) {
        super("client-" + numero);
        this.socket = socket;
        this.numero = numero;
        this.compteur = compteur;
    }

    @Override
    public void run() {
        String prefixe = "[client " + numero + " " + socket.getRemoteSocketAddress() + "] ";
        try (Socket s = socket) {
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());

            // Envoi au client de son numéro d'ordre
            oos.writeInt(numero);
            oos.flush();

            while (true) {
                Object recu;
                try {
                    recu = ois.readObject();
                } catch (EOFException e) {
                    break; // le client a fermé la connexion
                }
                if (!(recu instanceof Operation)) {
                    oos.writeObject(Resultat.erreur("objet recu inattendu"));
                    oos.flush();
                    continue;
                }
                Operation op = (Operation) recu;
                Resultat resultat = Calculateur.calculer(op.getOperande1(), op.getOperateur(), op.getOperande2());

                oos.writeObject(resultat);
                oos.flush();

                // Section critique : incrémentation synchronisée du compteur partagé
                int total = compteur.incrementer();
                System.out.println(prefixe + op + " = " + resultat
                        + "   | total des operations traitees : " + total);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(prefixe + "erreur : " + e.getMessage());
        }
        System.out.println(prefixe + "deconnecte.");
    }
}
