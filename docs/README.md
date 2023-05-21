[![Maven Central](https://img.shields.io/maven-central/v/com.metreeca/link.svg)](https://central.sonatype.com/artifact/com.metreeca/link/0.52.0/versions)

# Metreeca/Link

Metreeca/Link is a model‑driven Java framework for rapid REST/JSON‑LD backend development.

Its engines automatically convert annotated JavaBean classes and high-level declarative JSON-LD models into extended REST
engines supporting data validation, CRUD operations, faceted search and analytical queries, relieving backend developers
from low-level chores and completely shielding frontend developers from linked data technicalities.

> ❗client-driven models on a par with GraphQL

> ❗️NoSQL storage adapters (annotation/schema-driven)

> ❗️JavaBeans support

# Documentation

> ❗️TBD

# Modules

|                 area | javadocs                                                       | description                                     |
|---------------------:|:---------------------------------------------------------------|:------------------------------------------------|
|            framework | [link-core](https://javadoc.io/doc/com.metreeca/link-core)     | JSON-LD data model                              |
|   wire format codecs | [link-jsonld](https://javadoc.io/doc/com.metreeca/link-jsonld) | JSON-LD wire format codec                       |
| data storage engines | [link‑rdf4j](https://javadoc.io/doc/com.metreeca/link-rdf4j)   | [RDF4J](https://rdf4j.org) graph storage engine |

# Getting Started

1. Add the framework to your Maven configuration

```xml 

<project>

    <dependencyManagement>
        <dependencies>

            <dependency>
                <groupId>${project.groupId}</groupId>
                <artifactId>${project.artifactId}</artifactId>
                <version>${revision}</version>
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

4. Delve into the [docs](https://metreeca.github.io/link/) to learn how
   to [publish](http://metreeca.github.io/link/tutorials/publishing-jsonld-apis)
   and [consume](https://metreeca.github.io/link/tutorials/consuming-jsonld-apis) your data as model-driven REST/JSON‑LD
   APIs…

# Support

- open an [issue](https://github.com/metreeca/link/issues) to report a problem or to suggest a new feature
- start a [discussion](https://github.com/metreeca/link/discussions) to ask a how-to question or to share an idea

# License

This project is licensed under the Apache 2.0 License – see [LICENSE](https://github.com/metreeca/link/blob/main/LICENSE)
file for details.

