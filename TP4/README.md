# TP4 — Sockets en mode non-connecté UDP : service de chat

| | |
|---|---|
| **Matière** | Développement d'applications réparties |
| **Auditoire** | LSI 3 — Faculté des Sciences de Sfax |
| **Année universitaire** | 2026 – 2027 |
| **Étudiant(e)** | _Nom Prénom à compléter_ |

Ce dossier contient une application de **chat client/serveur** basée sur le protocole **UDP** (`DatagramSocket` / `DatagramPacket`), réalisée en deux étapes :

- **Étape 2** : chat **unidirectionnel**, où le client envoie et le serveur affiche ;
- **Étape 3** : chat **bidirectionnel**, où le serveur diffuse chaque message à tous les autres clients.

---

## Structure du projet

```
TP4/
├── README.md
└── src/
    ├── serverPackage/
    │   ├── ServeurUDP.java        # Etape 2 : reception et affichage
    │   └── ServeurChatUDP.java    # Etape 3 : liste des clients + diffusion
    └── clientPackage/
        ├── ClientUDP.java         # Etape 2 : envoi seul
        └── ClientChatUDP.java     # Etape 3 : envoi + reception (2 threads)
```

## Compilation et exécution

Depuis le dossier `TP4/` :

```powershell
javac -encoding UTF-8 -d out src/serverPackage/*.java src/clientPackage/*.java

# Étape 2
java -cp out serverPackage.ServeurUDP
java -cp out clientPackage.ClientUDP [adresseIPServeur]

# Étape 3 : un serveur, puis plusieurs clients (un terminal chacun)
java -cp out serverPackage.ServeurChatUDP
java -cp out clientPackage.ClientChatUDP [adresseIPServeur]
```

Le serveur utilise le **port UDP 1234**. Dans le client, on tape d'abord son nom d'utilisateur, puis ses messages, puis `quit` pour quitter.

---

## Rappel : TCP et UDP

| | TCP (TP1 à TP3) | UDP (TP4) |
|---|---|---|
| Connexion | Oui : `accept()` / `new Socket(...)` | **Non** : aucune connexion préalable |
| Classes | `ServerSocket`, `Socket` | `DatagramSocket`, `DatagramPacket` |
| Données | Flux continu d'octets | **Datagrammes** indépendants (paquets) |
| Adresse du destinataire | Fixée une fois à la connexion | Portée par **chaque** datagramme |
| Fiabilité / ordre | Garantis | **Non garantis** : perte, doublon ou désordre possibles |
| Coût | Plus lourd (handshake, accusés de réception) | Léger et rapide |

En UDP, la même `DatagramSocket` peut envoyer à n'importe quelle adresse et recevoir de n'importe qui. C'est le serveur qui doit mémoriser les adresses de ses clients s'il veut leur répondre.

---

# Étape 2 : Chat unidirectionnel avec un seul client

## Serveur — `ServeurUDP`

```java
// Liaison au port 1234 avec InetSocketAddress (pas d'accept() en UDP)
try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(PORT))) {
    System.out.println("Serveur UDP en ecoute sur le port " + PORT + "...");

    byte[] buffer = new byte[TAILLE_MAX];
    while (true) {
        DatagramPacket paquet = new DatagramPacket(buffer, buffer.length);
        socket.receive(paquet);                  // bloquant jusqu'à l'arrivée d'un datagramme

        // Seuls les getLength() premiers octets du tampon contiennent le message
        String message = new String(paquet.getData(), 0, paquet.getLength(), StandardCharsets.UTF_8);
        System.out.println("Message de " + paquet.getAddress().getHostAddress() + ":" + paquet.getPort()
                + " -> " + message);
    }
}
```

- `receive()` remplit le `DatagramPacket` avec les données reçues **et** l'adresse de l'expéditeur (`getAddress()`, `getPort()`).
- Le tampon est réutilisé d'un paquet à l'autre. Il faut donc utiliser `getLength()` pour ne pas relire des octets d'un message précédent, plus long.

