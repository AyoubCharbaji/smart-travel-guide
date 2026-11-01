# Requêtes SPARQL — Smart Travel Guide

Ce document présente les 5 requêtes SPARQL sélectionnées pour la soutenance académique,
ainsi que leur question métier, leur explication technique et des exemples de résultats attendus.

Les requêtes sont implémentées dans `src/main/java/com/pi/smart_travel_guide/Service/RdfService.java`
et exposées via les endpoints REST de `RdfController.java`.

---

## 1. Recherche d'hôtels par étoiles et prix

**Nom :** `searchHotels`  
**Endpoint :** `GET /api/hotels/search?minStars=4&maxPrice=200`  
**Type :** SELECT avec `FILTER`  

### Question métier
Quels hôtels proposent au moins 4 étoiles et coûtent moins de 200 € la nuit ?

### Code SPARQL
```sparql
PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX tr:   <http://example.org/travel#>
PREFIX d:    <http://example.org/travel/data#>

SELECT ?hotel ?ville ?etoiles ?prix
WHERE {
  ?h a tr:Hotel ;
     rdfs:label ?hotel ;
     tr:starRating ?etoiles ;
     tr:pricePerNight ?prix ;
     tr:locatedIn ?c .
  ?c a tr:City ; rdfs:label ?ville .
  FILTER (?etoiles >= ?minStars && ?prix <= ?maxPrice)
}
ORDER BY ?prix
```

### Explication
- `?h a tr:Hotel` : sélectionne uniquement les ressources de type Hôtel
- `tr:starRating ?etoiles` et `tr:pricePerNight ?prix` : récupère les notes et prix
- `tr:locatedIn ?c` + `?c a tr:City ; rdfs:label ?ville` : jointure pour obtenir le nom de la ville
- `FILTER (?etoiles >= ?minStars && ?prix <= ?maxPrice)` : filtre paramétré sur les critères entrants
- `ORDER BY ?prix` : trie du moins cher au plus cher

### Paramètres
| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `minStars` | entier | 0 | Nombre minimal d'étoiles (0 à 5) |
| `maxPrice` | décimal | 100000 | Prix maximum par nuit |

### Exemple de résultat attendu
```json
[
  {
    "hotel": "Hôtel Médina Tunis",
    "ville": "Tunis",
    "etoiles": 4,
    "prix": 120.0
  },
  {
    "hotel": "Djerba Oasis",
    "ville": "Djerba",
    "etoiles": 4,
    "prix": 150.0
  }
]
```

---

## 2. Villes avec filtre optionnel par pays

**Nom :** `getCities`  
**Endpoint :** `GET /api/cities?country=Tunisia`  
**Type :** SELECT avec paramètre optionnel  

### Question métier
Quelles villes sont listées dans la base, et dans quel pays se trouvent-elles ?

### Code SPARQL
```sparql
PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX tr:   <http://example.org/travel#>
PREFIX d:    <http://example.org/travel/data#>

SELECT ?ville ?pays
WHERE {
  ?v a tr:City ;
     rdfs:label ?ville ;
     tr:locatedIn ?cty .
  ?cty rdfs:label ?pays .
}
ORDER BY ?pays ?ville
```

### Explication
- `?v a tr:City` : sélectionne les villes
- `tr:locatedIn ?cty` : lie chaque ville à son pays
- `?cty rdfs:label ?pays` : récupère le libellé du pays
- Si le paramètre `country` est fourni, le service lie `?cty` à l'URI du pays demandé (`d:Tunisia`, `d:France` ou `d:Italy`), filtrant ainsi les résultats.

### Paramètres
| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `country` | chaîne | (aucun) | Identifiant du pays (`Tunisia`, `France`, `Italy`) |

### Exemple de résultat attendu (sans filtre)
```json
[
  { "ville": "Tunis", "pays": "Tunisie" },
  { "ville": "Sousse", "pays": "Tunisie" },
  { "ville": "Djerba", "pays": "Tunisie" },
  { "ville": "Paris", "pays": "France" },
  { "ville": "Nice", "pays": "France" },
  { "ville": "Rome", "pays": "Italie" }
]
```

---

## 3. Statistiques de dépenses par voyageur

**Nom :** `statsTravelers`  
**Endpoint :** `GET /api/stats/travelers`  
**Type :** SELECT avec agrégation (`GROUP BY`, `COUNT`, `SUM`)  

### Question métier
Combien chaque voyageur a-t-il dépensé au total, et pour combien de nuits ?

### Code SPARQL
```sparql
PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX tr:   <http://example.org/travel#>

SELECT ?voyageur
       (COUNT(?b)   AS ?nbReservations)
       (SUM(?nuits) AS ?totalNuits)
       (SUM(?total) AS ?totalDepense)
WHERE {
  ?t a tr:Traveler ;
     foaf:name ?voyageur ;
     tr:makesBooking ?b .
  ?b tr:nights ?nuits ;
     tr:totalPrice ?total .
}
GROUP BY ?voyageur
ORDER BY DESC(?totalDepense)
```

### Explication
- `?t a tr:Traveler` : sélectionne les voyageurs
- `foaf:name ?voyageur` : récupère le nom complet (vocabulaire FOAF réutilisé)
- `tr:makesBooking ?b` : lie chaque voyageur à ses réservations
- `COUNT(?b)` : nombre de réservations par voyageur
- `SUM(?nuits)` : nombre total de nuits réservées
- `SUM(?total)` : montant total dépensé
- `GROUP BY ?voyageur` : groupe les résultats par voyageur
- `ORDER BY DESC(?totalDepense)` : classe du plus gros dépensier au plus petit

