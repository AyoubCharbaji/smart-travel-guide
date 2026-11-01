# Smart Travel Guide

Projet universitaire de **Web sémantique** — Thème : Tourism & Travel.

## Membres de l'équipe

- **Ayoub**
- **Mohamed Fares**

## Objectif académique

Exposer une base de connaissances touristique publiée sous forme de RDF/RDFS à travers un **Web Service REST (Spring Boot)**.
Le projet démontre le passage complet :

**Dataset RDF → Modélisation RDFS → Requêtes SPARQL → Inférence (RDFS + règles personnalisées) → Web Service REST → Résultats JSON.**

## Technologies utilisées

| Technologie | Version | Rôle |
|-------------|---------|------|
| Java | 21 | Langage de programmation |
| Spring Boot | 3.4.5 | Framework Web et injection de dépendances |
| Apache Jena | 4.10.0 | Manipulation RDF/RDFS, raisonneur, SPARQL |
| Maven | — | Gestion de build et dépendances |
| JUnit 5 | — | Tests unitaires et d'intégration |

## Architecture du projet

```
src/
├── main/
│   ├── java/com/pi/smart_travel_guide/
│   │   ├── SmartTravelGuideApplication.java   # Classe principale Spring Boot
│   │   ├── Controller/
│   │   │   └── RdfController.java              # 9 endpoints REST sous /api
│   │   ├── Service/
│   │   │   └── RdfService.java                 # Chargement Jena, règles, requêtes SPARQL, 3 modèles
│   │   └── exception/
│   │       ├── ApiExceptionHandler.java        # Gestion centralisée des erreurs (404, 400, 500)
│   │       └── ResourceNotFoundException.java  # Exception métier 404
│   └── resources/
│       ├── application.properties              # Configuration Spring Boot
│       └── travel.rdf                          # Dataset RDF/XML (ontologie + instances)
└── test/
    └── java/com/pi/smart_travel_guide/
        ├── Controller/
        │   └── RdfControllerTest.java          # Tests d'intégration REST (endpoints, codes HTTP, JSON)
        └── Service/
            └── RdfServiceTest.java             # Tests métier (requêtes SPARQL, inférence)
```

## Présentation du dataset (`travel.rdf`)

Le fichier `src/main/resources/travel.rdf` contient :

- **3 pays :** Tunisie, France, Italie
- **6 villes :** Tunis, Sousse, Djerba, Paris, Nice, Rome
- **6 activités touristiques** (ex. Visite de la médina, Plongée sous-marine, Visite du Louvre…)
- **6 hôtels** et **2 auberges de jeunesse**
- **5 voyageurs** (Ali, Sara, Karim, Lina, Marc)
- **8 réservations** (B001 à B008)

## Modèle RDF / RDFS

### Classes (9)

| Classe | Superclasse (`rdfs:subClassOf`) |
|--------|---------------------------------|
| `tr:Destination` | `schema.org/Place` |
| `tr:Country` | `tr:Destination` |
| `tr:City` | `tr:Destination` |
| `tr:Accommodation` | `schema.org/LodgingBusiness` |
| `tr:Hotel` | `tr:Accommodation` |
| `tr:Hostel` | `tr:Accommodation` |
| `tr:Traveler` | `foaf:Person` |
| `tr:Booking` | — |
| `tr:Activity` | — |

### Propriétés (11)

| Propriété | Domaine | Range | Sous-propriété de |
|-----------|---------|-------|-------------------|
| `tr:locatedIn` | `schema.org/Place` | `tr:Destination` | — |
| `tr:hasVisited` | `tr:Traveler` | `tr:Destination` | — |
| `tr:hasFavoriteDestination` | `tr:Traveler` | `tr:Destination` | `tr:hasVisited` |
| `tr:makesBooking` | `tr:Traveler` | `tr:Booking` | — |
| `tr:bookedAccommodation` | `tr:Booking` | `tr:Accommodation` | — |
| `tr:offersActivity` | `tr:Destination` | `tr:Activity` | — |
| `tr:pricePerNight` | `tr:Accommodation` | `xsd:decimal` | — |
| `tr:starRating` | `tr:Accommodation` | `xsd:integer` | — |
| `tr:nights` | `tr:Booking` | `xsd:integer` | — |
| `tr:totalPrice` | `tr:Booking` | `xsd:decimal` | — |
| `tr:activityPrice` | `tr:Activity` | `xsd:decimal` | — |

### Vocabulaires externes réutilisés

- **FOAF** : `foaf:Person`, `foaf:name`, `foaf:knows`
- **Schema.org** : `schema.org/Place`, `schema.org/LodgingBusiness`

### Schéma graphique

Un schéma visuel du modèle est disponible dans `docs/schema.drawio` (format draw.io).

## Requêtes SPARQL

Cinq requêtes principales sont sélectionnées pour la soutenance. Leur documentation détaillée se trouve dans `docs/requetes-sparql.md` :

1. **`searchHotels`** — Recherche d'hôtels par étoiles et prix (`FILTER`)
2. **`getCities`** — Liste des villes avec filtre optionnel par pays
3. **`statsTravelers`** — Statistiques de dépenses par voyageur (`GROUP BY`, `COUNT`, `SUM`)
4. **`getVisited`** — Destinations visitées par un voyageur (avec/sans inférence)
5. **`getAllAccommodations`** — Tous les hébergements (`rdfs:subClassOf*`)

