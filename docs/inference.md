# Inférence et enrichissement — Smart Travel Guide

Ce document explique les mécanismes d'inférence sémantique mis en œuvre dans le projet,
la différence entre les trois modèles Jena, et présente des exemples concrets de triplets inférés.

## 1. Les trois modèles Jena

| Modèle | Construction | Contenu | Nombre de triplets (exemple) |
|--------|-------------|---------|------------------------------|
| `rawModel` | `loadModel()` depuis `travel.rdf` | Graphe brut uniquement | 215 |
| `rulesModel` | `applyRules(rawModel)` | Brut + 2 règles `CONSTRUCT` personnalisées (R1, R2) | 230 |
| `enrichedModel` | `ModelFactory.createRDFSModel(rulesModel)` | Brut + règles + raisonneur RDFS natif de Jena | ≥ 230 |

### Distinction importante

- **Règle `CONSTRUCT` personnalisée (R1, R2)** : requête SPARQL écrite à la main qui génère de nouveaux triplets. C'est une inférence *métier*, spécifique à l'application.
- **Raisonnement RDFS natif de Jena** : mécanisme standard du Web sémantique qui déduit automatiquement des triplets à partir de déclarations `rdfs:subClassOf`, `rdfs:subPropertyOf`, `rdfs:domain`, `rdfs:range`.

Une requête `CONSTRUCT` n'est **pas** une règle RDFS native. Ce sont deux mécanismes distincts et complémentaires.

---

## 2. Mécanisme 1 — Héritage des classes (`rdfs:subClassOf`)

### Déclarations dans `travel.rdf`
```xml
<tr:Hotel rdf:about="&tr;Hotel">
  <rdfs:subClassOf rdf:resource="&tr;Accommodation"/>
</tr:Hotel>
<tr:Hostel rdf:about="&tr;Hostel">
  <rdfs:subClassOf rdf:resource="&tr;Accommodation"/>
</tr:Hostel>
<tr:Country rdf:about="&tr;Country">
  <rdfs:subClassOf rdf:resource="&tr;Destination"/>
</tr:Country>
<tr:City rdf:about="&tr;City">
  <rdfs:subClassOf rdf:resource="&tr;Destination"/>
</tr:City>
```

### Triplets initiaux (dans `rawModel`)
- `d:Hotel_Medina_Tunis rdf:type tr:Hotel`
- `tr:Hotel rdfs:subClassOf tr:Accommodation`

### Triplet inféré (dans `enrichedModel` uniquement)
- `d:Hotel_Medina_Tunis rdf:type tr:Accommodation`

### Explication
Grâce à `rdfs:subClassOf`, le raisonneur RDFS de Jena déduit qu'un hôtel est aussi un hébergement. Ce triplet **n'est pas présent explicitement** dans le fichier RDF, il est déduit automatiquement par le reasonneur.

### Requête pour constater
```sparql
SELECT ?x WHERE { ?x a tr:Accommodation }
```
- Sans inférence (`rawModel`) : aucun résultat direct de type `tr:Accommodation` (les instances sont typées `tr:Hotel` ou `tr:Hostel`)
- Avec inférence (`enrichedModel`) : tous les hôtels et auberges apparaissent aussi comme `tr:Accommodation`

---

## 3. Mécanisme 2 — Héritage des propriétés (`rdfs:subPropertyOf`)

### Déclaration dans `travel.rdf`
```xml
<tr:hasFavoriteDestination rdf:about="&tr;hasFavoriteDestination">
  <rdfs:subPropertyOf rdf:resource="&tr;hasVisited"/>
</tr:hasFavoriteDestination>
```

### Triplets initiaux (dans `rawModel`)
- `d:Ali tr:hasFavoriteDestination d:Rome` (présent dans le fichier)
- `d:Ali tr:hasVisited d:Paris` (présent dans le fichier)

### Triplet inféré (dans `enrichedModel` uniquement)
- `d:Ali tr:hasVisited d:Rome`

### Explication
Puisque `hasFavoriteDestination` est une sous-propriété de `hasVisited`, tout voyageur qui a une destination préférée a aussi visité cette destination. Le raisonneur déduit ce triplet automatiquement.

### Requête pour constater
```sparql
SELECT ?d WHERE { d:Ali tr:hasVisited ?d }
```
- Sans inférence : seulement les destinations explicitement visitées par Ali (ex. Paris)
- Avec inférence : Paris + Rome (déduit via `subPropertyOf`)

---

## 4. Règles personnalisées R1 et R2 (`CONSTRUCT`)

Ces règles ne sont pas du RDFS natif, mais des transformations SPARQL personnalisées appliquées avant le reasonneur.

### R1 — `RULE_VISITED`
**Principe :** Si un voyageur réserve un hébergement situé dans une ville, il a visité cette ville.

**Triplet inféré exemple :**
- `d:Ali tr:makesBooking d:B001` + `d:B001 tr:bookedAccommodation d:Hotel_Sousse_Beach` + `d:Hotel_Sousse_Beach tr:locatedIn d:Sousse`
- → `d:Ali tr:hasVisited d:Sousse`

### R2 — `RULE_COUNTRY`
**Principe :** Si X est situé dans une ville C, et C est située dans un pays P, alors X est situé dans le pays P.

**Triplet inféré exemple :**
- `d:Hotel_Medina_Tunis tr:locatedIn d:Tunis` + `d:Tunis tr:locatedIn d:Tunisia`
- → `d:Hotel_Medina_Tunis tr:locatedIn d:Tunisia`

---

## 5. Comparaison des modèles

| Élément | `rawModel` | `rulesModel` | `enrichedModel` |
|---------|-----------|--------------|-----------------|
| Triplets explicites (travel.rdf) | ✅ | ✅ | ✅ |
| Triplets dérivés par R1 (`CONSTRUCT`) | ❌ | ✅ | ✅ |
| Triplets dérivés par R2 (`CONSTRUCT`) | ❌ | ✅ | ✅ |
| Triplets déduits par RDFS natif (`subClassOf`) | ❌ | ❌ | ✅ |
| Triplets déduits par RDFS natif (`subPropertyOf`) | ❌ | ❌ | ✅ |

### Exemple concret : `getVisited("Ali")`

| Modèle | Destinations retournées |
|--------|------------------------|
| `rawModel` (sans inférence) | Paris (visite explicite uniquement) |
| `enrichedModel` (avec inférence RDFS + règles) | Paris + Rome (`subPropertyOf`) + Tunis + Sousse (règle R1) |

---

## 6. Limites

- Les modèles sont construits une seule fois au démarrage de l'application. Une modification de `travel.rdf` nécessite un redémarrage.
- Le reasonneur RDFS de Jena est un reasonneur léger : il couvre les mécanismes RDFS de base (`subClassOf`, `subPropertyOf`, `domain`, `range`), mais pas l'ensemble d'OWL.
- Les règles `CONSTRUCT` R1 et R2 sont statiques : elles ne se réappliquent pas automatiquement si le graphe change après leur application initiale.