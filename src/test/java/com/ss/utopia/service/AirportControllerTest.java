package com.ss.utopia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.ss.utopia.AirportController;
import com.ss.utopia.exceptions.AirportAlreadyExistsException;
import com.ss.utopia.exceptions.AirportNotFoundException;
import com.ss.utopia.models.Airport;
import com.ss.utopia.services.AirportService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Profile("test")
@SpringBootTest
public class AirportControllerTest {

  final String SERVICE_PATH_AIRPORTS = "/airports";

  @Mock
  AirportService service;

  @InjectMocks
  AirportController controller;

  MockMvc mvc;
  Airport testAirport;
  List<Airport> testAirportList;

	@BeforeEach
	void setup() throws Exception {
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
    Mockito.reset(service);

    testAirport = new Airport("TES", "Testsville");

    testAirportList = new ArrayList<Airport>();
    testAirportList.add(new Airport("DCA", "Washington DC"));
    testAirportList.add(new Airport("IAH", "Huston"));
    testAirportList.add(new Airport("JFK", "New York"));
    testAirportList.add(new Airport("LGA", "New York"));
    testAirportList.add(new Airport("LAX", "Los Angeles"));
    testAirportList.add(new Airport("MCO", "Orlando"));
    testAirportList.add(new Airport("MIA", "Miami"));
    testAirportList.add(new Airport("ORD", "Chicago"));
    testAirportList.add(new Airport("SAN", "San Diego"));
    testAirportList.add(new Airport("SFO", "San Francisco"));
    testAirportList.add(new Airport("SJU", "Puerto Rico"));
	}

  @Test
  void test_validAirportModel_getAirportIataId() {
    assertEquals("TES", testAirport.getAirportIataId());
  }

  @Test
  void test_validAirportModel_getAirportCityName() {
    assertEquals("Testsville", testAirport.getAirportCityName());
  }

