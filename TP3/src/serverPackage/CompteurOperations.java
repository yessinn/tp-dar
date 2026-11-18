package serverPackage;

/**
 * Activité 3-2 (étape 3) : compteur global du nombre d'opérations traitées.
 *
 * Une seule instance est partagée par tous les threads ClientProcess : c'est
 * une ressource critique. L'opération ++total n'est pas atomique (lecture,
 * addition, écriture) : sans synchronisation, deux threads peuvent lire la même
 * valeur et l'une des deux incrémentations est perdue.
 *
 * Le mot-clé synchronized garantit qu'un seul thread à la fois exécute
 * incrementer() sur cet objet (verrou de l'objet).
 */
public class CompteurOperations {

    private int total = 0;

    /** Incrémente le compteur et renvoie la nouvelle valeur, de façon atomique. */
    public synchronized int incrementer() {
        total++;
        return total;
    }

    public synchronized int getTotal() {
        return total;
    }
}