## Client — `ClientUDP`

```java
try (DatagramSocket socket = new DatagramSocket()) {        // port local choisi par le système
    InetSocketAddress serveur = new InetSocketAddress(InetAddress.getByName(ip), PORT);

    System.out.print("Nom d'utilisateur : ");
    String nom = clavier.nextLine().trim();

    while (clavier.hasNextLine()) {
        String texte = clavier.nextLine();
        if (texte.equalsIgnoreCase("quit")) break;
        byte[] donnees = ("[" + nom + "] : " + texte).getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(donnees, donnees.length, serveur));   // l'adresse est dans le paquet
    }
}
```

## Exécution

Console du client :

```
Nom d'utilisateur : Alice
Bonjour Alice, tapez vos messages ('quit' pour quitter).
Bonjour serveur
(envoye) Bonjour serveur
Ceci est un test UDP
(envoye) Ceci est un test UDP
quit
Fin du client.
```

Console du serveur :

```
Serveur UDP en ecoute sur le port 1234...
Message de 127.0.0.1:50501 -> [Alice] : Bonjour serveur
Message de 127.0.0.1:50501 -> [Alice] : Ceci est un test UDP
```

**Interprétation :**

- Les messages arrivent au serveur préfixés par le nom d'utilisateur (`[Alice] : ...`), avec l'IP et le port de l'expéditeur.
- Le port `50501` est un port éphémère choisi par le système à la création de la `DatagramSocket` du client. Il reste le même pour tous les messages de ce client, ce qui permettra à l'étape 3 d'identifier chaque client par son couple (IP, port).
- Aucune connexion n'est établie. Si le client est lancé **avant** le serveur, il n'obtient aucune erreur (contrairement au `Connection refused` du TP1) : ses datagrammes sont simplement **perdus**. UDP ne garantit pas la remise.
- La communication est **unidirectionnelle** : le serveur ne répond pas, et le client ne fait qu'envoyer.

---

# Étape 3 : Chat bidirectionnel et diffusion à plusieurs clients

## Serveur — `ServeurChatUDP`

Le serveur « simule » la connexion en tenant une **liste des adresses** (`SocketAddress` = IP + port) des clients qui lui ont écrit :

```java
private static final Set<SocketAddress> clients = new LinkedHashSet<>();

while (true) {
    DatagramPacket paquet = new DatagramPacket(buffer, buffer.length);
    socket.receive(paquet);
    SocketAddress expediteur = paquet.getSocketAddress();
    String message = new String(paquet.getData(), 0, paquet.getLength(), StandardCharsets.UTF_8);

    // 1) enregistrement de l'expéditeur s'il est nouveau (un Set ignore les doublons)
    if (clients.add(expediteur)) {
        System.out.println("Nouveau client enregistre : " + expediteur + " (" + clients.size() + " client(s))");
    }

    if (message.startsWith("/join ")) {            // arrivée
        String nom = message.substring(6);
        envoyer(socket, "*** Bienvenue " + nom + " ! " + clients.size() + " participant(s) dans le chat ***", expediteur);
        diffuser(socket, "*** " + nom + " a rejoint le chat ***", expediteur);
    } else if (message.startsWith("/quit ")) {     // départ
        String nom = message.substring(6);
        clients.remove(expediteur);
        diffuser(socket, "*** " + nom + " a quitte le chat ***", expediteur);
    } else {                                        // message normal
        System.out.println("Message de " + expediteur + " -> " + message);
        diffuser(socket, message, expediteur);     // 2) retransmission aux autres
    }
}
```

```java
/** Envoie le message à tous les clients enregistrés, sauf l'expéditeur. */
private static void diffuser(DatagramSocket socket, String message, SocketAddress expediteur) throws IOException {
    for (SocketAddress client : clients) {
        if (!client.equals(expediteur)) {
            envoyer(socket, message, client);
        }
    }
}
```

