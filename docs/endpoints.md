# Endpoints REST — Smart Travel Guide

Ce document présente les 9 endpoints de l'API REST, leur fonctionnement, des exemples de requêtes (curl / Postman) et des exemples de réponses JSON attendues.

L'application doit être démarrée (`./mvnw.cmd spring-boot:run`) pour tester ces endpoints. Ils sont accessibles sur `http://localhost:8080/api`.

---

## 1. Tous les hébergements

**Méthode :** `GET`  
**URL :** `http://localhost:8080/api/accommodations`  
**Paramètres :** aucun

### Exemple de requête
```bash
curl http://localhost:8080/api/accommodations
```

### Exemple de réponse JSON
```json
[
  {
    "label": "Hôtel Médina Tunis",
    "type": "Hotel",
    "prix": 120.0,
    "ville": "Tunis"
  },
  {
    "label": "Sousse Beach Resort",
    "type": "Hotel",
    "prix": 180.0,
    "ville": "Sousse"
  },
  {
    "label": "Djerba Oasis",
    "type": "Hotel",
    "prix": 150.0,
    "ville": "Djerba"
  },
  {
    "label": "Hôtel Paris Lumière",
    "type": "Hotel",
    "prix": 220.0,
    "ville": "Paris"
  },
  {
    "label": "Riviera Palace Nice",
    "type": "Hotel",
    "prix": 310.0,
    "ville": "Nice"
  },
  {
    "label": "Hotel Colosseo",
    "type": "Hotel",
    "prix": 130.0,
    "ville": "Rome"
  },
  {
    "label": "Tunis Backpackers",
    "type": "Hostel",
    "prix": 30.0,
    "ville": "Tunis"
  },
  {
    "label": "Roma Centro Hostel",
    "type": "Hostel",
    "prix": 40.0,
    "ville": "Rome"
  }
]
```

### Codes HTTP
- `200 OK` : succès, liste des 8 hébergements (6 hôtels + 2 auberges)

---

## 2. Détail d'un hébergement

**Méthode :** `GET`  
**URL :** `http://localhost:8080/api/accommodations/{id}`  
**Paramètres :**
- `id` (path) : identifiant de l'hébergement (`Hotel_Medina_Tunis`, `Hostel_Tunis_Backpackers`, etc.)

### Exemple de requête
```bash
curl http://localhost:8080/api/accommodations/Hotel_Medina_Tunis
```

### Exemple de réponse JSON
```json
{
  "label": "Hôtel Médina Tunis",
  "type": "Hotel",
  "etoiles": 4,
  "prix": 120.0,
  "ville": "Tunis",
  "pays": "Tunisie"
}
```

### Codes HTTP
- `200 OK` : hébergement trouvé
- `404 Not Found` : hébergement introuvable (identifiant invalide ou inexistant)

### Comportement en cas d'erreur
Si l'identifiant contient des caractères non autorisés ou n'existe pas, une exception `ResourceNotFoundException` est levée et retournée en 404 avec un message JSON :
```json
{
  "timestamp": "2026-11-01T...",
  "status": 404,
  "error": "Not Found",
  "message": "Hébergement introuvable : Unknown_Hotel"
}
```

---

## 3. Villes avec filtre optionnel par pays

**Méthode :** `GET`  
**URL :** `http://localhost:8080/api/cities?country=Tunisia`  
**Paramètres :**
- `country` (optionnel) : identifiant du pays (`Tunisia`, `France`, `Italy`)

### Exemple de requête (sans filtre)
```bash
curl http://localhost:8080/api/cities
```

### Exemple de réponse JSON (sans filtre)
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

### Exemple de requête (avec filtre)
```bash
curl "http://localhost:8080/api/cities?country=Tunisia"
```

### Exemple de réponse JSON (Tunisia)
```json
[
  { "ville": "Tunis", "pays": "Tunisie" },
  { "ville": "Sousse", "pays": "Tunisie" },
  { "ville": "Djerba", "pays": "Tunisie" }
]
```

### Codes HTTP
- `200 OK` : succès
- `404 Not Found` : si le paramètre `country` ne correspond à aucun pays existant

---

## 4. Recherche d'hôtels par étoiles et prix

**Méthode :** `GET`  
**URL :** `http://localhost:8080/api/hotels/search?minStars=4&maxPrice=200`  
**Paramètres :**
- `minStars` (optionnel, défaut `0`) : nombre minimal d'étoiles (0 à 5)
- `maxPrice` (optionnel, défaut `100000`) : prix maximum par nuit

