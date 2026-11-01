# Tests et validation technique — Smart Travel Guide

Ce document récapitule la stratégie de tests mise en place pour valider le projet
sur les quatre parties académiques : RDF, SPARQL, inférence et Web Service REST.

## 1. Fichiers de tests

| Chemin | Couche testée | Nombre de tests |
|--------|--------------|-----------------|
| `src/test/java/com/pi/smart_travel_guide/Service/RdfServiceTest.java` | Service (RDF, SPARQL, inférence) | 9 |
| `src/test/java/com/pi/smart_travel_guide/Controller/RdfControllerTest.java` | Contrôleur REST (endpoints, codes HTTP, JSON) | 8 |

Total : **17 tests automatisés**.

---

## 2. Détails des tests — `RdfServiceTest`

| Test | Ce qui est vérifié | Résultat attendu |
|------|-------------------|------------------|
| `shouldLoadAllAccommodations` | Chargement des 8 hébergements (6 hôtels + 2 auberges) | 8 résultats |
| `shouldFindHotelsByStarsAndPrice` | Recherche d'hôtels avec filtre étoiles/prix | Tous ≥ 4 étoiles et ≤ 200 € |
| `shouldReturnAllCities` | Liste complète des villes | 6 villes |
| `shouldFilterCitiesByCountry` | Filtrage des villes par pays (Tunisia) | 3 villes, toutes en Tunisie |
| `shouldReturnTravelerStatsOrderedBySpending` | Agrégation des dépenses par voyageur | Ali Ben Salah en tête |
| `shouldReturnVisitedDestinationsWithoutInference` | Destinations visitées sans inférence | Contient Paris |
| `shouldReturnMoreDestinationsWithInference` | L'inférence ajoute des destinations | Avec inf ≥ sans inf |
| `shouldThrowExceptionForUnknownAccommodation` | Gestion d'erreur 404 | `ResourceNotFoundException` |
| `shouldInferHasVisitedFromHasFavoriteDestination` | Inférence RDFS `subPropertyOf` | Rome déduit depuis favorite |

---

## 3. Détails des tests — `RdfControllerTest`

| Test | Endpoint | Ce qui est vérifié | Résultat attendu |
|------|----------|-------------------|------------------|
| `shouldReturnAllAccommodations` | `GET /api/accommodations` | Code 200, tableau JSON, 8 éléments | 200 OK, 8 hébergements |
| `shouldReturnAccommodationById` | `GET /api/accommodations/Hotel_Medina_Tunis` | Code 200, label et type corrects | 200 OK, "Hôtel Médina Tunis", "Hotel" |
| `shouldReturn404ForUnknownAccommodation` | `GET /api/accommodations/Unknown_Hotel` | Code 404 pour ID inexistant | 404 Not Found |
| `shouldReturnCitiesByCountry` | `GET /api/cities?country=Tunisia` | Code 200, 3 villes en Tunisie | 200 OK, 3 éléments |
| `shouldSearchHotelsByStarsAndPrice` | `GET /api/hotels/search?minStars=4&maxPrice=200` | Code 200, tableau JSON | 200 OK, tableau non vide |
| `shouldReturnTravelerStats` | `GET /api/stats/travelers` | Code 200, 5 voyageurs | 200 OK, 5 éléments |
| `shouldReturnInferenceComparison` | `GET /api/inference/compare` | Code 200, clés `triplets_bruts`, `triplets_apres_regles_R1_R2`, `triplets_apres_RDFS` | 200 OK, 3 clés présentes |
| `shouldReturnVisitedDestinationsWithInference` | `GET /api/travelers/Ali/visited?withInference=true` | Code 200, voyageur Ali, inférence activée | 200 OK, `avecInference: true` |

---

## 4. Comment lancer les tests

Depuis la racine du projet, dans PowerShell :

```powershell
# Tous les tests
./mvnw.cmd test

# Seulement les tests du service
./mvnw.cmd test -Dtest=RdfServiceTest

# Seulement les tests du contrôleur
./mvnw.cmd test -Dtest=RdfControllerTest
```

### Résultat attendu
```
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 5. Ce qui est couvert par les tests

| Partie académique | Tests concernés | Éléments validés |
|------------------|-----------------|------------------|
| **Dataset RDF** | `RdfServiceTest` | Chargement du fichier `travel.rdf`, 215 triplets bruts |
| **Requêtes SPARQL** | `RdfServiceTest`, `RdfControllerTest` | 9 requêtes SELECT, paramètres, filtres, agrégations |
| **Inférence** | `RdfServiceTest`, `RdfControllerTest` | Règles R1/R2, raisonneur RDFS natif, `subClassOf`, `subPropertyOf`, comparaison avant/après |
| **Web Service REST** | `RdfControllerTest` | 9 endpoints, codes HTTP (200, 404), format JSON, paramètres |

---

## 6. Interprétation des résultats

- **BUILD SUCCESS** : tous les tests passent, le projet est fonctionnel.
- **Test en rouge** : indique une régression ou une incohérence dans les données RDF ou le code Java. Vérifier le fichier `travel.rdf` et la classe correspondante.
- **404 inattendu** : vérifier que l'identifiant utilisé dans l'URL existe dans `travel.rdf` (ex. `Hotel_Medina_Tunis`, pas `Hotel_Medina_Tunis1`).

---

## 7. Limites des tests

- Les tests ne couvrent pas tous les paramètres optionnels (ex. `bookings` sans filtre `traveler`).
- Les valeurs exactes de prix et de nombre de triplets dépendent du contenu de `travel.rdf` ; si le dataset est modifié, les tests doivent être mis à jour en conséquence.
- Les tests n'incluent pas de tests de charge ni de tests d'interface utilisateur (hors périmètre académique).