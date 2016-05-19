<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="urn:jboss:domain:4.0"
	xmlns:ds="urn:jboss:domain:datasources:4.0">
	<xsl:output method="xml" indent="yes"/> 
	<xsl:template match="ds:datasources">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<ds:datasource jndi-name="java:jboss/datasources/MyKeycloakDS" pool-name="MyKeycloakDS" enabled="true" use-java-context="true">
			    <ds:connection-url>jdbc:postgresql:keycloak_db</ds:connection-url>
			    <ds:driver>psql</ds:driver>
			    <ds:security>
				<ds:user-name>keycloak_db</ds:user-name>
				<ds:password>keycloakDb999</ds:password>
			    </ds:security>
			</ds:datasource>
			<xsl:apply-templates select="*"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="ds:drivers">
		<xsl:apply-templates select="@*"/>
                    <ds:driver name="psql" module="org.postgresql">
                        <ds:xa-datasource-class>org.postgresql.Driver</ds:xa-datasource-class>
                    </ds:driver>
		<xsl:apply-templates select="*"/>
	</xsl:template>

	<xsl:template match="text()">
		<xsl:value-of select="normalize-space()"/>
	</xsl:template>
		
	<!-- umkopieren der restlichen Zeilen -->
	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|text()|comment()|processing-instruction()"/>
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>

