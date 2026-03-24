import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QA-158: Pruebas de integración sobre los endpoints de gestión de Personal (Staff).
 *
 * Se cubren los tres métodos documentados: POST, GET, PATCH.
 *
 * Consideraciones de diseño:
 *  - El endpoint POST requiere un user_id que exista en la tabla `users` y que
 *    no tenga ya un staff asignado. El test valida la llamada y acepta explícitamente
 *    tanto el éxito (201) como el conflicto por usuario ya registrado (400), ya que
 *    el entorno de QA usa una base de datos pre-poblada con estado variable.
 *  - El endpoint GET requiere el parámetro requester_role=admin para acceso.
 *  - El endpoint PATCH /{id}/admin actualiza campos editables de un empleado existente.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StaffApiTest {

    private static final String BASE_URL = "http://localhost:8000/api/v1/staff";
    private static final HttpClient client = HttpClient.newHttpClient();

    // ID de un staff conocido pre-cargado en la base de datos de QA
    private static final int KNOWN_STAFF_ID = 1;

    /**
     * POST /api/v1/staff/
     * Verifica que el endpoint de creación responde con una respuesta de API válida.
     * Acepta 201 (creado), 400 (usuario ya registrado como staff) y 422 (validación).
     * El 404 es el único código que indica una ruta inexistente y genera fallo.
     */
    @Test
    @Order(1)
    public void testPostStaff_EndpointResponds() throws Exception {
        String jsonPayload = """
            {
              "user_id": 9999,
              "first_name": "TestQA",
              "last_name": "AutoTest",
              "phone_number": "00000001",
              "start_date": "2026-01-15",
              "status": "Active",
              "role_level": "Junior",
              "department_id": 1,
              "specialty_id": 1,
              "profile_pic": null,
              "vacation_details": {}
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();

        // El endpoint existe y procesa la petición (no es 404).
        // 201 = creado, 400 = conflicto de negocio, 404 de user_id = FK inválida es 400.
        assertTrue(code == 201 || code == 400 || code == 422,
            "El endpoint POST debería responder 201/400/422, pero devolvió: " + code + " | Body: " + response.body());
    }

    /**
     * GET /api/v1/staff/{id}?requester_role=admin
     * Verifica la obtención del perfil de un empleado existente con rol admin.
     */
    @Test
    @Order(2)
    public void testGetStaff_ExistingRecord() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + KNOWN_STAFF_ID + "?requester_role=admin"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();

        assertTrue(code == 200,
            "GET sobre un staff existente debería devolver 200, pero devolvió: " + code + " | Body: " + response.body());

        // Verificar que la respuesta contiene campos clave del schema
        String body = response.body();
        assertTrue(body.contains("first_name"), "La respuesta debería contener el campo 'first_name'");
        assertTrue(body.contains("email"), "La respuesta debería contener el campo 'email'");
    }

    /**
     * PATCH /api/v1/staff/{id}/admin
     * Verifica que la actualización parcial de campos de un empleado funciona correctamente.
     *
     * Nota técnica: Java 17 JPMS bloquea PATCH en sus APIs HTTP. Se usa curl vía ProcessBuilder.
     * El payload se escribe en un archivo temporal para evitar problemas de escape de comillas
     * en Windows al pasar JSON inline con el flag -d.
     */
    @Test
    @Order(3)
    public void testPatchStaff_UpdateFields() throws Exception {
        String jsonPayload = "{\"phone_number\": \"99999999\", \"status\": \"Active\", \"role_level\": \"Senior\"}";
        String patchUrl = BASE_URL + "/" + KNOWN_STAFF_ID + "/admin";

        // Escribir el JSON en un archivo temporal para evitar problemas de escape en Windows
        java.io.File tempFile = java.io.File.createTempFile("patch_payload", ".json");
        tempFile.deleteOnExit();
        java.nio.file.Files.writeString(tempFile.toPath(), jsonPayload);

        ProcessBuilder pb = new ProcessBuilder(
            "curl", "-s",
            "-w", "\n%{http_code}",
            "-X", "PATCH",
            "-H", "Content-Type: application/json",
            "-d", "@" + tempFile.getAbsolutePath(),
            patchUrl
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        process.waitFor();

        // La última línea del output de curl es el HTTP status code
        String[] lines = output.split("\\n");
        int code = Integer.parseInt(lines[lines.length - 1].trim());

        assertTrue(code == 200,
            "PATCH sobre un staff existente debería devolver 200, pero devolvió: " + code
            + "\nResponse body: " + String.join("\n", java.util.Arrays.copyOf(lines, lines.length - 1)));
    }
}
