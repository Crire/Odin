# Migrations

O worker Go usa as tabelas de notificacao que pertencem ao banco canonico da General API.

A migration inicial dessas tabelas fica em:

```text
../General API/src/main/resources/db/migration/V1__initial_schema.sql
```

Este diretorio fica reservado para migrations operacionais futuras do worker, como indices adicionais, particionamento de `notification_deliveries` ou tabelas de dead-letter dedicadas.
