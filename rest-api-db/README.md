# rest-api-db repo

This repo provides a simple REST API app based on Spring Boot and JPA for database access.

It can be deployed as a standalone web app or as a Tanzu Application Platform workload resource.

## Deployment

This app can be deployed as a stand-alone web app, as a Tanzu Application Platform (TAP) workload resource or, as a Kubernetes Deployment and Service.

### Standalone app with embedded Tomcat server

You can build the project using Maven:

```bash
mvn clean package
```

To run the app using the embedded Tomcat server you can run this command:

```bash
mvn spring-boot:run
```

You can access the `customers` API endpoint using `curl`:

```bash
curl -w'\n' curl localhost:8080/customers  
```

You can access the API docs using `curl`:

```bash
curl http://localhost:8080/v3/api-docs  
```

### TAP deployment

See the [DEPLOYING.md](DEPLOYING.md)