### Exemple de résultat attendu
```json
[
  {
    "voyageur": "Ali Ben Salah",
    "nbReservations": 2,
    "totalNuits": 7,
    "totalDepense": 1380.0
  },
  {
    "voyageur": "Karim Jaziri",
    "nbReservations": 2,
    "totalNuits": 9,
    "totalDepense": 1110.0
  }
]
```

---

## 4. Destinations visitées par un voyageur (avec ou sans inférence)

**Nom :** `getVisited`  
**Endpoint :** `GET /api/travelers/Ali/visited?withInference=true`  
**Type :** SELECT avec `DISTINCT`  

### Question métier
Quelles destinations un voyageur a-t-il visitées, directement ou par déduction ?

### Code SPARQL
```sparql
PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX tr:   <http://example.org/travel#>
PREFIX d:    <http://example.org/travel/data#>

SELECT DISTINCT ?destination
WHERE {
  ?trav tr:hasVisited ?d .
  ?d rdfs:label ?destination .
}
ORDER BY ?destination
```

### Explication
- `?trav tr:hasVisited ?d` : sélectionne les destinations visitées par le voyageur
- `?d rdfs:label ?destination` : récupère le libellé de la destination
- `DISTINCT` : élimine les doublons
- Le service choisit le modèle `rawModel` (sans inférence) ou `enrichedModel` (avec inférence RDFS + règles personnalisées) selon le paramètre `withInference`.

### Paramètres
| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `id` (path) | chaîne | — | Identifiant du voyageur (`Ali`, `Sara`, `Karim`, `Lina`, `Marc`) |
| `withInference` | booléen | false | `true` pour utiliser le graphe enrichi par inférence |

### Comportement avec/sans inférence
**Sans inférence (rawModel) :**  
Ali a explicitement `tr:hasVisited d:Paris`. Résultat : `["Paris"]`.

**Avec inférence (enrichedModel) :**  
Grâce à la règle R1 (CONSTRUCT personnalisée : réservation → hébergement → ville → hasVisited) et à `rdfs:subPropertyOf` (`hasFavoriteDestination ⊑ hasVisited`), le raisonneur déduit des destinations supplémentaires (Rome, Tunis, Sousse, etc.). Résultat : davantage de destinations que sans inférence.

### Exemple de résultat attendu (avec inférence)
```json
{
  "voyageur": "Ali",
  "avecInference": true,
  "nombre": 4,
  "destinations": ["Paris", "Rome", "Tunis", "Sousse"]
}
```
*(À confirmer par exécution réelle du service.)*

---

## 5. Tous les hébergements (hôtels et auberges)

**Nom :** `getAllAccommodations`  
**Endpoint :** `GET /api/accommodations`  
**Type :** SELECT avec héritage de classe (`rdfs:subClassOf*`)  

### Question métier
Quels sont tous les hébergements disponibles, quelle que soit leur sous-catégorie (hôtel ou auberge) ?

### Code SPARQL
```sparql
PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX tr:   <http://example.org/travel#>

SELECT ?label ?type ?prix ?ville
WHERE {
  ?acc a ?type ;
       rdfs:label ?label ;
       tr:pricePerNight ?prix ;
       tr:locatedIn ?ville .
  ?type rdfs:subClassOf* tr:Accommodation .
}
ORDER BY ?label
```

### Explication
- `?acc a ?type` : chaque hébergement a un type concret (Hotel ou Hostel)
- `?type rdfs:subClassOf* tr:Accommodation` : l'astérisque `*` signifie "zéro ou plus" — cela récupère `Hotel` et `Hostel` car ils sont sous-classes de `Accommodation`
- `rdfs:label`, `tr:pricePerNight`, `tr:locatedIn` : récupère les informations d'affichage

### Exemple de résultat attendu
```json
[
  {
    "label": "Hôtel Médina Tunis",
    "type": "Hotel",
    "prix": 120.0,
    "ville": "Tunis"
  },
  {
    "label": "Tunis Backpackers",
    "type": "Hostel",
    "prix": 30.0,
    "ville": "Tunis"
  }
]
```

---

## Autres requêtes disponibles (non présentées en priorité)

| Requête | Endpoint | Fonctionnalité principale |
|---------|----------|---------------------------|
| `getAccommodation(id)` | `/api/accommodations/{id}` | Détail complet d'un hébergement (404 si absent) |
| `getBookings(traveler)` | `/api/bookings?traveler=` | Liste des réservations avec détails |
| `statsCities()` | `/api/stats/cities` | `OPTIONAL` + `COALESCE` pour le chiffre d'affaires par ville |
| `compare()` | `/api/inference/compare` | Comparaison quantitative brut / règles / RDFS |

---

## Requêtes spéciales (ASK et CONSTRUCT)

| Type | Nom | Rôle |
|------|-----|------|
| ASK | `requireExists` | Vérifie l'existence d'une ressource avant de la retourner (génère un 404 si absente) |
| CONSTRUCT | `RULE_VISITED` | Règle personnalisée R1 : déduit `hasVisited` à partir des réservations |
| CONSTRUCT | `RULE_COUNTRY` | Règle personnalisée R2 : déduit la localisation au pays par transitivité ville → pays |

> **Distinction importante :** Les requêtes `CONSTRUCT` (R1, R2) sont des transformations personnalisées écrites en SPARQL. L'inférence RDFS native est quant à elle assurée par le raisonneur de Jena (`ModelFactory.createRDFSModel`). Ces deux mécanismes sont distincts et complémentaires.