# TP2 — Les sockets en mode connecté TCP : E/R de CC & objets

| | |
|---|---|
| **Matière** | Développement d'applications réparties |
| **Auditoire** | LSI 3 — Faculté des Sciences de Sfax |
| **Année universitaire** | 2026 – 2027 |
| **Étudiant(e)** | _Nom Prénom à compléter_ |

Ce dossier contient le code Java et le compte rendu des deux activités du TP 2 :

- **Activité 2-1** : échange de **chaînes de caractères** (CC) entre le client et le serveur ;
- **Activité 2-2** : échange d'**objets** (`Serializable`) entre le client et le serveur.

Les deux activités reprennent le service de calcul du TP1, qui était limité à un seul entier (`x * 5`). Désormais, le client envoie une **opération complète**, par exemple `55 * 25`.

---

## Structure du projet

```
TP2/
├── README.md
├── captures/                               # captures d'ecran du compte rendu
└── src/
    ├── commonPackage/                      # classes partagees par le client et le serveur
    │   ├── SyntaxeOperation.java           # validation de la syntaxe "nombre operateur nombre"
    │   ├── Operation.java                  # Activite 2-2 : objet envoye (Serializable)
    │   └── Resultat.java                   # Activite 2-2 : objet renvoye (Serializable)
    ├── serverPackage/
    │   ├── ServerOperationCC.java          # Activite 2-1 : BufferedReader / PrintWriter
    │   └── ServerOperationObjet.java       # Activite 2-2 : ObjectInputStream / ObjectOutputStream
    └── clientPackage/
        ├── ClientOperationCC.java
        └── ClientOperationObjet.java
```

Le serveur écoute sur le **port 1234**. Le client prend en argument facultatif l'adresse du serveur (`localhost` par défaut).

> **Étape 1 (préparation de l'espace de travail)** : le répertoire de travail demandé, `votreNomPrenom_LSI3_DevAppReparties`, correspond ici au dépôt GitHub qui regroupe tous les TP. Le TP2 se trouve dans le dossier `TP2/`.

## Compilation et exécution

Depuis le dossier `TP2/` :

```powershell
javac -encoding UTF-8 -d out src/commonPackage/*.java src/serverPackage/*.java src/clientPackage/*.java
```

Puis, dans **deux terminaux différents** : d'abord le serveur, ensuite le client.

```powershell
# Activité 2-1
java -cp out serverPackage.ServerOperationCC        # terminal 1
java -cp out clientPackage.ClientOperationCC        # terminal 2

# Activité 2-2
java -cp out serverPackage.ServerOperationObjet     # terminal 1
java -cp out clientPackage.ClientOperationObjet     # terminal 2
```

Dans le client, on tape une opération par ligne (`55 * 25`, `12+30`, `7 / 2`…), puis `fin` pour quitter.

---

# Activité 2-1 : Échange de chaînes de caractères

## Problème de départ

Avec `InputStream.read()` / `OutputStream.write(int)` (Activité 1-2), un seul **octet** est échangé à la fois. Il est donc impossible de transmettre une opération comme `34 * 55`.

## Solution : les flux de traitement de caractères

On « enveloppe » les flux d'octets de la socket dans des flux de **caractères** du package `java.io` :

| Rôle | Classe | Méthode utilisée |
|---|---|---|
| Lecture | `BufferedReader` ← `InputStreamReader` ← `socket.getInputStream()` | `readLine()` : lit une ligne entière |
| Écriture | `PrintWriter` ← `OutputStreamWriter` ← `socket.getOutputStream()` | `println()` : écrit une ligne entière |

- `InputStreamReader` / `OutputStreamWriter` convertissent les octets en caractères (encodage UTF-8 imposé des deux côtés).
- `BufferedReader` permet de lire une ligne complète, jusqu'au retour à la ligne.
- `PrintWriter` est créé avec `autoFlush = true`, donc chaque `println()` est envoyé immédiatement.
- Le **retour à la ligne** sert de séparateur entre les messages. C'est ce qui permet de savoir où s'arrête chaque élément.

## Protocole d'échange

Pour chaque opération :

| Étape | Sens | Contenu |
|---|---|---|
| a) | client → serveur | 3 lignes : premier opérande (`55`), opérateur (`*`), deuxième opérande (`25`) |
| b) | serveur | calcul de l'opération |
| c) | serveur → client | 1 ligne : `55 * 25 = 1375`, ou `Erreur : ...` |
| d) | client | affichage du résultat |