  @Test
  void test_findAllAirports_withValidAirports_thenStatus200() {    
    try {
      when(service.findAll()).thenReturn(testAirportList);

      MvcResult response = mvc.perform(get(SERVICE_PATH_AIRPORTS)
      .header("Accept", "application/json"))
      .andExpect(status().is(200))
      .andReturn();
  
      List<Airport> actual = Arrays.stream(
        new ObjectMapper().readValue(response.getResponse().getContentAsString(),
        Airport[].class)).collect(Collectors.toList());
  
      assertEquals(testAirportList.size(), actual.size());
      for(int i = 0; i < testAirportList.size(); i++) {
        assertEquals(testAirportList.get(i).getAirportIataId(), actual.get(i).getAirportIataId());
        assertEquals(testAirportList.get(i).getAirportCityName(), actual.get(i).getAirportCityName());
      }
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_findAllAirports_withNoValidAirports_thenStatus204() {    
    try {
      when(service.findAll()).thenReturn(Collections.emptyList());

      MvcResult response = mvc.perform(get(SERVICE_PATH_AIRPORTS)
      .header("Accept", "application/json"))
      .andExpect(status().is(204))
      .andReturn();
  
      assertEquals("", response.getResponse().getContentAsString());
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_findByIataId_withValidAirport_thenStatus200() {    
    try {
      when(service.findByIataId(testAirport.getAirportIataId())).thenReturn(testAirport);

      MvcResult response = mvc.perform(get(SERVICE_PATH_AIRPORTS + "/" + testAirport.getAirportIataId())
      .header("Accept", "application/json"))
      .andExpect(status().is(200))
      .andReturn();
  
      Airport actual = new ObjectMapper().readValue(response
      .getResponse().getContentAsString(), Airport.class);
  
      assertEquals(testAirport.getAirportIataId(), actual.getAirportIataId());
      assertEquals(testAirport.getAirportCityName(), actual.getAirportCityName());
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_findByIataId_withInvalidAirport_thenStatus404() {    
    try {
      String invalidIataId = "123";
      when(service.findByIataId(invalidIataId)).thenThrow(new AirportNotFoundException());

      MvcResult response = mvc.perform(get(SERVICE_PATH_AIRPORTS + "/" + invalidIataId)
      .header("Accept", "application/json"))
      .andExpect(status().is(404))
      .andReturn();
  
      assertEquals("", response.getResponse().getContentAsString());
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_findByIataId_withBadParams_thenStatus400() {    
    try {
      String invalidIataId = "thisistoolong";
      when(service.findByIataId(invalidIataId)).thenThrow(new IllegalArgumentException());

      mvc.perform(get(SERVICE_PATH_AIRPORTS + "/" + invalidIataId)
      .header("Accept", "application/json"))
      .andExpect(status().is(400))
      .andReturn();
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_findByCity_withValidAirports_singleResult_thenStatus200() {    
    try {
      String testCitySearch = "wash"; // Should only return DCA
      HashMap<String, String> filterMap = new HashMap<String, String>();
      filterMap.put("searchTerms", testCitySearch);
      when(service.findBySearchAndFilter(filterMap)).thenReturn(testAirportList.stream()
      .filter(airport -> airport.getAirportCityName().toLowerCase().contains(testCitySearch))
      .collect(Collectors.toList()));

      MvcResult response = mvc.perform(get(SERVICE_PATH_AIRPORTS + "/search?city=" + testCitySearch)
      .header("Accept", "application/json"))
      .andExpect(status().is(200))
      .andReturn();

      List<Airport> actual = Arrays.stream(
        new ObjectMapper().readValue(response.getResponse().getContentAsString(),
        Airport[].class)).collect(Collectors.toList());

      assertEquals(1, actual.size()); 
      assertEquals("DCA", actual.get(0).getAirportIataId());
      assertEquals("Washington DC", actual.get(0).getAirportCityName());
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_findByCity_withValidAirports_multiResult_thenStatus200() {    
    try {
      String testCitySearch = "york"; // Should return both JFK & LGA
      HashMap<String, String> filterMap = new HashMap<String, String>();
      filterMap.put("searchTerms", testCitySearch);
      when(service.findBySearchAndFilter(filterMap)).thenReturn(testAirportList.stream()
      .filter(airport -> airport.getAirportCityName().toLowerCase().contains(testCitySearch))
      .collect(Collectors.toList()));

      MvcResult response = mvc.perform(get(SERVICE_PATH_AIRPORTS + "/search?city=" + testCitySearch)
      .header("Accept", "application/json"))
      .andExpect(status().is(200))
      .andReturn();

      List<Airport> actual = Arrays.stream(
        new ObjectMapper().readValue(response.getResponse().getContentAsString(),
        Airport[].class)).collect(Collectors.toList());

      assertEquals(2, actual.size()); 
      assertTrue(actual.get(0).getAirportIataId().equals("JFK") || actual.get(1).getAirportIataId().equals("LGA"));
      assertTrue(actual.get(0).getAirportIataId().equals("JFK") || actual.get(1).getAirportIataId().equals("LGA"));
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_findByCity_withNoValidAirports_thenStatus204() {    
    try {
      String testCitySearch = "dal"; // Should return no content as Dallas is not in the list
      HashMap<String, String> filterMap = new HashMap<String, String>();
      filterMap.put("searchTerms", testCitySearch);
      when(service.findBySearchAndFilter(filterMap)).thenReturn(testAirportList.stream()
      .filter(airport -> airport.getAirportCityName().toLowerCase().contains(testCitySearch))
      .collect(Collectors.toList()));

      MvcResult response = mvc.perform(get(SERVICE_PATH_AIRPORTS + "/search?city=" + testCitySearch)
      .header("Accept", "application/json"))
      .andExpect(status().is(204))
      .andReturn();

      assertEquals("", response.getResponse().getContentAsString());
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_insert_withValidAirport_thenStatus201() {    
    try {
      when(service.insert(testAirport.getAirportIataId(), testAirport.getAirportCityName()))
      .thenReturn(testAirport);

      MvcResult response = mvc.perform(post(SERVICE_PATH_AIRPORTS)
      .header("Accept", "application/json")
      .content(new ObjectMapper().writeValueAsString(testAirport)))
      .andExpect(status().is(201))
      .andReturn();

      Airport actual = new ObjectMapper().readValue(response
      .getResponse().getContentAsString(), Airport.class);

      assertEquals(testAirport.getAirportIataId(), actual.getAirportIataId());
      assertEquals(testAirport.getAirportCityName(), actual.getAirportCityName());
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_insert_withDuplicateAirport_thenStatus409() {    
    try {
      when(service.insert(testAirport.getAirportIataId(), testAirport.getAirportCityName()))
      .thenThrow(new AirportAlreadyExistsException("duplicate"));

      mvc.perform(post(SERVICE_PATH_AIRPORTS)
      .header("Accept", "application/json")
      .content(new ObjectMapper().writeValueAsString(testAirport)))
      .andExpect(status().is(409))
      .andReturn();
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_insert_withInvalidAirport_thenStatus400() {    
    try {
      String invalidIataId = "ThisIsTooLong";
      when(service.insert(invalidIataId, testAirport.getAirportCityName()))
      .thenThrow(new IllegalArgumentException());

      MvcResult response = mvc.perform(post(SERVICE_PATH_AIRPORTS)
      .header("Accept", "application/json")
      .content(new ObjectMapper().writeValueAsString(new Airport(invalidIataId, testAirport.getAirportCityName()))))
      .andExpect(status().is(400))
      .andReturn();

      assertEquals("", response.getResponse().getContentAsString());
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_insert_withBadParams_thenStatus400() {    
    try {
      mvc.perform(post(SERVICE_PATH_AIRPORTS)
      .header("Accept", "application/json")
      .content(""))
      .andExpect(status().is(400))
      .andReturn();
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_update_withValidAirport_thenStatus202() {    
    try {
      String newCityName = "newcityname"; // only Airport names can be updated, as other data relies on the unique iataId
      when(service.update(testAirport.getAirportIataId(), newCityName))
      .thenReturn(new Airport(testAirport.getAirportIataId(), newCityName));

      MvcResult response = mvc.perform(put(SERVICE_PATH_AIRPORTS + "/" + testAirport.getAirportIataId())
      .header("Accept", "application/json")
      .content(newCityName))
      .andExpect(status().is(202))
      .andReturn();

      Airport actual = new ObjectMapper().readValue(response
      .getResponse().getContentAsString(), Airport.class);

      assertEquals(testAirport.getAirportIataId(), actual.getAirportIataId());
      assertEquals(newCityName, actual.getAirportCityName());
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_update_withInvalidAirport_thenStatus404() {    
    try {
      String invalidIataId = "NOT"; // as the IATA Code "NOT" is not present in the testAirportsList
      when(service.update(invalidIataId, testAirport.getAirportCityName()))
      .thenThrow(new AirportNotFoundException());

      mvc.perform(put(SERVICE_PATH_AIRPORTS + "/" + invalidIataId)
      .header("Accept", "application/json")
      .content(testAirport.getAirportCityName()))
      .andExpect(status().is(404))
      .andReturn();
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_update_withInvalidInput_thenStatus400() {    
    try {
      String invalidNewCityName = ""; // as names cannot be empty

      mvc.perform(put(SERVICE_PATH_AIRPORTS + "/" + testAirport.getAirportIataId())
      .header("Accept", "application/json")
      .content(invalidNewCityName))
      .andExpect(status().is(400))
      .andReturn();
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_update_withBadParams_thenStatus400() {    
    try {
      mvc.perform(post(SERVICE_PATH_AIRPORTS)
      .header("Accept", "application/json")
      .content(""))
      .andExpect(status().is(400))
      .andReturn();
    } catch(Exception e) {
      fail();
    }
  }

  @Test
  void test_delete_withValidAirport_thenStatus204() {    
    try {
      mvc.perform(delete(SERVICE_PATH_AIRPORTS + "/" + testAirport.getAirportIataId())
      .header("Accept", "application/json"))
      .andExpect(status().is(204))
      .andReturn();
    } catch(Exception e) {
      fail();
    }
  }
}
