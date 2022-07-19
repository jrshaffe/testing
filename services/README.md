# Shared services

This document describes how to install service operators and other packages that are needed to operate and provision services that can be used by the sample applictions.

## Oracle

For the Oracle<sup>®️</sup> Database we use the [Oracle Database Operator for Kubernetes](https://github.com/oracle/oracle-database-operator) provided by Oracle Corporation. 

### Install operator

```
kubectl apply -f https://raw.githubusercontent.com/oracle/oracle-database-operator/main/oracle-database-operator.yaml
```

Check that the pods are running:

```
kubectl get pods -n oracle-database-operator-system
```

### Create database

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
  ("ID" number(19,0) generated always as identity start with 0 minvalue 0
      cache 20 noorder  nocycle  nokeep  noscale  not null enable,
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

Once the claim is created we can use it when we deploy our sample apps using the `--service-ref` option:

```
tanzu apps workload create rest-api-db \
  --file config/workload.yaml \
  --env spring_profiles_active=oracle \
  --service-ref db=services.apps.tanzu.vmware.com/v1alpha1:ResourceClaim:oracle-xedb-claim
```

## Microsoft SQL Server

TBD