La communication se termine quand le client tape `fin`. Le client ferme alors sa socket, et `readLine()` renvoie `null` côté serveur.

## Validation de la syntaxe (côté client)

Avant tout envoi, le client vérifie la saisie avec une expression régulière (classe `SyntaxeOperation`) :

```java
private static final String NOMBRE = "([+-]?\\d+(?:[.,]\\d+)?)";
private static final Pattern FORMAT = Pattern.compile(
        "^\\s*" + NOMBRE + "\\s*([-+*/])\\s*" + NOMBRE + "\\s*$");
```

- **nombre** : entier ou décimal, avec un signe facultatif (`55`, `-8`, `2.5`, `2,5`) ;
- **opérateur** : exactement **un** caractère parmi `+ - * /` ;
- les espaces autour de l'opérateur sont facultatifs (`12+30` est accepté).

Une saisie invalide comme `25 ** 55` ou `abc + 3` est refusée, et **rien n'est envoyé** au serveur. Le serveur revérifie quand même les données reçues (conversion en nombre, opérateur connu, division par zéro), car il ne doit pas faire confiance aveuglément au client.

## Code

### Serveur — `ServerOperationCC` (extrait)

```java
BufferedReader in = new BufferedReader(
        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
PrintWriter out = new PrintWriter(
        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

while (true) {
    // a) réception des trois éléments de l'opération (une ligne chacun)
    String operande1 = in.readLine();
    if (operande1 == null) { // le client a fermé la connexion
        System.out.println("Le client s'est deconnecte : fin de la communication.");
        break;
    }
    String operateur = in.readLine();
    String operande2 = in.readLine();

    // b) calcul  -  c) envoi du résultat (une ligne de texte)
    String reponse = calculer(operande1, operateur, operande2);
    out.println(reponse);
}
```

```java
static String calculer(String texte1, String operateur, String texte2) {
    double x, y;
    try {
        x = Double.parseDouble(texte1);
        y = Double.parseDouble(texte2);
    } catch (NumberFormatException e) {
        return "Erreur : operande invalide";
    }
    double resultat;
    switch (operateur) {
        case "+": resultat = x + y; break;
        case "-": resultat = x - y; break;
        case "*": resultat = x * y; break;
        case "/":
            if (y == 0) return "Erreur : division par zero impossible";
            resultat = x / y;
            break;
        default:
            return "Erreur : operateur inconnu \"" + operateur + "\"";
    }
    return formater(x) + " " + operateur + " " + formater(y) + " = " + formater(resultat);
}
```

### Client — `ClientOperationCC` (extrait)

```java
String saisie = clavier.nextLine().trim();

// Validation de la syntaxe AVANT l'envoi
String[] elements = SyntaxeOperation.analyser(saisie);
if (elements == null) {
    System.out.println("Syntaxe invalide : \"" + saisie + "\" ...");
    continue;
}

// a) envoi des trois éléments de l'opération
out.println(elements[0]); // premier opérande
out.println(elements[1]); // opérateur
out.println(elements[2]); // deuxième opérande

// d) lecture et affichage du résultat
String reponse = in.readLine();
System.out.println(">>> Resultat recu du serveur : " + reponse);
```

## Exécution

Saisies testées : `55 * 25`, `25 ** 55`, `abc + 3`, `12+30`, `5 - -8`, `2,5 * 4`, `7 / 2`, `7 / 0`, puis `fin`.

