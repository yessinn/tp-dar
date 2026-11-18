# TP3 — Serveur multi-threads : sockets en mode connecté TCP

| | |
|---|---|
| **Matière** | Développement d'applications réparties |
| **Auditoire** | LSI 3 — Faculté des Sciences de Sfax |
| **Année universitaire** | 2026 – 2027 |
| **Étudiant(e)** | _Nom Prénom à compléter_ |

Ce dossier correspond au projet **TP3-DAR2026**. Il contient les deux activités du TP 3 :

- **Activité 3-1** : acceptation de plusieurs clients (serveur multi-thread, échange de chaînes de caractères) ;
- **Activité 3-2** : calculatrice multi-thread avec communication par objets et compteur global partagé.

Dans les TP1 et TP2, le serveur ne servait qu'**un seul client** par exécution : après `accept()`, il dialoguait avec ce client puis s'arrêtait. Ici, le serveur accepte les clients en boucle et confie chacun à un **thread dédié**.

---

## Structure du projet

```
TP3/
├── README.md
└── src/
    ├── commonPackage/
    │   ├── SyntaxeOperation.java       # validation "nombre operateur nombre" (reprise du TP2)
    │   ├── Calculateur.java            # calcul + - * / (partage par 3-1 et 3-2)
    │   ├── Operation.java              # objet envoye par le client (Serializable)
    │   └── Resultat.java               # objet renvoye par le serveur (Serializable)
    ├── serverPackage/
    │   ├── ServeurMultiClients.java    # Activite 3-1 : boucle d'acceptation
    │   ├── ClientProcessCC.java        # Activite 3-1 : thread par client (texte)
    │   ├── ServeurCalculatrice.java    # Activite 3-2 : boucle d'acceptation
    │   ├── ClientProcess.java          # Activite 3-2 : thread par client (objets)
    │   ├── CompteurOperations.java     # Activite 3-2 : compteur global synchronise
    │   └── DemoConcurrence.java        # Activite 3-2 : demonstration avec / sans synchronized
    └── clientPackage/
        ├── ClientCC.java               # Activite 3-1
        └── ClientCalculatrice.java     # Activite 3-2
```

## Compilation et exécution

Depuis le dossier `TP3/` :

```powershell
javac -encoding UTF-8 -d out src/commonPackage/*.java src/serverPackage/*.java src/clientPackage/*.java
```

```powershell
# Activité 3-1 : un serveur, puis autant de clients que souhaité (un terminal chacun)
java -cp out serverPackage.ServeurMultiClients
java -cp out clientPackage.ClientCC                  # client local
java -cp out clientPackage.ClientCC 192.168.1.14     # client vers l'IP du serveur
telnet localhost 1234                                # client telnet

# Activité 3-2
java -cp out serverPackage.ServeurCalculatrice
java -cp out clientPackage.ClientCalculatrice [adresseIPServeur]

# Démonstration de l'accès concurrent
java -cp out serverPackage.DemoConcurrence
```

---

# Activité 3-1 : Acceptation de plusieurs clients

## Principe du serveur multi-thread

```
                    ┌──────────────── thread principal ────────────────┐
                    │  while (true) {                                  │
 client 1 ──────────┼──►  socket = serverSocket.accept();              │
 client 2 ──────────┼──►  numero++;                                    │
 client 3 ──────────┼──►  new ClientProcessCC(socket, numero).start(); │
                    │  }                                               │
                    └───────┬───────────────┬───────────────┬──────────┘
                            ▼               ▼               ▼
                     thread client-1  thread client-2  thread client-3
                     (dialogue avec   (dialogue avec   (dialogue avec
                      le client 1)     le client 2)     le client 3)
```

- Le **thread principal** ne fait qu'une chose : attendre une connexion (`accept()`), la numéroter et lancer un thread pour elle. Il retourne aussitôt à `accept()`.
- Chaque **thread `ClientProcessCC`** gère tout le dialogue avec **son** client, avec sa propre `Socket` et ses propres flux. Un client lent ou inactif ne bloque donc plus les autres.
- Le numéro d'ordre est incrémenté uniquement par le thread principal, donc sans risque d'accès concurrent.

