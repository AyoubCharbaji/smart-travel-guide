package com.pi.smart_travel_guide.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RdfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAllAccommodations() throws Exception {
        mockMvc.perform(get("/api/accommodations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void shouldReturnAccommodationById() throws Exception {
        mockMvc.perform(get("/api/accommodations/Hotel_Medina_Tunis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Hôtel Médina Tunis"))
                .andExpect(jsonPath("$.type").value("Hotel"));
    }

    @Test
    void shouldReturn404ForUnknownAccommodation() throws Exception {
        mockMvc.perform(get("/api/accommodations/Unknown_Hotel"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnCitiesByCountry() throws Exception {
        mockMvc.perform(get("/api/cities").param("country", "Tunisia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void shouldSearchHotelsByStarsAndPrice() throws Exception {
        mockMvc.perform(get("/api/hotels/search")
                        .param("minStars", "4")
                        .param("maxPrice", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturnTravelerStats() throws Exception {
        mockMvc.perform(get("/api/stats/travelers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void shouldReturnInferenceComparison() throws Exception {
        mockMvc.perform(get("/api/inference/compare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triplets_bruts").exists())
                .andExpect(jsonPath("$.triplets_apres_regles_R1_R2").exists())
                .andExpect(jsonPath("$.triplets_apres_RDFS").exists());
    }

    @Test
    void shouldReturnVisitedDestinationsWithInference() throws Exception {
        mockMvc.perform(get("/api/travelers/Ali/visited")
                        .param("withInference", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voyageur").value("Ali"))
                .andExpect(jsonPath("$.avecInference").value(true));
    }
}