### Exemple de requête
```bash
curl "http://localhost:8080/api/hotels/search?minStars=4&maxPrice=200"
```

### Exemple de réponse JSON
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

### Codes HTTP
- `200 OK` : succès
- `400 Bad Request` : si `minStars` n'est pas entre 0 et 5, ou si `maxPrice` est négatif

### Exemple d'erreur 400
```json
{
  "timestamp": "2026-11-01T...",
  "status": 400,
  "error": "Bad Request",
  "message": "minStars doit être compris entre 0 et 5"
}
```

---

## 5. Réservations par voyageur

**Méthode :** `GET`  
**URL :** `http://localhost:8080/api/bookings?traveler=Ali`  
**Paramètres :**
- `traveler` (optionnel) : identifiant du voyageur (`Ali`, `Sara`, `Karim`, `Lina`, `Marc`)

### Exemple de requête (sans filtre)
```bash
curl http://localhost:8080/api/bookings
```

### Exemple de requête (avec filtre)
```bash
curl "http://localhost:8080/api/bookings?traveler=Ali"
```

### Exemple de réponse JSON (traveler=Ali)
```json
[
  {
    "voyageur": "Ali Ben Salah",
    "hebergement": "Sousse Beach Resort",
    "ville": "Sousse",
    "nuits": 4,
    "total": 720.0
  },
  {
    "voyageur": "Ali Ben Salah",
    "hebergement": "Hôtel Paris Lumière",
    "ville": "Paris",
    "nuits": 3,
    "total": 660.0
  }
]
```

### Codes HTTP
- `200 OK` : succès
- `404 Not Found` : si le voyageur spécifié n'existe pas

---

## 6. Statistiques de dépenses par voyageur

**Méthode :** `GET`  
**URL :** `http://localhost:8080/api/stats/travelers`  
**Paramètres :** aucun

### Exemple de requête
```bash
curl http://localhost:8080/api/stats/travelers
```

### Exemple de réponse JSON
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
  },
  {
    "voyageur": "Sara Trabelsi",
    "nbReservations": 2,
    "totalNuits": 7,
    "totalDepense": 560.0
  },
  {
    "voyageur": "Lina Gharbi",
    "nbReservations": 1,
    "totalNuits": 2,
    "totalDepense": 620.0
  },
  {
    "voyageur": "Marc Dupont",
    "nbReservations": 1,
    "totalNuits": 3,
    "totalDepense": 360.0
  }
]
```

### Codes HTTP
- `200 OK` : succès, trié par dépense totale décroissante

---

## 7. Bilan par ville

**Méthode :** `GET`  
**URL :** `http://localhost:8080/api/stats/cities`  
**Paramètres :** aucun

### Exemple de requête
```bash
curl http://localhost:8080/api/stats/cities
```

### Exemple de réponse JSON
```json
[
  {
    "ville": "Sousse",
    "nbHebergements": 1,
    "nbReservations": 2,
    "chiffreAffaires": 1080.0
  },
  {
    "ville": "Paris",
    "nbHebergements": 1,
    "nbReservations": 1,
    "chiffreAffaires": 660.0
  },
  {
    "ville": "Nice",
    "nbHebergements": 1,
    "nbReservations": 1,
    "chiffreAffaires": 620.0
  },
  {
    "ville": "Djerba",
    "nbHebergements": 1,
    "nbReservations": 1,
    "chiffreAffaires": 1050.0
  },
  {
    "ville": "Tunis",
    "nbHebergements": 2,
    "nbReservations": 2,
    "chiffreAffaires": 420.0
  },
  {
    "ville": "Rome",
    "nbHebergements": 2,
    "nbReservations": 1,
    "chiffreAffaires": 200.0
  }
]
```

### Codes HTTP
- `200 OK` : succès, trié par chiffre d'affaires décroissant

---

## 8. Destinations visitées par un voyageur (avec ou sans inférence)

**Méthode :** `GET`  
**URL :** `http://localhost:8080/api/travelers/{id}/visited?withInference=true`  
**Paramètres :**
- `id` (path) : identifiant du voyageur (`Ali`, `Sara`, `Karim`, `Lina`, `Marc`)
- `withInference` (optionnel, défaut `false`) : `true` pour utiliser le graphe enrichi par inférence RDFS + règles personnalisées

### Exemple de requête (sans inférence)
```bash
curl "http://localhost:8080/api/travelers/Ali/visited?withInference=false"
```

### Exemple de réponse JSON (sans inférence)
```json
{
  "voyageur": "Ali",
  "avecInference": false,
  "nombre": 1,
  "destinations": ["Paris"]
}
```