## Code

### `ServeurMultiClients` — boucle d'acceptation

```java
int numeroClient = 0;
try (ServerSocket serverSocket = new ServerSocket(PORT)) {
    System.out.println("Serveur multi-thread demarre sur le port " + PORT + ", en attente de clients...");
    while (true) {
        Socket socket = serverSocket.accept();
        numeroClient++;
        // b) adresse IP (et port) du client + numéro d'ordre
        System.out.println("Connexion du client numero " + numeroClient
                + " depuis " + socket.getRemoteSocketAddress());
        new ClientProcessCC(socket, numeroClient).start();
    }
}
```

### `ClientProcessCC` — thread dédié à un client

```java
public class ClientProcessCC extends Thread {
    private final Socket socket;
    private final int numero;

    @Override
    public void run() {
        String prefixe = "[client " + numero + " " + socket.getRemoteSocketAddress() + "] ";
        try (Socket s = socket) {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), UTF_8), true);

            // c) envoi au client de son numéro d'ordre
            out.println("Bienvenue ! Vous etes le client numero " + numero);
            out.println("Tapez une operation (ex. 55 * 25) ou 'fin' pour quitter.");

            String ligne;
            while ((ligne = in.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.equalsIgnoreCase("fin")) { out.println("Au revoir client numero " + numero); break; }
                String[] elements = SyntaxeOperation.analyser(ligne);   // validation côté serveur
                String reponse = (elements == null)
                        ? "Erreur : syntaxe invalide \"" + ligne + "\""
                        : /* calcul avec Calculateur.calculer(...) */;
                out.println(reponse);
                System.out.println(prefixe + ligne + "  ->  " + reponse);
            }
        } catch (IOException e) { ... }
        System.out.println(prefixe + "deconnecte.");
    }
}
```

Par rapport à l'Activité 2-1, l'opération est envoyée sur **une seule ligne** (`55 * 25`) au lieu de trois. Le service devient ainsi utilisable directement depuis **telnet**. Comme un client telnet n'effectue aucune validation, c'est le **serveur** qui valide chaque ligne reçue.

### `ClientCC` — adresse du serveur avec `InetAddress` / `InetSocketAddress`

```java
String ip = (args.length > 0) ? args[0] : "localhost";
InetSocketAddress adresseServeur = new InetSocketAddress(InetAddress.getByName(ip), PORT);
Socket socket = new Socket();
socket.connect(adresseServeur, 5000);  // délai maximal de connexion : 5 s

System.out.println("<<< " + in.readLine());   // "Bienvenue ! Vous etes le client numero N"
```

## Tests locaux : plusieurs clients Java simultanés

Trois clients sont lancés **en même temps**, chacun envoyant une opération toutes les 0,6 s : le client 1 vers `localhost`, le client 2 vers `127.0.0.1` et le client 3 vers l'IP Wi-Fi `192.168.1.14`.

Console du serveur :

```
Serveur multi-thread demarre sur le port 1234, en attente de clients...
Connexion du client numero 1 depuis /127.0.0.1:65148
[client 1 /127.0.0.1:65148] 55 * 25  ->  55 * 25 = 1375
Connexion du client numero 2 depuis /127.0.0.1:65149
[client 2 /127.0.0.1:65149] 12+30  ->  12 + 30 = 42
Connexion du client numero 3 depuis /192.168.1.14:65152
[client 3 /192.168.1.14:65152] 100 - 1  ->  100 - 1 = 99
[client 1 /127.0.0.1:65148] 7 / 0  ->  Erreur : division par zero impossible
[client 3 /192.168.1.14:65152] deconnecte.
[client 1 /127.0.0.1:65148] deconnecte.
[client 2 /127.0.0.1:65149] 2,5 * 4  ->  2.5 * 4 = 10
[client 2 /127.0.0.1:65149] deconnecte.
```

Consoles des clients 1 et 2 :

