package com.pi.smart_travel_guide.Service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RdfServiceTest {

    @Autowired
    private RdfService rdfService;

    @Test
    void shouldLoadAllAccommodations() {
        List<Map<String, Object>> accommodations = rdfService.getAllAccommodations();
        assertThat(accommodations).isNotEmpty();
        // 6 hôtels + 2 auberges de jeunesse = 8 hébergements au total
        assertThat(accommodations).hasSize(8);
    }

    @Test
    void shouldFindHotelsByStarsAndPrice() {
        List<Map<String, Object>> hotels = rdfService.searchHotels(4, 200.0);
        assertThat(hotels).isNotEmpty();
        for (Map<String, Object> hotel : hotels) {
            Number stars = (Number) hotel.get("etoiles");
            Number price = (Number) hotel.get("prix");
            assertThat(stars.intValue()).isGreaterThanOrEqualTo(4);
            assertThat(price.doubleValue()).isLessThanOrEqualTo(200.0);
        }
    }

    @Test
    void shouldReturnAllCities() {
        List<Map<String, Object>> cities = rdfService.getCities(null);
        assertThat(cities).hasSize(6); // Tunis, Sousse, Djerba, Paris, Nice, Rome
    }

    @Test
    void shouldFilterCitiesByCountry() {
        List<Map<String, Object>> cities = rdfService.getCities("Tunisia");
        assertThat(cities).hasSize(3);
        for (Map<String, Object> city : cities) {
            assertThat(city.get("pays")).isEqualTo("Tunisie");
        }
    }

    @Test
    void shouldReturnTravelerStatsOrderedBySpending() {
        List<Map<String, Object>> stats = rdfService.statsTravelers();
        assertThat(stats).isNotEmpty();
        Map<String, Object> first = stats.get(0);
        assertThat(first.get("voyageur")).isEqualTo("Ali Ben Salah");
    }

    @Test
    void shouldReturnVisitedDestinationsWithoutInference() {
        Map<String, Object> result = rdfService.getVisited("Ali", false);
        assertThat(result.get("avecInference")).isEqualTo(false);
        @SuppressWarnings("unchecked")
        List<Object> destinations = (List<Object>) result.get("destinations");
        assertThat(destinations).contains("Paris");
    }

    @Test
    void shouldReturnMoreDestinationsWithInference() {
        Map<String, Object> without = rdfService.getVisited("Ali", false);
        Map<String, Object> with = rdfService.getVisited("Ali", true);

        @SuppressWarnings("unchecked")
        List<Object> withoutDest = (List<Object>) without.get("destinations");
        @SuppressWarnings("unchecked")
        List<Object> withDest = (List<Object>) with.get("destinations");

        assertThat(withDest.size()).isGreaterThanOrEqualTo(withoutDest.size());
    }

    @Test
    void shouldThrowExceptionForUnknownAccommodation() {
        assertThatThrownBy(() -> rdfService.getAccommodation("Unknown_Hotel"))
                .isInstanceOf(com.pi.smart_travel_guide.exception.ResourceNotFoundException.class);
    }

    @Test
    void shouldInferHasVisitedFromHasFavoriteDestination() {
        // Ali a explicitement hasVisited Paris et hasFavoriteDestination Rome.
        // Grâce à rdfs:subPropertyOf (hasFavoriteDestination ⊑ hasVisited),
        // le reasonneur RDFS déduit que Ali a aussi visité Rome.
        Map<String, Object> without = rdfService.getVisited("Ali", false);
        Map<String, Object> with = rdfService.getVisited("Ali", true);

        @SuppressWarnings("unchecked")
        List<Object> withoutDest = (List<Object>) without.get("destinations");
        @SuppressWarnings("unchecked")
        List<Object> withDest = (List<Object>) with.get("destinations");

        assertThat(withoutDest).contains("Paris");
        assertThat(withDest).contains("Rome");
        assertThat(withDest.size()).isGreaterThan(withoutDest.size());
    }
}