### Exemple de requête (avec inférence)
```bash
curl "http://localhost:8080/api/travelers/Ali/visited?withInference=true"
```

### Exemple de réponse JSON (avec inférence)
```json
{
  "voyageur": "Ali",
  "avecInference": true,
  "nombre": 4,
  "destinations": ["Paris", "Rome", "Tunis", "Sousse"]
}
```
*(Le nombre et la liste exacts dépendent des triplets déduits par le reasonneur RDFS et les règles R1/R2.)*

### Codes HTTP
- `200 OK` : succès
- `404 Not Found` : si le voyageur n'existe pas

---

## 9. Comparaison des modèles (brut / règles / RDFS)

**Méthode :** `GET`  
**URL :** `http://localhost:8080/api/inference/compare`  
**Paramètres :** aucun

### Exemple de requête
```bash
curl http://localhost:8080/api/inference/compare
```

### Exemple de réponse JSON
```json
{
  "triplets_bruts": 215,
  "triplets_apres_regles_R1_R2": 230,
  "triplets_apres_RDFS": 232,
  "comparaisons": [
    {
      "question": "Nombre d'hébergements (classe Accommodation)",
      "regleResponsable": "RDFS : subClassOf",
      "sansInference": 0,
      "avecInference": 8
    },
    {
      "question": "Nombre de relations hasVisited",
      "regleResponsable": "R1 + RDFS : subPropertyOf",
      "sansInference": 2,
      "avecInference": 6
    },
    {
      "question": "Nombre d'hôtels situés en Tunisie",
      "regleResponsable": "R2 : transitivité ville -> pays",
      "sansInference": 0,
      "avecInference": 6
    }
  ]
}
```
*(Les nombres exacts dépendent du contenu réel de `travel.rdf` et des déductions du reasonneur.)*

### Codes HTTP
- `200 OK` : succès

---

## Tableau récapitulatif des endpoints

| Endpoint | Méthode | Paramètres | Modèle Jena | Usage académique |
|----------|---------|------------|-------------|------------------|
| `/api/accommodations` | GET | — | `rawModel` | Liste des hébergements (`subClassOf*`) |
| `/api/accommodations/{id}` | GET | `id` | `rawModel` | Détail hébergement (404 si absent) |
| `/api/cities` | GET | `?country=` | `rawModel` | Villes avec filtre optionnel par pays |
| `/api/hotels/search` | GET | `?minStars=`, `?maxPrice=` | `rawModel` | Recherche avec `FILTER` |
| `/api/bookings` | GET | `?traveler=` | `rawModel` | Réservations jointes |
| `/api/stats/travelers` | GET | — | `rawModel` | Agrégation (`GROUP BY`, `SUM`) |
| `/api/stats/cities` | GET | — | `rawModel` | Bilan par ville (`OPTIONAL`, `COALESCE`) |
| `/api/travelers/{id}/visited` | GET | `id`, `?withInference=` | `rawModel` / `enrichedModel` | Démonstration de l'inférence |
| `/api/inference/compare` | GET | — | 3 modèles | Comparaison brut / règles / RDFS |

---

## Gestion des erreurs (format JSON unifié)

Toutes les erreurs retournent un format cohérent :

```json
{
  "timestamp": "2026-11-01T22:30:00.000+01:00",
  "status": 404,
  "error": "Not Found",
  "message": "Hébergement introuvable : Unknown_Hotel"
}
```

| Code | Signification | Cause |
|------|--------------|-------|
| `200` | OK | Requête réussie |
| `400` | Bad Request | Paramètre invalide (ex. `minStars` hors de 0-5) |
| `404` | Not Found | Ressource inexistante (hébergement, ville, pays, voyageur) |
| `500` | Internal Server Error | Erreur d'exécution SPARQL (détails masqués au client) |

---

## Test avec Postman

Pour la démonstration orale, vous pouvez importer ces requêtes dans Postman :

1. **GET** `http://localhost:8080/api/accommodations`
2. **GET** `http://localhost:8080/api/accommodations/Hotel_Medina_Tunis`
3. **GET** `http://localhost:8080/api/cities?country=Tunisia`
4. **GET** `http://localhost:8080/api/hotels/search?minStars=4&maxPrice=200`
5. **GET** `http://localhost:8080/api/travelers/Ali/visited?withInference=true`
6. **GET** `http://localhost:8080/api/inference/compare`

Vérifiez que le corps de la réponse est bien du JSON valide et que les codes HTTP correspondent aux attentes.