```
Connecte au serveur 127.0.0.1:1234
<<< Bienvenue ! Vous etes le client numero 1
<<< Tapez une operation (ex. 55 * 25) ou 'fin' pour quitter.
Operation : 55 * 25
<<< 55 * 25 = 1375
Operation : 7 / 0
<<< Erreur : division par zero impossible
Operation : fin
<<< Au revoir client numero 1
```

```
Connecte au serveur 127.0.0.1:1234
<<< Bienvenue ! Vous etes le client numero 2
<<< Tapez une operation (ex. 55 * 25) ou 'fin' pour quitter.
Operation : 12+30
<<< 12 + 30 = 42
Operation : abc + 3
Syntaxe invalide, format attendu : nombre operateur nombre
Operation : 2,5 * 4
<<< 2.5 * 4 = 10
Operation : fin
<<< Au revoir client numero 2
```

**Interprétation :**

- Les trois clients sont connectés **en même temps**. Le client 2 est accepté alors que le client 1 n'a pas terminé, et les lignes des trois clients **s'entremêlent** dans la console du serveur. Les échanges sont bien traités en parallèle, par trois threads distincts.
- Chaque client reçoit **son propre numéro d'ordre** (1, 2, 3), attribué dans l'ordre d'arrivée des connexions.
- `getRemoteSocketAddress()` donne l'adresse IP **et** le port éphémère de chaque client. Le client 3, connecté via l'IP Wi-Fi, apparaît avec l'adresse `192.168.1.14` et non `127.0.0.1`.
- Les clients se déconnectent dans un ordre différent de leur ordre d'arrivée, sans perturber les autres.

## Test avec un client telnet

Telnet ouvre une simple connexion TCP en mode texte : ce qu'on tape est envoyé tel quel au serveur, et ce que le serveur renvoie est affiché. Sur cette machine, le client Telnet de Windows n'est pas activé (Panneau de configuration → Programmes → Activer ou désactiver des fonctionnalités Windows → Client Telnet). Le test a donc été réalisé avec un client TCP brut équivalent (`System.Net.Sockets.TcpClient` en PowerShell), qui comme telnet envoie les lignes **sans aucune validation** :

```
Bienvenue ! Vous etes le client numero 1
Tapez une operation (ex. 55 * 25) ou 'fin' pour quitter.
> 25 ** 55
Erreur : syntaxe invalide "25 ** 55"
> abc + 3
Erreur : syntaxe invalide "abc + 3"
> 34 * 55
34 * 55 = 1870
> fin
Au revoir client numero 1
```

Côté serveur :

```
Connexion du client numero 1 depuis /[0:0:0:0:0:0:0:1]:65191
[client 1 /[0:0:0:0:0:0:0:1]:65191] 25 ** 55  ->  Erreur : syntaxe invalide "25 ** 55"
[client 1 /[0:0:0:0:0:0:0:1]:65191] abc + 3  ->  Erreur : syntaxe invalide "abc + 3"
[client 1 /[0:0:0:0:0:0:0:1]:65191] 34 * 55  ->  34 * 55 = 1870
[client 1 /[0:0:0:0:0:0:0:1]:65191] deconnecte.
```

**Interprétation :** le serveur ne dépend pas du client Java : n'importe quel client TCP qui envoie des lignes de texte peut l'utiliser. L'adresse `[0:0:0:0:0:0:0:1]` est l'adresse de bouclage **IPv6** (équivalent de `127.0.0.1`), car `localhost` a été résolu en IPv6 par ce client. Les saisies invalides sont rejetées par le serveur lui-même. Avec la commande `telnet localhost 1234`, le comportement est identique.

## Tests en réseau local

Pour tester sur plusieurs machines du même réseau :

1. Sur la machine serveur, lancer `java -cp out serverPackage.ServeurMultiClients` et relever son adresse IP avec `ipconfig` (ici `192.168.1.14`).
2. Sur chaque machine cliente, lancer `java -cp out clientPackage.ClientCC 192.168.1.14`.
3. Autoriser Java dans le pare-feu Windows (réseaux privés) sur la machine serveur.

