package com.pi.smart_travel_guide.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.pi.smart_travel_guide.exception.ResourceNotFoundException;

@Service
public class RdfService {

    private static final String TR   = "http://example.org/travel#";
    private static final String DATA = "http://example.org/travel/data#";

    // Un identifiant ne doit contenir que lettres, chiffres, "_" (protection contre l'injection SPARQL)
    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_]{1,60}$");

    private static final String PREFIXES = """
        PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        PREFIX tr:   <http://example.org/travel#>
        PREFIX d:    <http://example.org/travel/data#>
        """;

    // ===== Règles d'inférence (étape 3) =====
    private static final String RULE_VISITED = """
        CONSTRUCT { ?t tr:hasVisited ?city }
        WHERE {
          ?t    tr:makesBooking        ?b .
          ?b    tr:bookedAccommodation ?acc .
          ?acc  tr:locatedIn           ?city .
          ?city a tr:City .
        }
        """;

    private static final String RULE_COUNTRY = """
        CONSTRUCT { ?x tr:locatedIn ?country }
        WHERE {
          ?x       tr:locatedIn ?city .
          ?city    tr:locatedIn ?country .
          ?country a tr:Country .
        }
        """;

    private final Model rawModel;         // graphe brut
    private final Model rulesModel;       // brut + règles R1 et R2
    private final InfModel enrichedModel; // brut + R1 + R2 + inférence RDFS

    public RdfService() {
        this.rawModel = loadModel();
        this.rulesModel = applyRules(rawModel);
        this.enrichedModel = ModelFactory.createRDFSModel(rulesModel);
        System.out.println("Brut : " + rawModel.size() + " triplets | avec règles : " + rulesModel.size());
    }

    // ------------------------------------------------------------------
    //  Chargement et inférence
    // ------------------------------------------------------------------

    private Model loadModel() {
        Model base = ModelFactory.createDefaultModel();
        try (InputStream in = new ClassPathResource("travel.rdf").getInputStream()) {
            RDFDataMgr.read(base, in, Lang.RDFXML);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger travel.rdf", e);
        }
        return base;
    }

    /** Copie le graphe brut puis y ajoute les triplets produits par chaque règle. */
    private Model applyRules(Model base) {
        Model copy = ModelFactory.createDefaultModel().add(base);
        copy.add(construct(RULE_VISITED, copy));
        copy.add(construct(RULE_COUNTRY, copy));
        return copy;
    }

    private Model construct(String body, Model m) {
        try (QueryExecution qe = QueryExecutionFactory.create(QueryFactory.create(PREFIXES + body), m)) {
            return qe.execConstruct();
        }
    }

    private Model pick(boolean withInference) {
        return withInference ? enrichedModel : rawModel;
    }

    // ------------------------------------------------------------------
    //  Outils internes
    // ------------------------------------------------------------------

    private ParameterizedSparqlString query(String body) {
        return new ParameterizedSparqlString(PREFIXES + body);
    }

    /** Exécute un SELECT et convertit chaque ligne en Map (donc en objet JSON). */
    private List<Map<String, Object>> execute(ParameterizedSparqlString pss, Model m) {
        try (QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(), m)) {
            ResultSet rs = qe.execSelect();
            List<String> vars = rs.getResultVars();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.hasNext()) {
                QuerySolution sol = rs.nextSolution();
                Map<String, Object> row = new LinkedHashMap<>();
                for (String v : vars) {
                    RDFNode node = sol.get(v);
                    if (node != null) row.put(v, toJava(node));
                }
                rows.add(row);
            }
            return rows;
        }
    }

    /** Nombre -> nombre JSON ; texte -> String ; ressource -> son nom local (ex. "Sousse"). */
    private Object toJava(RDFNode node) {
        if (node.isLiteral()) {
            Object value = node.asLiteral().getValue();
            return (value instanceof Number) ? value : node.asLiteral().getLexicalForm();
        }
        return node.asResource().getLocalName();
    }

    private String checkId(String id) {
        if (id == null || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Identifiant invalide : lettres, chiffres et '_' uniquement");
        }
        return id;
    }

    /** Lève une 404 si la ressource n'existe pas avec le type attendu. */
    private void requireExists(String id, String typeLocalName, String message) {
        ParameterizedSparqlString ask = query("ASK { ?x a ?type }");
        ask.setIri("x", DATA + checkId(id));
        ask.setIri("type", TR + typeLocalName);
        try (QueryExecution qe = QueryExecutionFactory.create(ask.asQuery(), rawModel)) {
            if (!qe.execAsk()) throw new ResourceNotFoundException(message);
        }
    }

    private long count(String body, Model m) {
        Object n = execute(query(body), m).get(0).get("n");
        return ((Number) n).longValue();
    }

    // ------------------------------------------------------------------
    //  Endpoint 1 : tous les hébergements
    // ------------------------------------------------------------------
    public List<Map<String, Object>> getAllAccommodations() {
        return execute(query("""
            SELECT ?label ?type ?prix ?ville
            WHERE {
              ?acc a ?type ;
                   rdfs:label ?label ;
                   tr:pricePerNight ?prix ;
                   tr:locatedIn ?ville .
              ?type rdfs:subClassOf* tr:Accommodation .
            }
            ORDER BY ?label
            """), rawModel);
    }

    // ------------------------------------------------------------------
    //  Endpoint 2 : détail d'un hébergement (404 s'il n'existe pas)
    // ------------------------------------------------------------------
    public Map<String, Object> getAccommodation(String id) {
        ParameterizedSparqlString q = query("""
            SELECT ?label ?type ?etoiles ?prix ?ville ?pays
            WHERE {
              VALUES ?type { tr:Hotel tr:Hostel }
              ?acc a ?type ;
                   rdfs:label ?label ;
                   tr:starRating ?etoiles ;
                   tr:pricePerNight ?prix ;
                   tr:locatedIn ?c .
              ?c a tr:City ; rdfs:label ?ville ; tr:locatedIn ?co .
              ?co rdfs:label ?pays .
            }
            """);
        q.setIri("acc", DATA + checkId(id));   // injection sécurisée de l'identifiant

        List<Map<String, Object>> rows = execute(q, rawModel);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Hébergement introuvable : " + id);
        }
        return rows.get(0);
    }

    // ------------------------------------------------------------------
    //  Endpoint 3 : villes (Q1), filtre optionnel par pays
    // ------------------------------------------------------------------
    public List<Map<String, Object>> getCities(String country) {
        ParameterizedSparqlString q = query("""
            SELECT ?ville ?pays
            WHERE {
              ?v a tr:City ;
                 rdfs:label ?ville ;
                 tr:locatedIn ?cty .
              ?cty rdfs:label ?pays .
            }
            ORDER BY ?pays ?ville
            """);
        if (country != null) {
            requireExists(country, "Country", "Pays introuvable : " + country);
            q.setIri("cty", DATA + country);
        }
        return execute(q, rawModel);
    }

    // ------------------------------------------------------------------
    //  Endpoint 4 : recherche d'hôtels (Q2, filtre paramétré)
    // ------------------------------------------------------------------
    public List<Map<String, Object>> searchHotels(int minStars, double maxPrice) {
        ParameterizedSparqlString q = query("""
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
            """);
        q.setLiteral("minStars", minStars);
        q.setLiteral("maxPrice", maxPrice);
        return execute(q, rawModel);
    }

    // ------------------------------------------------------------------
    //  Endpoint 5 : réservations (Q3, jointure), filtre optionnel voyageur
    // ------------------------------------------------------------------
    public List<Map<String, Object>> getBookings(String traveler) {
        ParameterizedSparqlString q = query("""
            SELECT ?voyageur ?hebergement ?ville ?nuits ?total
            WHERE {
              ?trav foaf:name ?voyageur ;
                    tr:makesBooking ?b .
              ?b tr:bookedAccommodation ?acc ;
                 tr:nights ?nuits ;
                 tr:totalPrice ?total .
              ?acc rdfs:label ?hebergement ;
                   tr:locatedIn ?c .
              ?c a tr:City ; rdfs:label ?ville .
            }
            ORDER BY ?voyageur
            """);
        if (traveler != null) {
            requireExists(traveler, "Traveler", "Voyageur introuvable : " + traveler);
            q.setIri("trav", DATA + traveler);
        }
        return execute(q, rawModel);
    }

    // ------------------------------------------------------------------
    //  Endpoint 6 : dépenses par voyageur (Q4, agrégation)
    // ------------------------------------------------------------------
    public List<Map<String, Object>> statsTravelers() {
        return execute(query("""
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
            """), rawModel);
    }

    // ------------------------------------------------------------------
    //  Endpoint 7 : bilan par ville (Q5, OPTIONAL + agrégation)
    // ------------------------------------------------------------------
    public List<Map<String, Object>> statsCities() {
        return execute(query("""
            SELECT ?ville
                   (COUNT(DISTINCT ?acc) AS ?nbHebergements)
                   (COUNT(?b) AS ?nbReservations)
                   (SUM(COALESCE(?total, 0)) AS ?chiffreAffaires)
            WHERE {
              ?c a tr:City ; rdfs:label ?ville .
              ?acc tr:locatedIn ?c ;
                   tr:pricePerNight ?p .
              OPTIONAL {
                ?b tr:bookedAccommodation ?acc ;
                   tr:totalPrice ?total .
              }
            }
            GROUP BY ?ville
            ORDER BY DESC(?chiffreAffaires)
            """), rawModel);
    }

    // ------------------------------------------------------------------
    //  Endpoint 8 : destinations visitées, avec ou sans inférence
    // ------------------------------------------------------------------
    public Map<String, Object> getVisited(String travelerId, boolean withInference) {
        requireExists(travelerId, "Traveler", "Voyageur introuvable : " + travelerId);

        ParameterizedSparqlString q = query("""
            SELECT DISTINCT ?destination
            WHERE {
              ?trav tr:hasVisited ?d .
              ?d rdfs:label ?destination .
            }
            ORDER BY ?destination
            """);
        q.setIri("trav", DATA + travelerId);

        List<Object> destinations = new ArrayList<>();
        for (Map<String, Object> row : execute(q, pick(withInference))) {
            destinations.add(row.get("destination"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("voyageur", travelerId);
        result.put("avecInference", withInference);
        result.put("nombre", destinations.size());
        result.put("destinations", destinations);
        return result;
    }

    // ------------------------------------------------------------------
    //  Endpoint 9 : comparaison globale sans / avec inférence
    // ------------------------------------------------------------------
    public Map<String, Object> compare() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("triplets_bruts", rawModel.size());
        result.put("triplets_apres_regles_R1_R2", rulesModel.size());
        result.put("triplets_apres_RDFS", enrichedModel.size());

        List<Map<String, Object>> tests = new ArrayList<>();
        tests.add(compareOne("Nombre d'hébergements (classe Accommodation)",
                "RDFS : subClassOf",
                "SELECT (COUNT(DISTINCT ?x) AS ?n) WHERE { ?x a tr:Accommodation }"));
        tests.add(compareOne("Nombre de relations hasVisited",
                "R1 + RDFS : subPropertyOf",
                "SELECT (COUNT(*) AS ?n) WHERE { ?t tr:hasVisited ?d }"));
        tests.add(compareOne("Nombre d'hôtels situés en Tunisie",
                "R2 : transitivité ville -> pays",
                "SELECT (COUNT(DISTINCT ?h) AS ?n) WHERE { ?h a tr:Hotel ; tr:locatedIn d:Tunisia }"));
        result.put("comparaisons", tests);
        return result;
    }

    private Map<String, Object> compareOne(String question, String regle, String sparql) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("question", question);
        m.put("regleResponsable", regle);
        m.put("sansInference", count(sparql, rawModel));
        m.put("avecInference", count(sparql, enrichedModel));
        return m;
    }
}