Console du client :

```
Je suis un client pas encore connecte...
je suis un client connecte au serveur localhost:1234
Operation (ex. 55 * 25, 'fin' pour quitter) : 55 * 25
>>> Resultat recu du serveur : 55 * 25 = 1375
Operation (ex. 55 * 25, 'fin' pour quitter) : 25 ** 55
Syntaxe invalide : "25 ** 55" (format attendu : nombre operateur nombre, operateurs + - * /)
Operation (ex. 55 * 25, 'fin' pour quitter) : abc + 3
Syntaxe invalide : "abc + 3" (format attendu : nombre operateur nombre, operateurs + - * /)
Operation (ex. 55 * 25, 'fin' pour quitter) : 12+30
>>> Resultat recu du serveur : 12 + 30 = 42
Operation (ex. 55 * 25, 'fin' pour quitter) : 5 - -8
>>> Resultat recu du serveur : 5 - -8 = 13
Operation (ex. 55 * 25, 'fin' pour quitter) : 2,5 * 4
>>> Resultat recu du serveur : 2.5 * 4 = 10
Operation (ex. 55 * 25, 'fin' pour quitter) : 7 / 2
>>> Resultat recu du serveur : 7 / 2 = 3.5
Operation (ex. 55 * 25, 'fin' pour quitter) : 7 / 0
>>> Erreur recue du serveur : division par zero impossible
Operation (ex. 55 * 25, 'fin' pour quitter) : fin
Fin de la communication. Socket du client fermee.
```

Console du serveur :

```
Je suis un serveur en attente de la connexion d'un client
un client est connecte
Recu : operande1 = "55", operateur = "*", operande2 = "25"
Resultat envoye : 55 * 25 = 1375
Recu : operande1 = "12", operateur = "+", operande2 = "30"
Resultat envoye : 12 + 30 = 42
Recu : operande1 = "5", operateur = "-", operande2 = "-8"
Resultat envoye : 5 - -8 = 13
Recu : operande1 = "2.5", operateur = "*", operande2 = "4"
Resultat envoye : 2.5 * 4 = 10
Recu : operande1 = "7", operateur = "/", operande2 = "2"
Resultat envoye : 7 / 2 = 3.5
Recu : operande1 = "7", operateur = "/", operande2 = "0"
Resultat envoye : Erreur : division par zero impossible
Le client s'est deconnecte : fin de la communication.
Sockets du serveur fermees.
```

**Interprétation :**

- Les opérations valides sont calculées correctement, y compris avec des nombres négatifs (`5 - -8 = 13`), décimaux (`2,5 * 4 = 10`) et des résultats supérieurs à 255 (`1375`). La limite d'un octet du TP1 a disparu.
- `25 ** 55` et `abc + 3` n'apparaissent **pas** dans la console du serveur : la validation côté client les a bloqués avant l'envoi.
- La division par zéro est détectée par le serveur, qui renvoie un message d'erreur au lieu d'un résultat.
- Quand le client tape `fin`, il ferme sa socket. `readLine()` renvoie alors `null` côté serveur, qui s'arrête proprement.

**Limite de cette démarche :** tout circule sous forme de **texte**. Le serveur doit reconvertir les chaînes en nombres (`Double.parseDouble`) et le résultat repart lui aussi en texte. Il faut définir soi-même le format des messages (une ligne par élément), et une erreur de format n'est détectée qu'à l'exécution. L'Activité 2-2 lève cette limite en échangeant directement des objets.

---

# Activité 2-2 : Échange d'objets

## Principe : la sérialisation

La **sérialisation** transforme un objet Java en une suite d'octets, qui peut circuler dans une socket. La **désérialisation** reconstruit l'objet de l'autre côté.