- `/join Nom` est envoyé automatiquement au lancement du client. Sans ce message, un client ne serait enregistré, et ne recevrait donc rien, qu'après avoir écrit son premier message.
- `/quit Nom` permet de retirer l'adresse du client de la liste. En UDP, le serveur n'a **aucun autre moyen** de savoir qu'un client est parti, car il n'y a pas de fermeture de connexion.
- Le serveur n'a qu'un seul thread : la liste n'est jamais modifiée en parallèle et ne nécessite pas de synchronisation.

## Client — `ClientChatUDP` : deux threads

Le client doit **recevoir en temps réel** pendant que l'utilisateur tape. Or `clavier.nextLine()` et `socket.receive()` sont tous les deux **bloquants**. Un seul thread ne peut pas attendre les deux à la fois, c'est pourquoi le client en utilise deux, sur la même `DatagramSocket` :

```java
// Thread de réception : affiche les messages dès leur arrivée
Thread reception = new Thread(() -> recevoir(socket), "reception");
reception.setDaemon(true);
reception.start();

envoyer(socket, "/join " + nom, serveur);          // le serveur enregistre notre adresse

// Thread principal : lecture du clavier et envoi
while (clavier.hasNextLine()) {
    String texte = clavier.nextLine();
    if (texte.equalsIgnoreCase("quit")) break;
    envoyer(socket, "[" + nom + "] : " + texte, serveur);
}
envoyer(socket, "/quit " + nom, serveur);
```

```java
private static void recevoir(DatagramSocket socket) {
    byte[] buffer = new byte[TAILLE_MAX];
    try {
        while (true) {
            DatagramPacket paquet = new DatagramPacket(buffer, buffer.length);
            socket.receive(paquet);
            System.out.println(">> " + new String(paquet.getData(), 0, paquet.getLength(), StandardCharsets.UTF_8));
        }
    } catch (SocketException e) {
        // socket fermée par le thread principal : fin normale de la réception
    }
}
```

Le thread de réception est un **démon** : il n'empêche pas la JVM de s'arrêter quand le thread principal se termine. La fermeture de la socket (`try-with-resources`) débloque son `receive()` avec une `SocketException`, qui marque la fin normale.

## Test du service de chat : trois clients

Scénario : Alice, Bob et Carla rejoignent le chat à 0,5 s d'intervalle. Carla se connecte via l'IP Wi-Fi `192.168.1.14`. Ils échangent chacun un message, puis Bob quitte, Alice écrit encore, et tout le monde quitte.

Console du serveur :

```
Serveur de chat UDP en ecoute sur le port 1234...
Nouveau client enregistre : /127.0.0.1:55015 (1 client(s))
   diffuse a 0 client(s)
Nouveau client enregistre : /127.0.0.1:63885 (2 client(s))
   diffuse a 1 client(s)
Nouveau client enregistre : /192.168.1.14:63886 (3 client(s))
   diffuse a 2 client(s)
Message de /127.0.0.1:55015 -> [Alice] : Bonjour tout le monde
   diffuse a 2 client(s)
Message de /127.0.0.1:63885 -> [Bob] : Salut Alice !
   diffuse a 2 client(s)
Message de /192.168.1.14:63886 -> [Carla] : Coucou, je suis sur l'IP Wi-Fi
   diffuse a 2 client(s)
Client retire : /127.0.0.1:63885 (2 client(s))
   diffuse a 2 client(s)
Message de /127.0.0.1:55015 -> [Alice] : Bob est parti
   diffuse a 1 client(s)
Client retire : /127.0.0.1:55015 (1 client(s))
   diffuse a 1 client(s)
Client retire : /192.168.1.14:63886 (0 client(s))
   diffuse a 0 client(s)
```

Console d'**Alice** :