Le client 3 du test précédent passe déjà par l'adresse IP réelle `192.168.1.14`, et non par l'adresse de bouclage. Avec une seconde machine, le serveur afficherait l'adresse IP de cette machine.

---

# Activité 3-2 : Calculatrice multi-thread et accès concurrent

## Étape 1 : Calculatrice multi-thread par objets

Le service de l'Activité 2-2 (échange d'objets `Operation` / `Resultat`) est intégré au serveur multi-thread : chaque client est servi par un thread **`ClientProcess`**.

```java
// ServeurCalculatrice : boucle d'acceptation
CompteurOperations compteur = new CompteurOperations();   // UNE seule instance, partagée
while (true) {
    Socket socket = serverSocket.accept();
    numeroClient++;
    System.out.println("Connexion du client numero " + numeroClient + " depuis " + socket.getRemoteSocketAddress());
    new ClientProcess(socket, numeroClient, compteur).start();
}
```

```java
// ClientProcess.run()
ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
oos.flush();
ObjectInputStream ois = new ObjectInputStream(s.getInputStream());

oos.writeInt(numero);          // envoi du numéro d'ordre au client
oos.flush();

while (true) {
    Object recu;
    try { recu = ois.readObject(); } catch (EOFException e) { break; }   // client parti
    Operation op = (Operation) recu;
    Resultat resultat = Calculateur.calculer(op.getOperande1(), op.getOperateur(), op.getOperande2());
    oos.writeObject(resultat);
    oos.flush();

    int total = compteur.incrementer();         // section critique (synchronisée)
    System.out.println(prefixe + op + " = " + resultat + "   | total des operations traitees : " + total);
}
```

Le client `ClientCalculatrice` reprend celui de l'Activité 2-2. Il lit d'abord son numéro (`ois.readInt()`), puis envoie des objets `Operation` et reçoit des objets `Resultat`. Il prend aussi l'adresse IP du serveur en argument (`InetSocketAddress`).

## Étape 3 : Gestion de l'accès concurrent

Le compteur global est une **ressource critique** : une seule instance est partagée par tous les threads `ClientProcess`.

```java
public class CompteurOperations {
    private int total = 0;

    public synchronized int incrementer() {
        total++;
        return total;
    }
}
```

`total++` n'est **pas atomique**. Il se décompose en trois opérations : lire `total`, ajouter 1, écrire le résultat. Sans protection, deux threads peuvent lire la même valeur (par ex. 5) et écrire tous les deux 6 : une incrémentation est alors **perdue**. Le mot-clé `synchronized` pose un verrou sur l'objet compteur, de sorte qu'un seul thread à la fois exécute `incrementer()`. L'incrémentation et la lecture de la nouvelle valeur forment ainsi un bloc indivisible.

## Test : 3 clients en parallèle

Trois clients sont lancés simultanément. Chacun envoie 20 opérations (`i * 10`, `i * 100` et `i * 1000` pour i = 1..20), soit **60 opérations** au total. Le client 3 passe par l'IP Wi-Fi.

Console du serveur (début et fin) :

```
Serveur calculatrice multi-thread demarre sur le port 1234, en attente de clients...
Connexion du client numero 1 depuis /127.0.0.1:65196
Connexion du client numero 2 depuis /127.0.0.1:65197
Connexion du client numero 3 depuis /192.168.1.14:65198
[client 2 /127.0.0.1:65197] 1 * 100 = 100   | total des operations traitees : 1
[client 1 /127.0.0.1:65196] 1 * 10 = 10   | total des operations traitees : 3
[client 3 /192.168.1.14:65198] 1 * 1000 = 1000   | total des operations traitees : 2
[client 1 /127.0.0.1:65196] 2 * 10 = 20   | total des operations traitees : 4
[client 3 /192.168.1.14:65198] 2 * 1000 = 2000   | total des operations traitees : 5
[client 2 /127.0.0.1:65197] 2 * 100 = 200   | total des operations traitees : 6
[client 2 /127.0.0.1:65197] 3 * 100 = 300   | total des operations traitees : 7
[client 1 /127.0.0.1:65196] 3 * 10 = 30   | total des operations traitees : 8
...
[client 3 /192.168.1.14:65198] 19 * 1000 = 19000   | total des operations traitees : 56
[client 2 /127.0.0.1:65197] 19 * 100 = 1900   | total des operations traitees : 57
[client 1 /127.0.0.1:65196] 20 * 10 = 200   | total des operations traitees : 58
[client 3 /192.168.1.14:65198] 20 * 1000 = 20000   | total des operations traitees : 59
[client 2 /127.0.0.1:65197] 20 * 100 = 2000   | total des operations traitees : 60
[client 1 /127.0.0.1:65196] deconnecte.
[client 2 /127.0.0.1:65197] deconnecte.
[client 3 /192.168.1.14:65198] deconnecte.
```