| Rôle | Classe | Méthode utilisée |
|---|---|---|
| Écriture | `ObjectOutputStream` ← `socket.getOutputStream()` | `writeObject(objet)` |
| Lecture | `ObjectInputStream` ← `socket.getInputStream()` | `readObject()` (renvoie un `Object` à caster) |

Pour être sérialisable, la classe doit implémenter l'interface **`java.io.Serializable`**. C'est une interface de marquage : elle ne contient aucune méthode. La classe doit aussi être présente, **dans le même package et avec le même `serialVersionUID`**, chez le client et chez le serveur. C'est pourquoi `Operation` et `Resultat` sont placées dans `commonPackage`.

## a) Classe sérialisable `Operation`

```java
package commonPackage;

import java.io.Serializable;

public class Operation implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double operande1;
    private final char operateur;
    private final double operande2;

    public Operation(double operande1, char operateur, double operande2) {
        this.operande1 = operande1;
        this.operateur = operateur;
        this.operande2 = operande2;
    }

    public double getOperande1() { return operande1; }
    public char getOperateur()   { return operateur; }
    public double getOperande2() { return operande2; }
}
```

Le résultat est renvoyé lui aussi sous forme d'objet, avec la classe `Resultat` (`Serializable`). Elle contient soit la valeur calculée, soit un message d'erreur :

```java
public class Resultat implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean succes;
    private final double valeur;
    private final String message;

    public static Resultat ok(double valeur)      { return new Resultat(true, valeur, null); }
    public static Resultat erreur(String message) { return new Resultat(false, 0, message); }
    // getters ...
}
```

## Ordre de création des flux (important)

Le constructeur d'`ObjectInputStream` **bloque** jusqu'à recevoir l'en-tête envoyé par l'`ObjectOutputStream` de l'autre côté. Si le client et le serveur créaient tous les deux leur `ObjectInputStream` en premier, chacun attendrait l'autre indéfiniment (**interblocage**). Des deux côtés, on crée donc d'abord l'`ObjectOutputStream`, on le vide avec `flush()`, puis on crée l'`ObjectInputStream` :

```java
ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
oos.flush();
ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
```

## Code

### Client — `ClientOperationObjet` (extrait)

```java
String[] elements = SyntaxeOperation.analyser(saisie);   // même validation qu'en 2-1
if (elements == null) { ... continue; }

// b) création et envoi de l'objet contenant les arguments de l'opération
Operation operation = new Operation(Double.parseDouble(elements[0]),
        elements[1].charAt(0), Double.parseDouble(elements[2]));
oos.writeObject(operation);
oos.flush();

// g) lecture et affichage du résultat
Resultat resultat = (Resultat) ois.readObject();
if (resultat.isSucces()) {
    System.out.println(">>> Resultat recu du serveur : " + operation + " = "
            + SyntaxeOperation.formater(resultat.getValeur()));
} else {
    System.out.println(">>> Erreur recue du serveur : " + resultat.getMessage());
}
```

### Serveur — `ServerOperationObjet` (extrait)

```java
while (true) {
    // c) réception de l'objet
    Object recu;
    try {
        recu = ois.readObject();
    } catch (EOFException e) { // le client a fermé la connexion
        System.out.println("Le client s'est deconnecte : fin de la communication.");
        break;
    }
    Operation operation = (Operation) recu;

    // d) récupération des données  -  e) calcul
    Resultat resultat = calculer(operation.getOperande1(), operation.getOperateur(),
            operation.getOperande2());

    // f) envoi du résultat sous forme d'objet
    oos.writeObject(resultat);
    oos.flush();
}
```

```java
static Resultat calculer(double x, char operateur, double y) {
    switch (operateur) {
        case '+': return Resultat.ok(x + y);
        case '-': return Resultat.ok(x - y);
        case '*': return Resultat.ok(x * y);
        case '/':
            if (y == 0) return Resultat.erreur("division par zero impossible");
            return Resultat.ok(x / y);
        default:
            return Resultat.erreur("operateur inconnu '" + operateur + "'");
    }
}
```