D'autres requêtes sont également disponibles : `getAccommodation`, `getBookings`, `statsCities`, `compare`.

## Mécanismes d'inférence

Le service utilise **trois niveaux de modèle Jena** :

| Modèle | Contenu |
|--------|---------|
| `rawModel` | Graphe RDF brut chargé depuis `travel.rdf` |
| `rulesModel` | Graphe brut enrichi par deux règles `CONSTRUCT` personnalisées (R1, R2) |
| `enrichedModel` | Graphe enrichi par les règles personnalisées **et** le raisonneur RDFS natif de Jena |

### Règle R1 (`RULE_VISITED`)
Si un voyageur effectue une réservation pour un hébergement situé dans une ville, alors ce voyageur a visité cette ville (`tr:hasVisited`).

### Règle R2 (`RULE_COUNTRY`)
Si une ressource X est située dans une ville C, et C est située dans un pays P, alors X est située dans le pays P (transitivité).

### Raisonnement RDFS natif de Jena
Le raisonneur `createRDFSModel` déduit automatiquement :
- L'héritage de classes via `rdfs:subClassOf`
- L'héritage de propriétés via `rdfs:subPropertyOf` (ex. `hasFavoriteDestination ⊑ hasVisited`)
- Les inférences liées à `rdfs:domain` et `rdfs:range`

L'endpoint `GET /api/inference/compare` permet de comparer quantitativement les résultats entre le graphe brut, le graphe après règles et le graphe après RDFS.

## Endpoints REST

Tous les endpoints retournent du **JSON** et sont préfixés par `/api`.

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/accommodations` | GET | Liste tous les hébergements |
| `/api/accommodations/{id}` | GET | Détail d'un hébergement (404 si absent) |
| `/api/cities?country=` | GET | Villes, filtre optionnel par pays |
| `/api/hotels/search?minStars=&maxPrice=` | GET | Recherche d'hôtels par étoiles et prix |
| `/api/bookings?traveler=` | GET | Réservations, filtre optionnel par voyageur |
| `/api/stats/travelers` | GET | Dépenses par voyageur (agrégation) |
| `/api/stats/cities` | GET | Bilan par ville (chiffre d'affaires) |
| `/api/travelers/{id}/visited?withInference=` | GET | Destinations visitées (brut ou inféré) |
| `/api/inference/compare` | GET | Comparaison des modèles (brut / règles / RDFS) |

### Gestion des erreurs

Toutes les erreurs retournent un format JSON unifié :

```json
{
  "timestamp": "2026-11-01T...",
  "status": 404,
  "error": "Not Found",
  "message": "Hébergement introuvable : Unknown_Hotel"
}
```

- **404** : ressource introuvable (`ResourceNotFoundException`)
- **400** : paramètre invalide (`IllegalArgumentException`)
- **500** : erreur d'exécution SPARQL (`QueryException`, détails masqués)

## Tests

Deux suites de tests automatisées sont disponibles :

- `src/test/java/com/pi/smart_travel_guide/Service/RdfServiceTest.java` — Tests métier (chargement RDF, requêtes SPARQL, inférence, exception 404).
- `src/test/java/com/pi/smart_travel_guide/Controller/RdfControllerTest.java` — Tests d'intégration REST (endpoints, codes HTTP 200/404, format JSON).

Elles couvrent :
- Chargement des hébergements (8 résultats attendus)
- Recherche d'hôtels par étoiles et prix
- Liste des villes (6) et filtre par pays (Tunisia → 3 villes)
- Statistiques de dépenses par voyageur (Ali en tête)
- Destinations visitées avec et sans inférence
- Exception 404 pour un hébergement inconnu
- Codes HTTP et réponses JSON des 9 endpoints REST

La stratégie de tests détaillée est dans `docs/tests.md`.

### Lancer les tests

```bash
./mvnw test
```

## Instructions d'installation et d'exécution

### Prérequis

- Java 21 (JDK)
- Maven (ou utilisation du wrapper `mvnw`)

### Lancer l'application

```bash
# Depuis la racine du projet
./mvnw spring-boot:run
```

L'application démarre sur le port **8080** (par défaut).

### Tester les endpoints

Vous pouvez utiliser **Postman**, votre navigateur ou `curl` :

```bash
# Tous les hébergements
curl http://localhost:8080/api/accommodations

# Recherche d'hôtels (4 étoiles minimum, max 200 €)
curl "http://localhost:8080/api/hotels/search?minStars=4&maxPrice=200"

# Villes de Tunisie
curl "http://localhost:8080/api/cities?country=Tunisia"

# Destinations visitées par Ali (avec inférence)
curl "http://localhost:8080/api/travelers/Ali/visited?withInference=true"
```

## Déclaration d'utilisation de l'IA générative

Ce projet a été accompagné par une intelligence artificielle générative pour :
- L'analyse de l'existant et l'audit initial
- La proposition de structure de tests et de documentation
- La rédaction des fiches de requêtes SPARQL
- La conception du schéma graphique `docs/schema.drawio`

Tout le code source existant (`travel.rdf`, `RdfService.java`, `RdfController.java`, etc.) a été produit et validé par les membres de l'équipe (Ayoub & Fares). L'IA n'a pas modifié directement le code source de l'application.