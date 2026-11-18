package serverPackage;

/**
 * Activité 3-2 (étape 3) : démonstration de l'utilité de synchronized.
 *
 * 4 threads incrémentent chacun 100 000 fois un compteur partagé :
 *   - une fois avec un compteur NON synchronisé,
 *   - une fois avec CompteurOperations (synchronisé).
 * Le total attendu est 400 000.
 *
 * Utilisation : java -cp out serverPackage.DemoConcurrence
 */
public class DemoConcurrence {

    private static final int NB_THREADS = 4;
    private static final int NB_INCREMENTS = 100_000;

    /** Compteur volontairement non protégé. */
    static class CompteurNonSynchronise {
        private int total = 0;

        int incrementer() {
            total++;
            return total;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CompteurNonSynchronise nonSync = new CompteurNonSynchronise();
        lancer(() -> nonSync.incrementer());
        System.out.println("Sans synchronized : total = " + nonSync.total
                + " (attendu " + NB_THREADS * NB_INCREMENTS + ")");

        CompteurOperations sync = new CompteurOperations();
        lancer(() -> sync.incrementer());
        System.out.println("Avec synchronized : total = " + sync.getTotal()
                + " (attendu " + NB_THREADS * NB_INCREMENTS + ")");
    }

    private static void lancer(Runnable increment) throws InterruptedException {
        Thread[] threads = new Thread[NB_THREADS];
        for (int i = 0; i < NB_THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < NB_INCREMENTS; j++) {
                    increment.run();
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }
}