Console du client 1 (extrait) :

```
Connecte au serveur 127.0.0.1:1234 : je suis le client numero 1
Operation (ex. 55 * 25, 'fin' pour quitter) : 1 * 10
>>> 1 * 10 = 10
Operation (ex. 55 * 25, 'fin' pour quitter) : 2 * 10
>>> 2 * 10 = 20
...
Operation (ex. 55 * 25, 'fin' pour quitter) : 20 * 10
>>> 20 * 10 = 200
Operation (ex. 55 * 25, 'fin' pour quitter) : fin
Client numero 1 : fin de la communication.
```

**Interprétation :**

- Les opérations des trois clients sont traitées **en parallèle** et s'entremêlent. Chaque client ne reçoit que ses propres résultats : les communications restent **indépendantes**, car chaque thread a sa socket et ses flux.
- Le compteur termine exactement à **60** = 3 × 20. Les 60 valeurs de 1 à 60 apparaissent chacune **une seule fois**, sans doublon ni trou : aucune incrémentation n'a été perdue, le compteur est cohérent.
- Les valeurs ne s'affichent pas toujours dans l'ordre (3 avant 2 au début). C'est normal : l'incrémentation est protégée, mais l'affichage (`println`) se fait juste après, hors du verrou, et un autre thread peut afficher entre-temps. Chaque valeur reste unique et correcte. Seul l'ordre d'affichage dépend de l'ordonnancement des threads.

## Démonstration : avec et sans `synchronized`

Avec seulement 60 opérations espacées dans le temps, les conflits sont rares. Pour les rendre visibles, `DemoConcurrence` lance **4 threads** qui incrémentent chacun **100 000 fois** le même compteur, d'abord sans protection, puis avec `CompteurOperations` :

```
java -cp out serverPackage.DemoConcurrence
Sans synchronized : total = 111750 (attendu 400000)
Avec synchronized : total = 400000 (attendu 400000)
```

**Interprétation :** sans synchronisation, près de **72 % des incrémentations sont perdues**, car plusieurs threads lisent et écrivent la même valeur en même temps. La valeur obtenue change d'une exécution à l'autre. Avec `synchronized`, le résultat est toujours exact. C'est pourquoi le compteur partagé entre les `ClientProcess` doit obligatoirement être synchronisé.

---

# Synthèse

| | TP1 / TP2 | TP3 |
|---|---|---|
| Clients par exécution du serveur | 1 | illimité |
| `accept()` | appelé une seule fois | appelé en boucle par le thread principal |
| Dialogue avec un client | dans le thread principal | dans un thread dédié (`ClientProcessCC` / `ClientProcess`) |
| Un client lent bloque les autres ? | oui (un seul client possible) | non |
| Données partagées entre clients | aucune | compteur global → accès `synchronized` |

- Un serveur multi-thread sépare deux rôles : **accepter** les connexions (thread principal) et **dialoguer** avec chaque client (un thread par client).
- Chaque thread possède sa propre `Socket` et ses propres flux. Les échanges avec différents clients sont donc indépendants.
- Dès que des threads partagent une donnée modifiable, comme le compteur global, ses accès doivent être **synchronisés**, sinon des mises à jour sont perdues.
- Limite de cette approche : un thread est créé par client. Avec un très grand nombre de clients, il faudrait plutôt un **pool de threads** (`ExecutorService`) pour limiter le nombre de threads créés.
