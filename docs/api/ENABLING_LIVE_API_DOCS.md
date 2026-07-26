# Enabling Live API Documentation

This guide adds auto-generated OpenAPI documents and an interactive Swagger UI
to all four services, served from the public subdomains.

The result is that a reviewer can open a browser at
`https://decision.payroll-credit.com/swagger-ui.html`, read the full contract,
click **Try it out**, and execute a real request against the live system —
with no tooling, no repository clone, and no command line.

That is a materially stronger artifact than a YAML file, and it also removes
the risk of the hand-written specifications drifting from the code.

---

## Before you begin — read this

**Sequencing.** These changes require rebuilding and redeploying all four
services. Any redeploy carries a risk of breaking a currently working system.
Consider the order of your remaining work:

- If the demonstration video has **not** yet been recorded, doing this first is
  worthwhile — a Swagger UI on screen is a strong moment in the recording.
- If the video **has** been recorded and the system is in a known-good state
  you would rather not disturb, the hand-written specifications in this
  directory already satisfy the requirement for formal API documentation. This
  step is then optional.

**Test locally first.** Run `docker compose up --build` on your workstation and
confirm all four Swagger UIs load before touching the EC2 host. Do not debug a
build failure over SSH on the production instance.

**Version pinning.** The library versions below were current as of mid-2026.
Check for a newer patch release before adding them, and confirm compatibility
with Spring Boot 3.2 and .NET 8 specifically.

---

## Part 1 — Java services (Spring Boot 3.2)

Apply the following to all three Java services: `ingestion-service`,
`income-verification-service`, and `decision-service`.

### 1.1 Add the dependency

In each service's `pom.xml`, inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

This single starter brings in both the OpenAPI document generator and the
Swagger UI web assets. No other dependency is required.

### 1.2 Configure paths and metadata

In each service's `src/main/resources/application.yml`, add:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
    display-request-duration: true
  show-actuator: false
```

`display-request-duration` is worth enabling: when a reviewer clicks *Try it
out*, Swagger UI shows the round-trip time next to the response, which is a
small but real demonstration of responsiveness.

### 1.3 Add document metadata

Create one configuration class per service. For `decision-service`, at
`src/main/java/com/mota/decision/config/OpenApiConfig.java`:

```java
package com.mota.decision.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI decisionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payroll Credit Platform — Decision Service")
                        .version("1.0.0")
                        .description("""
                                Deterministic, auditable credit eligibility decisioning.

                                The rules engine uses no machine learning. Every decision
                                persists a plain-language reasoning field, satisfying CFPB
                                adverse action notice requirements under Regulation B and
                                Basel II model reproducibility requirements.
                                """)
                        .contact(new Contact()
                                .name("Clayton Soares da Mota")
                                .url("https://github.com/claytonmota/payroll-credit-mvp"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("https://decision.payroll-credit.com")
                                .description("Public deployment (AWS EC2, us-east-1)"),
                        new Server()
                                .url("http://localhost:8083")
                                .description("Local Docker Compose stack")));
    }
}
```

Repeat for the other two services, changing the package, bean name, title,
description, and server URLs to match. Copy the `description` text from the
corresponding hand-written YAML in this directory so the wording stays
consistent.

### 1.4 Annotate the controllers

Without annotations, springdoc still produces a valid document, but it will be
bare — no descriptions, no examples, no meaningful response documentation. The
annotations are what make the generated output equivalent in quality to the
hand-written specifications.

Example for `EligibilityController`:

```java
package com.mota.decision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
// ... existing imports

@RestController
@RequestMapping("/v1/eligibility")
@Tag(name = "Eligibility", description = "Current decisions and full audit history.")
public class EligibilityController {

