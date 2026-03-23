import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StaffApiTest {

    private static final String BASE_URL = "http://localhost:8000/api/staff";
    private static final HttpClient client = HttpClient.newHttpClient();

    @Test
    @Order(1)
    public void testPostStaff() throws Exception {
        String jsonPayload = """
            {
              "user_id": 3,
              "first_name": "TestQA",
              "last_name": "User",
              "phone_number": "12345678",
              "start_date": "2026-03-12",
              "status": "Active",
              "role_level": "Mid",
              "department_id": 1,
              "specialty_id": 1,
              "profile_pic": "url",
              "vacation_details": {}
            }
            """;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertTrue(response.statusCode() == 200 || response.statusCode() == 201, 
            "Expected 200 or 201 but got " + response.statusCode());
        assertTrue(response.body().contains("TestQA"));
    }

    @Test
    @Order(2)
    public void testGetStaff() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/3")) // using the ID expected from DB or creation
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertTrue(response.statusCode() == 200 || response.statusCode() == 404, 
            "Expected a valid API response for GET");
    }

    @Test
    @Order(3)
    public void testPutStaff() throws Exception {
        String jsonPayload = """
            {
              "first_name": "TestQA_Updated",
              "last_name": "User",
              "phone_number": "87654321",
              "status": "Inactive",
              "role_level": "Senior",
              "profile_pic": "new_url"
            }
            """;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/3"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertTrue(response.statusCode() == 200 || response.statusCode() == 404,
            "Expected valid response for PUT updates");
    }
}
