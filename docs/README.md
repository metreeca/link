[![Maven Central](https://img.shields.io/maven-central/v/com.metreeca/kona.svg)](https://search.maven.org/artifact/com.metreeca/kona/)

# Metreeca/Kona

Metreeca/Kona is a model‑driven Java framework for rapid REST/JSON‑LD development.

Its engines automatically convert annotated JavaBean classes and high-level declarative JSON-LD models into extended REST APIs supporting data validation, CRUD operations, faceted search, relieving backend developers from low-level chores and completely shielding frontend developers from linked data technicalities.

> ❗️NoSQL storage adapters (annotation/schema-driven)

# Documentation

> ❗️TBD

# Modules

|             area | javadocs                                                     | description |
|-----------------:|:-------------------------------------------------------------|:------------|
|             core | [kona-core](https://javadoc.io/doc/com.metreeca/kona-core)   | ❗️          |
|    JSON bindings | [kona-gson](https://javadoc.io/doc/com.metreeca/kona-json)   | ❗️          |
| storage adapters | [kona‑rdf4j](https://javadoc.io/doc/com.metreeca/kona-rdf4j) | ❗️          |

# Getting Started

1. Add the framework to your Maven configuration

```xml 
<project>

    <dependencyManagement>
        <dependencies>

            <dependency>
                <groupId>${project.groupId}</groupId>
                <artifactId>${project.artifactId}</artifactId>
                <version>${project.version}</version>
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

4. Delve into the [docs](https://metreeca.github.io/kona/) to learn how
   to [publish](http://metreeca.github.io/kona/tutorials/publishing-jsonld-apis)
   and [consume](https://metreeca.github.io/kona/tutorials/consuming-jsonld-apis) your data as model-driven REST/JSON‑LD
   APIs…

# Support

- open an [issue](https://github.com/metreeca/kona/issues) to report a problem or to suggest a new feature
- start a [discussion](https://github.com/metreeca/kona/discussions) to ask a how-to question or to share an idea

# License

This project is licensed under the Apache 2.0 License – see [LICENSE](https://github.com/metreeca/kona/blob/main/LICENSE)
file for details.

