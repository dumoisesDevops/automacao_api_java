package testeApi;

import com.github.javafaker.Faker;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class APITest {

    private static String accessToken;
    private static final String JSON_FILE_PATH = "C:\\Users\\User\\Desktop\\ProjetosEstudos\\automacao_mobile\\automacao_java_api\\data\\produto_fake.json";
    private static String productId;

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "https://dummyjson.com";
    }

    @Test
    @Order(1)
    @Description("Verifica se endpoint /user esta listando todos os usuarios para autenticacao")
    public void testGetUsers() {
        Response response = given()
                .when()
                .get("/users")
                .then()
                .extract().response();

        System.out.println("Código de Status: " + response.statusCode());
        System.out.println("Body da Resposta:");
        response.getBody().prettyPrint();

        assertEquals(200, response.statusCode(), "O código de status deve ser 200");
        assertNotNull(response.getBody(), "O corpo da resposta não deve ser nulo");

        int usersCount = response.jsonPath().getList("users").size();
        assertTrue(usersCount > 0, "A lista de usuários deve conter pelo menos 1 elemento");
    }

    @Test
    @Order(2)
    @Description("Verifica  o status da aplicacao")
    public void testGetTest() {
        Response response = given()
                .when()
                .get("/test")
                .then()
                .extract().response();

        System.out.println("Código de Status: " + response.statusCode());
    }

    @Test
    @Order(3)
    @Description("Verifica login e efetuado corretamente e armazena  novo token de autorizacao")
    public void testGenerateToken() {
        String requestBody = """
        {
            "username": "emilys",
            "password": "emilyspass"
        }
        """;

        Response response =
                given()
                        .header("Content-Type", "application/json")
                        .body(requestBody)
                        .when()
                        .post("/auth/login")
                        .then()
                        .extract()
                        .response();

        System.out.println("Código de Status: " + response.statusCode());
        System.out.println("Body da Resposta:");
        response.getBody().prettyPrint();

        assertEquals(200, response.statusCode(), "O código de status deve ser 200");
        assertNotNull(response.getBody(), "O corpo da resposta não deve ser nulo");

        accessToken = response.jsonPath().getString("accessToken");
        assertNotNull(accessToken, "O token não deve ser nulo");

        System.out.println("Token gerado: " + accessToken);
    }

    @Test
    @Order(4)
    @Description("Verifica se  o endpoint /products exibe todos produtos cadastrados")
    public void testGetProductsWithToken() {
        assertNotNull(accessToken, "Token não foi gerado! O teste de login precisa ser executado primeiro.");

        Response response =
                given()
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + accessToken)
                        .when()
                        .get("/auth/products")
                        .then()
                        .extract()
                        .response();

        System.out.println("Código de Status: " + response.statusCode());
        System.out.println("Body da Resposta: " + response.getBody().asString());

        assertEquals(200, response.statusCode(), "Erro ao obter produtos: código de status inesperado.");
        assertNotNull(response.jsonPath().getList("products"), "A lista de produtos não deve ser nula.");
    }

    @Test
    @Order(5)
    @Description("Gera produtos  aleatorios  com dados fakes e armazena  em json")
    public void testGenerateFakeProductData() {
        Faker faker = new Faker(new Locale("en-US"));

        JSONObject productJson = new JSONObject();
        productJson.put("title", faker.commerce().productName());
        productJson.put("description", faker.lorem().sentence());
        productJson.put("price", faker.number().numberBetween(10, 100));
        productJson.put("discountPercentage", faker.number().randomDouble(2, 5, 20));
        productJson.put("rating", faker.number().randomDouble(2, 3, 5));
        productJson.put("stock", faker.number().numberBetween(10, 100));
        productJson.put("brand", faker.company().name());
        productJson.put("category", "fragrances");
        productJson.put("thumbnail", "https://i.dummyjson.com/data/products/11/thumbnail.jpg");

        try (FileWriter file = new FileWriter(JSON_FILE_PATH)) {
            file.write(productJson.toString());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Erro ao salvar os dados do produto no arquivo JSON.");
        }

        System.out.println("✅ Produto fake gerado e salvo em JSON!");
        System.out.println(productJson.toString());
    }

    @Test
    @Order(6)
    @Description("Verifica se novo produto e cadastrado com sucesso ")
    public void testCreateProduct() {
        assertNotNull(accessToken, "Token não foi gerado! O teste de login precisa ser executado primeiro.");

        String requestBody;
        try {
            requestBody = new String(Files.readAllBytes(Paths.get(JSON_FILE_PATH)));
        } catch (IOException e) {
            fail("Erro ao ler o arquivo JSON.");
            return;
        }

        Response response =
                given()
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + accessToken)
                        .body(requestBody)
                        .when()
                        .post("/products/add")
                        .then()
                        .extract()
                        .response();

        System.out.println("Código de Status: " + response.statusCode());
        System.out.println("Body da Resposta: " + response.getBody().asString());

        assertEquals(201, response.statusCode(), "O código de status deve ser 201");
        productId = response.jsonPath().getString("id");
    }

    @Test
    @Order(7)
    @Description("Verifica se lista todos os produtos  no endpoint / produtos mesmo sem autenticacao ")
    public void testGetProdutos() {
        Response response = given()
                .when()
                .get("/products")
                .then()
                .extract().response();

        System.out.println("Código de Status: " + response.statusCode());
        System.out.println("Body da Resposta:");
        response.getBody().prettyPrint();

        assertEquals(200, response.statusCode(), "O código de status deve ser 200");
        assertNotNull(response.getBody(), "O corpo da resposta não deve ser nulo");

        int productsCount = response.jsonPath().getList("products").size();
        assertTrue(productsCount > 0, "A lista de products deve conter pelo menos 1 elemento");
    }

    @Test
    @Order(8)
    @Step("Verifica se e listados  produtos de acordo com id informado")
    public void testGetProductById() {
        // Verifica se o ID foi armazenado corretamente
        assertNotNull(productId, "O ID do produto não foi armazenado corretamente.");

        Response response =
                given()
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + accessToken)
                        .when()
                        .get("/products/" + 1) // Utilizando o ID armazenado
                        .then()
                        .extract()
                        .response();

        System.out.println("Código de Status: " + response.statusCode());
        System.out.println("Body da Resposta: " + response.getBody().asString());

        assertEquals(200, response.statusCode(), "O código de status deve ser 200");
    }

}
