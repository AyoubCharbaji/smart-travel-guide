package com.pi.smart_travel_guide.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pi.smart_travel_guide.Service.RdfService;

@RestController
@RequestMapping("/api")                 // préfixe commun à tous les endpoints
public class RdfController {

    private final RdfService rdfService;

    // Injection par constructeur : Spring fournit automatiquement le bean RdfService
    public RdfController(RdfService rdfService) {
        this.rdfService = rdfService;
    }

    /** 1. Tous les hébergements */
    @GetMapping("/accommodations")
    public List<Map<String, Object>> accommodations() {
        return rdfService.getAllAccommodations();
    }

    /** 2. Détail d'un hébergement : 404 s'il n'existe pas */
    @GetMapping("/accommodations/{id}")
    public Map<String, Object> accommodation(@PathVariable String id) {
        return rdfService.getAccommodation(id);
    }

    /** 3. Villes, filtre optionnel ?country=Tunisia */
    @GetMapping("/cities")
    public List<Map<String, Object>> cities(@RequestParam(required = false) String country) {
        return rdfService.getCities(country);
    }

    /** 4. Recherche d'hôtels ?minStars=4&maxPrice=200 */
    @GetMapping("/hotels/search")
    public List<Map<String, Object>> searchHotels(
            @RequestParam(defaultValue = "0") int minStars,
            @RequestParam(defaultValue = "100000") double maxPrice) {

        if (minStars < 0 || minStars > 5) {
            throw new IllegalArgumentException("minStars doit être compris entre 0 et 5");
        }
        if (maxPrice < 0) {
            throw new IllegalArgumentException("maxPrice doit être positif");
        }
        return rdfService.searchHotels(minStars, maxPrice);
    }

    /** 5. Réservations, filtre optionnel ?traveler=Ali */
    @GetMapping("/bookings")
    public List<Map<String, Object>> bookings(@RequestParam(required = false) String traveler) {
        return rdfService.getBookings(traveler);
    }

    /** 6. Statistiques par voyageur */
    @GetMapping("/stats/travelers")
    public List<Map<String, Object>> statsTravelers() {
        return rdfService.statsTravelers();
    }

    /** 7. Statistiques par ville */
    @GetMapping("/stats/cities")
    public List<Map<String, Object>> statsCities() {
        return rdfService.statsCities();
    }

    /** 8. Destinations visitées : ?withInference=true pour le graphe enrichi */
    @GetMapping("/travelers/{id}/visited")
    public Map<String, Object> visited(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean withInference) {
        return rdfService.getVisited(id, withInference);
    }

    /** 9. Comparaison globale sans / avec inférence */
    @GetMapping("/inference/compare")
    public Map<String, Object> compare() {
        return rdfService.compare();
    }
}