    @GetMapping("/{userId}")
    @Operation(
            summary = "Retrieve the most recent eligibility decision",
            description = """
                    Returns the latest decision on record for this worker.

                    Returns 404 if no decision has been produced yet. Because the
                    pipeline is asynchronous, a 404 immediately after submitting
                    payroll events is expected and transient.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Most recent eligibility decision.",
                    content = @Content(
                            schema = @Schema(implementation = EligibilityDecision.class),
                            examples = @ExampleObject(
                                    name = "approved",
                                    summary = "Approved — stable income, high confidence",
                                    value = """
                                            {
                                              "decisionId": "3f2c9a41-8e17-4d5b-9c2a-7b1e40f3d8aa",
                                              "userId": "user-1001",
                                              "decision": "APPROVED",
                                              "creditLimitUsd": 1592.01,
                                              "suggestedApr": 18.99,
                                              "averageMonthlyIncome": 5306.70,
                                              "incomeConfidenceScore": 1.0,
                                              "incomeStabilityLabel": "STABLE",
                                              "reasoning": "Stable income stream with high confidence score (1.00).",
                                              "decidedAt": "2026-07-13T23:03:58.412009Z"
                                            }
                                            """))),
            @ApiResponse(
                    responseCode = "404",
                    description = "No decision has been produced for this worker yet.")
    })
    public ResponseEntity<?> getDecision(
            @Parameter(description = "Worker identifier, as submitted to the Ingestion Service.",
                       example = "user-1001")
            @PathVariable String userId) {
        // ... existing implementation unchanged
    }
}
```

Also annotate the DTOs so field descriptions appear in the schema view:

```java
public class EligibilityDecision {

    @Schema(description = "Unique identifier for this decision record.",
            example = "3f2c9a41-8e17-4d5b-9c2a-7b1e40f3d8aa")
    private String decisionId;

    @Schema(description = """
            Recommended credit limit in USD. Sized at 30% of average monthly
            income for approvals, 10% for referrals, and 0 for denials.
            """,
            example = "1592.01")
    private Double creditLimitUsd;

    // ... remaining fields
}
```

---

## Part 2 — C# service (.NET 8)

For `credit-profile-service`.

### 2.1 Add the package

```bash
cd credit-profile-service/src/CreditProfileService
dotnet add package Swashbuckle.AspNetCore --version 6.6.2
```

### 2.2 Register the services

In `Program.cs`, before `var app = builder.Build();`:

```csharp
using Microsoft.OpenApi.Models;

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "Payroll Credit Platform — Credit Profile Service",
        Version = "1.0.0",
        Description =
            "Aggregate credit profile combining payroll income and bureau data.\n\n" +
            "Written in C# on .NET 8, this service consumes the same `income.verified` " +
            "Kafka topic as the Java-based Decision Service, in an independent consumer " +
            "group, with no shared library between them.",
        Contact = new OpenApiContact
        {
            Name = "Clayton Soares da Mota",
            Url = new Uri("https://github.com/claytonmota/payroll-credit-mvp")
        },
        License = new OpenApiLicense
        {
            Name = "Apache 2.0",
            Url = new Uri("https://www.apache.org/licenses/LICENSE-2.0")
        }
    });

    options.AddServer(new OpenApiServer
    {
        Url = "https://creditprofile.payroll-credit.com",
        Description = "Public deployment (AWS EC2, us-east-1)"
    });
    options.AddServer(new OpenApiServer
    {
        Url = "http://localhost:8084",
        Description = "Local Docker Compose stack"
    });

    // Surface XML doc comments in the generated schema.
    var xmlFile = $"{System.Reflection.Assembly.GetExecutingAssembly().GetName().Name}.xml";
    var xmlPath = Path.Combine(AppContext.BaseDirectory, xmlFile);
    if (File.Exists(xmlPath))
    {
        options.IncludeXmlComments(xmlPath);
    }
});
```

### 2.3 Enable the middleware

After `var app = builder.Build();`:

```csharp
app.UseSwagger(options =>
{
    options.RouteTemplate = "v3/api-docs/{documentName}.json";
});
app.UseSwaggerUI(options =>
{
    options.SwaggerEndpoint("/v3/api-docs/v1.json", "Credit Profile Service v1");
    options.RoutePrefix = "swagger-ui";
    options.DisplayRequestDuration();
});
```

Note the deliberate path choices: `/v3/api-docs` and `/swagger-ui` mirror the
springdoc defaults used by the Java services, so all four services expose
documentation at consistent URLs.

### 2.4 Enable XML documentation output

In `CreditProfileService.csproj`, inside the main `<PropertyGroup>`:

```xml
<GenerateDocumentationFile>true</GenerateDocumentationFile>
<NoWarn>$(NoWarn);1591</NoWarn>
```

`1591` suppresses the warning for public members lacking XML comments, which
would otherwise flood the build output.

Then document the endpoints with standard XML comments:

```csharp
/// <summary>Retrieve the aggregate credit profile for a worker.</summary>
/// <remarks>
/// Returns the aggregate profile document, including the current income
/// assessment, the bureau lookup result, the thin-file classification, and
/// the full sequence of income snapshots observed over time.
///
/// Identifiers ending in <c>-thinfile</c> deterministically produce a
/// thin-file result from the bureau stub.
/// </remarks>
/// <param name="userId">Worker identifier, as submitted to the Ingestion Service.</param>
/// <response code="200">Aggregate credit profile.</response>
/// <response code="404">No profile exists for this worker yet.</response>
app.MapGet("/v1/credit-profile/{userId}", async (string userId, ICreditProfileRepository repo) =>
{
    // ... existing implementation unchanged
})
.WithName("GetCreditProfile")
.WithTags("Credit Profile")
.Produces<CreditProfileDocument>(StatusCodes.Status200OK)
.Produces(StatusCodes.Status404NotFound);
```

---

## Part 3 — Reverse proxy

The Swagger UI assets and the JSON document are served from the same origin as
the API, so if your nginx configuration proxies everything under `/` to the
container, no change is required.

If instead you proxy only specific path prefixes, add the two documentation
paths. For each server block:

```nginx
location /swagger-ui {
    proxy_pass http://127.0.0.1:8083;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

location /v3/api-docs {
    proxy_pass http://127.0.0.1:8083;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

The `X-Forwarded-Proto` header matters. Without it, springdoc generates
`http://` URLs in the served document even though the site is HTTPS, and the
*Try it out* button then fails with a mixed-content error in the browser.

---

## Part 4 — Verify

Locally first:

```bash
docker compose up --build

# Then open each in a browser:
#   http://localhost:8081/swagger-ui.html
#   http://localhost:8082/swagger-ui.html
#   http://localhost:8083/swagger-ui.html
#   http://localhost:8084/swagger-ui
```

For each service, confirm: the page loads, endpoints are listed with
descriptions, schemas expand with field-level documentation, and *Try it out*
executes successfully against the local stack.

Then deploy:

```bash
git add -A
git commit -m "feat(api): expose OpenAPI documents and Swagger UI on all services

Adds springdoc-openapi to the three Spring Boot services and Swashbuckle to
the .NET credit profile service. Each service now serves its contract at
/v3/api-docs and an interactive UI at /swagger-ui, so the API can be
inspected and exercised from a browser without cloning the repository."

git push
```

On the host:

```bash
cd ~/payroll-credit-mvp
git pull
docker compose build
docker compose up -d
sleep 30
docker compose ps
```

Confirm all eight containers report `Up`, then check each public URL:

```
https://ingestion.payroll-credit.com/swagger-ui.html
https://income.payroll-credit.com/swagger-ui.html
https://decision.payroll-credit.com/swagger-ui.html
https://creditprofile.payroll-credit.com/swagger-ui
```

---

## Part 5 — Replace the hand-written specifications

Once the live documents are serving, export them and let the generated output
supersede the hand-written files:

```bash
curl -s https://ingestion.payroll-credit.com/v3/api-docs \
  -o docs/api/ingestion-service.openapi.json
curl -s https://income.payroll-credit.com/v3/api-docs \
  -o docs/api/income-verification-service.openapi.json
curl -s https://decision.payroll-credit.com/v3/api-docs \
  -o docs/api/decision-service.openapi.json
curl -s https://creditprofile.payroll-credit.com/v3/api-docs/v1.json \
  -o docs/api/credit-profile-service.openapi.json
```

Keep the YAML files alongside the JSON, or delete them — but do not maintain
both by hand. Whichever set you keep must be the one that cannot drift.

---

## A note on exposure

Swagger UI documents your API to everyone, including the bots already
scanning your open ports. For this system that is acceptable and even
desirable: the endpoints are already public, unauthenticated, and carry no
real personal data, and the entire point of the artifact is to be inspectable.

It would not be acceptable on a system holding real consumer financial data.
When the Identity Service on the roadmap introduces authentication, the
documentation endpoints should move behind it, or be restricted to
non-production environments.
