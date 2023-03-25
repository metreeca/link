[![Maven Central](https://img.shields.io/maven-central/v/com.metreeca/rest.svg)](https://search.maven.org/artifact/com.metreeca/rest/)

# Metreeca/REST

Metreeca/REST is a model‑driven Java framework for rapid REST/JSON‑LD API development.

Its engines automatically convert annotated JavaBean classes and high-level declarative JSON-LD models into extended REST
APIs supporting data validation, CRUD operations, faceted search, relieving backend developers from low-level chores and
completely shielding frontend developers from linked data technicalities.

> ❗client-driven templates on a par with GraphQL

> ❗️NoSQL storage adapters (annotation/schema-driven)

> ❗️JavaBeans support

# Documentation

> ❗️TBD

# Modules

|                 area | javadocs                                                     | description                                     |
| -------------------: | :----------------------------------------------------------- | :---------------------------------------------- |
|            framework | [rest-core](https://javadoc.io/doc/com.metreeca/rest-core)   | JSON-LD data model                              |
|   wire format codecs | [rest-jsonld](https://javadoc.io/doc/com.metreeca/rest-jsonld) | JSON-LD wire format codec                       |
| data storage engines | [rest‑rdf4j](https://javadoc.io/doc/com.metreeca/rest-rdf4j) | [RDF4J](https://rdf4j.org) graph storage engine |

# Getting Started

1. Add the framework to your Maven configuration

```xml 
<project>

    <dependencyManagement>
        <dependencies>

            <dependency>
                <groupId>com.metreeca</groupId>
                <artifactId>rest</artifactId>
                <version>0.1-SNAPSHOT</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

        </dependencies>
    </dependencyManagement>

    <dependencies>

        …

    </dependencies>

</project>
```

2. ❗️TBD

4. Delve into the [docs](https://metreeca.github.io/rest/) to learn how
   to [publish](http://metreeca.github.io/rest/tutorials/publishing-jsonld-apis)
   and [consume](https://metreeca.github.io/rest/tutorials/consuming-jsonld-apis) your data as model-driven REST/JSON‑LD
   APIs…

# Support

- open an [issue](https://github.com/metreeca/rest/issues) to report a problem or to suggest a new feature
- start a [discussion](https://github.com/metreeca/rest/discussions) to ask a how-to question or to share an idea

# License

This project is licensed under the Apache 2.0 License – see [LICENSE](https://github.com/metreeca/rest/blob/main/LICENSE)
file for details.