```
Nom d'utilisateur : Alice
Tapez vos messages ('quit' pour quitter).
>> *** Bienvenue Alice ! 1 participant(s) dans le chat ***
>> *** Bob a rejoint le chat ***
>> *** Carla a rejoint le chat ***
Bonjour tout le monde
>> [Bob] : Salut Alice !
>> [Carla] : Coucou, je suis sur l'IP Wi-Fi
>> *** Bob a quitte le chat ***
Bob est parti
quit
Vous avez quitte le chat.
```

Console de **Bob** :

```
Nom d'utilisateur : Bob
Tapez vos messages ('quit' pour quitter).
>> *** Bienvenue Bob ! 2 participant(s) dans le chat ***
>> *** Carla a rejoint le chat ***
>> [Alice] : Bonjour tout le monde
Salut Alice !
>> [Carla] : Coucou, je suis sur l'IP Wi-Fi
quit
Vous avez quitte le chat.
```

Console de **Carla** :

```
Nom d'utilisateur : Carla
Tapez vos messages ('quit' pour quitter).
>> *** Bienvenue Carla ! 3 participant(s) dans le chat ***
>> [Alice] : Bonjour tout le monde
>> [Bob] : Salut Alice !
Coucou, je suis sur l'IP Wi-Fi
>> *** Bob a quitte le chat ***
>> [Alice] : Bob est parti
>> *** Alice a quitte le chat ***
quit
Vous avez quitte le chat.
```

**Interprétation :**

- **Diffusion** : chaque message est reçu par **tous les autres** clients et **jamais** par son expéditeur. Alice ne voit pas son propre « Bonjour tout le monde », mais Bob et Carla le reçoivent.
- **Enregistrement** : chaque client est identifié par son couple (IP, port). Le serveur compte 1, 2 puis 3 clients au fur et à mesure des `/join`. Carla apparaît avec l'adresse `192.168.1.14`, puisqu'elle passe par l'interface Wi-Fi.
- **Temps réel** : les messages des autres s'affichent (`>>`) dès leur arrivée, même quand l'utilisateur ne tape rien, grâce au thread de réception.
- **Départ** : après le `/quit` de Bob, le serveur retire son adresse. Le message « Bob est parti » n'est diffusé qu'à **1** client (Carla), et Bob ne le reçoit pas. Bob ne reçoit pas non plus l'annonce de son propre départ.
- Chaque client ne reçoit que les messages postérieurs à son arrivée. Bob n'a pas reçu « Alice a rejoint le chat », car il n'était pas encore enregistré. Le serveur ne garde aucun historique.

> **Remarque sur les accents :** les messages sont encodés en UTF-8 dans les datagrammes. Selon la page de code de la console Windows, les caractères accentués saisis au clavier peuvent être mal lus par `Scanner`. Les tests ci-dessus utilisent donc des messages sans accents. La commande `chcp 65001` passe la console en UTF-8.

---

# Synthèse

- En UDP, il n'y a **ni connexion ni flux** : chaque `DatagramPacket` est un message autonome qui porte son adresse de destination. Une seule `DatagramSocket` côté serveur suffit pour dialoguer avec **tous** les clients, sans thread par client (contrairement au serveur TCP du TP3).
- La notion de « client connecté » n'existe pas : le serveur doit la **simuler** en mémorisant les adresses (`SocketAddress`) des expéditeurs, et prévoir un message explicite (`/quit`) pour les oublier.
- Côté client, recevoir en temps réel tout en lisant le clavier exige **deux threads**, car `receive()` et `nextLine()` sont bloquants.
- UDP ne garantit ni la remise ni l'ordre des datagrammes. C'est acceptable pour un chat en réseau local, où les pertes sont rares et sans gravité, mais un service exigeant la fiabilité (comme la calculatrice) reste mieux servi par TCP.
- Taille limitée : un datagramme est lu dans un tampon de taille fixe (1024 octets ici). Un message plus long serait **tronqué**.