## Exécution

Mêmes saisies que pour l'Activité 2-1.

Console du client :

```
Je suis un client pas encore connecte...
je suis un client connecte au serveur localhost:1234
Operation (ex. 55 * 25, 'fin' pour quitter) : 55 * 25
>>> Resultat recu du serveur : 55 * 25 = 1375
Operation (ex. 55 * 25, 'fin' pour quitter) : 25 ** 55
Syntaxe invalide : "25 ** 55" (format attendu : nombre operateur nombre, operateurs + - * /)
Operation (ex. 55 * 25, 'fin' pour quitter) : abc + 3
Syntaxe invalide : "abc + 3" (format attendu : nombre operateur nombre, operateurs + - * /)
Operation (ex. 55 * 25, 'fin' pour quitter) : 12+30
>>> Resultat recu du serveur : 12 + 30 = 42
Operation (ex. 55 * 25, 'fin' pour quitter) : 5 - -8
>>> Resultat recu du serveur : 5 - -8 = 13
Operation (ex. 55 * 25, 'fin' pour quitter) : 2,5 * 4
>>> Resultat recu du serveur : 2.5 * 4 = 10
Operation (ex. 55 * 25, 'fin' pour quitter) : 7 / 2
>>> Resultat recu du serveur : 7 / 2 = 3.5
Operation (ex. 55 * 25, 'fin' pour quitter) : 7 / 0
>>> Erreur recue du serveur : division par zero impossible
Operation (ex. 55 * 25, 'fin' pour quitter) : fin
Fin de la communication. Socket du client fermee.
```

Console du serveur :

```
Je suis un serveur en attente de la connexion d'un client
un client est connecte
Objet recu : commonPackage.Operation [55 * 25]
Objet Resultat envoye : 1375
Objet recu : commonPackage.Operation [12 + 30]
Objet Resultat envoye : 42
Objet recu : commonPackage.Operation [5 - -8]
Objet Resultat envoye : 13
Objet recu : commonPackage.Operation [2.5 * 4]
Objet Resultat envoye : 10
Objet recu : commonPackage.Operation [7 / 2]
Objet Resultat envoye : 3.5
Objet recu : commonPackage.Operation [7 / 0]
Objet Resultat envoye : Erreur : division par zero impossible
Le client s'est deconnecte : fin de la communication.
Sockets du serveur fermees.
```

**Interprétation :**

- Le serveur reçoit bien un objet de type `commonPackage.Operation`, reconstruit à l'identique à partir des octets reçus. Il en récupère directement les champs typés (`double`, `char`) avec les getters, **sans aucune conversion de texte**.
- Le résultat revient au client sous forme d'objet `Resultat`. Le client sait immédiatement s'il s'agit d'une valeur ou d'une erreur (`isSucces()`), sans analyser une chaîne.
- Les résultats sont identiques à ceux de l'Activité 2-1. Seule la façon de transporter les données change.
- Quand le client tape `fin`, il ferme sa socket. `readObject()` lève alors une `EOFException` côté serveur, qui s'arrête proprement.

## Comparaison des deux démarches

| | Activité 2-1 : chaînes de caractères | Activité 2-2 : objets |
|---|---|---|
| Flux utilisés | `BufferedReader` / `PrintWriter` | `ObjectInputStream` / `ObjectOutputStream` |
| Données transmises | Texte, une ligne par élément | Objet complet (`Operation`, `Resultat`) |
| Conversion côté serveur | `Double.parseDouble` sur chaque chaîne | Aucune : champs déjà typés |
| Format du message | À définir soi-même (ordre et séparateurs) | Défini par la classe |
| Contrainte | Les deux côtés doivent respecter le même format texte | La classe doit exister des deux côtés (même package, même `serialVersionUID`) |
| Interopérabilité | Tout langage peut lire/écrire du texte | Java uniquement (format de sérialisation propre à Java) |
