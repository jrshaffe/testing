# Shared services

This document describes how to install service operators and other packages that are needed to operate and provision services that can be used by the sample applictions.

Databases:

- [PostgreSQL](#postgres)
- [Oracle](#oracle)
- [MicrosoftSQL Server](#sqlserver)

## <a name="postgres"></a> PostgreSQL

For the PostgreSQL database we are using the [Tanzu Postgres Operator](https://docs.vmware.com/en/VMware-Tanzu-SQL-with-Postgres-for-Kubernetes/1.8/tanzu-postgres-k8s/GUID-install-operator.html) provided by [ VMware Tanzu SQL with Postgres for Kubernetes](https://docs.vmware.com/en/VMware-Tanzu-SQL-with-Postgres-for-Kubernetes/index.html).

### Install operator

> We will install directly from TanzuNet ignoring the [warnings in the doc](https://docs.vmware.com/en/VMware-Tanzu-SQL-with-Postgres-for-Kubernetes/1.8/tanzu-postgres-k8s/GUID-install-operator.html#relocate-images-to-a-private-registry). If you prefer, feel free to relocate the images and adjust these instructions accordingly.

> Log in to the [Tanzu Network](https://network.tanzu.vmware.com/) and then accept the EULA on the product page for [Packages for VMware Tanzu Data Services](https://network.tanzu.vmware.com/products/packages-for-vmware-tanzu-data-services).

Install the package repository `tanzu-data-services-repository`:

```
tanzu package repository add tanzu-data-services-repository \
  --url registry.tanzu.vmware.com/packages-for-vmware-tanzu-data-services/tds-packages:1.1.0 \
  -n tap-install
```

> Postgres Operator version 1.8.0 is part of TDS version 1.1.0

> We rely on the `tap-registry` secret in the `tap-install` namespace for the package installation.

Install the Postgres operator:

```
tanzu package install postgres-operator --package-name postgres-operator.sql.tanzu.vmware.com --version 1.8.0 \
  -n tap-install \
  -f services/postgres/create/postgres-operator-values.yaml
```

### Create database instance

Review the storage class and other configuration settings to use based on the [Deploying a Postgres Instance - Prerequisits](https://docs.vmware.com/en/VMware-Tanzu-SQL-with-Postgres-for-Kubernetes/1.8/tanzu-postgres-k8s/GUID-create-delete-postgres.html#prerequisites) section.

Update the [postgres-sample.yaml](postgres/create/postgres-sample.yaml) with the storage class and make any other adjsutments as needed.

> postgres-sample.yaml
```
apiVersion: sql.tanzu.vmware.com/v1
kind: Postgres
metadata:
  name: postgres-sample
spec:
  memory: 800Mi
  cpu: "0.8"
  storageClassName: standard
  storageSize: 10G
  pgConfig:
    dbname: postgres-sample
    username: pgadmin
    appUser: tanzu
```

Deploy the database instance manifest using:

```
kubectl apply -f services/postgres/create/postgres-sample.yaml
```

Wait for the database to become `Running`:

```
kubectl get postgres/postgres-sample
```

### Create the ClusterInstanceClass

In order to bind to the Postgres databas we need to create a `ClusterInstanceClass` that we can use.

> clusterinstanceclass.yaml
```
---
apiVersion: services.apps.tanzu.vmware.com/v1alpha1
kind: ClusterInstanceClass
metadata:
  name: postgres-sample
spec:
  description:
    short: PostgreSQL Database
  pool:
    group: sql.tanzu.vmware.com
    kind: Postgres
```

Create the class using:

```
kubectl apply -f services/postgres/service-binding/clusterinstanceclass.yaml
```

### Create the service claim

With all the pieces in place we can now create our service claim.

Let's check that we have the service class defined:

```
tanzu services classes list
```

We should see something like:

```
  NAME             DESCRIPTION                       
  postgres-sample  PostgreSQL Database               
```

Next, we can check that we have an instance to claim for the `postgres-sample` class:

```
tanzu services claimable list --class postgres-sample
```

We should see something like:

```
  NAME             NAMESPACE  KIND      APIVERSION               
  postgres-sample  default    Postgres  sql.tanzu.vmware.com/v1  
```

We can now claim this service:

```
tanzu service claim create postgres-sample-claim \
  --resource-name postgres-sample \
  --resource-kind Postgres \
  --resource-api-version sql.tanzu.vmware.com/v1
```

### Create the service claim

Since the claim is already created we can use it when we deploy our sample apps using the `--service-ref` option:

Change to the app directory:

```
cd rest-api-db
```

Then deploy the workload for the app:

```
tanzu apps workload create rest-api-db \
  --file config/workload.yaml \
  --env spring_profiles_active=postgres \
  --service-ref db=services.apps.tanzu.vmware.com/v1alpha1:ResourceClaim:postgres-sample-claim
```

## <a name="oracle"></a> Oracle

For the Oracle<sup>®️</sup> Database we use the [Oracle Database Operator for Kubernetes](https://github.com/oracle/oracle-database-operator) provided by Oracle Corporation. 

### Install operator

```
kubectl apply -f https://raw.githubusercontent.com/oracle/oracle-database-operator/main/oracle-database-operator.yaml
```

Check that the pods are running:

```
kubectl get pods -n oracle-database-operator-system
```

### Create database instance

For the sample Oracle XE database we create a [configuration file](oracle/create/xedb-sample.yaml) using the instructions outlined here: https://github.com/oracle/oracle-database-operator/blob/main/docs/sidb/README.md#xe-database.

> NOTE: We create this sample database in the `default` namespace.

First we need to create image pull secret using your [Oracle Container Registry](https://container-registry.oracle.com) credentials:

```
kubectl create secret docker-registry oracle-container-registry-secret \
  --docker-server=container-registry.oracle.com \
  --docker-username=${ORACLE_ID} \
  --docker-password=${ORACLE_PASSWORD} \
  --docker-email=${EMAIL}
```

Now, we can apply the `xedb-sample.yaml` manifest using:

```
kubectl apply -f oracle/xedb-sample.yaml
```

This should create a database and we can check the status by running:

```
kubectl get singleinstancedatabase.database.oracle.com 
```

It will take a few minutes for it to become `Healthy`.

### Create a user for our sample

We need to create a user and a table that our sample app can use. We create a user in the "Pluggable Database" named `XEPDB1` that was created during the installation. To be able to connect to the database we run the following to connect to the pod that is running the database:

```
kubectl exec -it $(kubectl get pod -l=app=xedb-sample -oname) -- /bin/bash
```

This should get us a bash prompt that we can use to start a SQL*Plus session as SYSDBA (when prompted, enter the SYS password (`s3cret!`) that was specified when we created the database):

```
sqlplus SYS@XEPDB1 AS SYSDBA
```

We can now create the user `tanzu`:

```
create user tanzu identified by "TAPt3st!";
grant connect to tanzu;
grant unlimited tablespace to tanzu;
grant create table to tanzu;
grant create sequence to tanzu;
```

Once this is done, we can log in as `tanzu` and create our table (when prompted, enter the tanzu password (`TAPt3st!`) that was specified when we created the user):

```
conn tanzu@XEPDB1
```

Finally, create the table:

```
create table "CUSTOMER"
  ("ID" number(19,0) generated always as identity,
    "FIRST_NAME" varchar2(255 char),
    "LAST_NAME" varchar2(255 char),
    primary key ("ID") using index
  );
```

We can now `exit` out of SQL*Plus and the bash session.

### Create bindable secret

We need to create a "bindable" secret based on the [Create a Binding Specification Compatible Secret](https://docs.vmware.com/en/Services-Toolkit-for-VMware-Tanzu-Application-Platform/0.7/svc-tlk/GUID-usecases-rds-ack-manual.html#create-a-binding-specification-compatible-secret-3) section in the [Services Toolkit documentation](https://docs.vmware.com/en/Services-Toolkit-for-VMware-Tanzu-Application-Platform/0.7/svc-tlk/GUID-usecases-rds-ack-manual.html#create-a-binding-specification-compatible-secret-3).

We need the following pieces:

1. A [secret templating service account](oracle/service-binding/secrettemplate-sa.yaml) that can read the Oracle `singleinstancedatabases` resource:
    ```
    kubectl apply -f oracle/service-binding/secrettemplate-sa.yaml
    ```
2. A [secret template](oracle/service-binding/bindable-oracle-secrettemplate.yaml) for our sample database:
    ```
    kubectl apply -f oracle/service-binding/bindable-oracle-secrettemplate.yaml
    ```
3. A [service instance class](oracle/service-binding/clusterinstanceclass.yaml) for `xedb-oracle`:
    ```
    kubectl apply -f oracle/service-binding/clusterinstanceclass.yaml
    ```
4. A [ClusterRole](oracle/service-binding/stk-secret-reader.yaml) for Services Toolkit to be able to read the secrets specified by the class:
    ```
    kubectl apply -f oracle/service-binding/stk-secret-reader.yaml
    ```

### Create the service claim

With all the pieces in place we can now create our service claim.

Let's check that we have the service class defined:

```
tanzu services classes list
```

We should see something like:

```
  NAME         DESCRIPTION                       
  xedb-oracle  Oracle XE SingleInstanceDatabase  
```

Next, we can check that we have an instance to claim for the `xedb-oracle` class:

```
tanzu services claimable list --class xedb-oracle
```

We should see:

```
  NAME             NAMESPACE  KIND    APIVERSION  
  oracle-bindable  default    Secret  v1          
```

We can now claim this service:

```
tanzu service claim create oracle-xedb-claim \
  --resource-name oracle-bindable \
  --resource-kind Secret \
  --resource-api-version v1
```

### Create the service claim

Since the claim is already created we can use it when we deploy our sample apps using the `--service-ref` option:

Change to the app directory:

```
cd rest-api-db
```

Then deploy the workload for the app:

```
tanzu apps workload create rest-api-db \
  --file config/workload.yaml \
  --env spring_profiles_active=oracle \
  --service-ref db=services.apps.tanzu.vmware.com/v1alpha1:ResourceClaim:oracle-xedb-claim
```

## <a name="sqlserver"></a> Microsoft SQL Server

TBD
