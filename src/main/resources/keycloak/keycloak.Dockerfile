FROM quay.io/keycloak/keycloak:26.4.0
COPY realm-export.json /opt/keycloak/data/import/realm-export.json
