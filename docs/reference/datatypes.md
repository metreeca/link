Controlled set of standard [XSD](https://www.w3.org/TR/xmlschema-2/#built-in-datatypes) data types used in UML class definitions, internally mapped to implementation data types according to the following table.

| UML         | RDF                                                                | Java               | JSON                              | TypeScript | Description                        |
|-------------|--------------------------------------------------------------------|--------------------|-----------------------------------|------------|------------------------------------|
| boolean     | [xsd:boolean](https://www.w3.org/TR/xmlschema-2/#boolean)          | Boolean            |                                   | boolean    |                                    |
| integer     | [xsd:integer](https://www.w3.org/TR/xmlschema-2/#integer)          | BigInteger         | number                            | number     | arbitrary precision integer number |
| decimal     | [xsd:decimal](https://www.w3.org/TR/xmlschema-2/#decimal)          | BigDecimal         | number                            | number     | arbitrary precision decimal number |
| year        | [xsd:gYear](https://www.w3.org/TR/xmlschema-2/#gYear)              | Year               | `“yyyy”`                          | number     | absolute ISO 8601 year (`yyyy`)    |
| date        | [xsd:date](https://www.w3.org/TR/xmlschema-2/#date)                | LocalDate          | `“yyyy-MM-dd"`                    | Date       | local ISO 8601 date (`yyyy-MM-dd`) |
| time        | [xsd:time](https://www.w3.org/TR/xmlschema-2/#time)                | LocalTime          | "`hh:mm:ss.sss`"                  |            | local ISO 8601 time                |
|             |                                                                    | OffsetTime         | `"hh:mm:ss.sss+hh:mm"`            |            | offset ISO 8601 time               |
| dateTime    | [xsd:dateTime](https://www.w3.org/TR/xmlschema-2/#dateTime)        | LocalDateTime      | "`yyyy-MM-ddThh:mm:ss.sss`"       |            | local ISO 8601 date-time           |
|             |                                                                    | OffsetDateTime     | "`yyyy-MM-ddThh:mm:ss.sss+hh:mm`" |            | offset ISO 8601 date-time          |
| instant     | [xsd:dateTime](https://www.w3.org/TR/xmlschema-2/#dateTime)        | Instant            | "`yyyy-MM-ddThh:mm:ss.sssZ`"      |            | UTC ISO 8601 date-time             |
| duration    | [xsd:duration](https://www.w3.org/TR/xmlschema-2/#duration)        | Period             | `"PyYMMdD"`                       |            | date-based ISO 8601 time amount    |
|             |                                                                    | Duration           | `"PdDThHmMs.sssS"`                |            | time-based ISO 8601 time amount    |
| *interval*  |                                                                    |                    |                                   |            |                                    |
| uri         | [xsd:anyURI](https://www.w3.org/TR/xmlschema-2/#anyURI)            | URI                | `“{uri}”`                         |            | absolute/relative URI              |
| string      | [xsd:string](https://www.w3.org/TR/xmlschema-2/#string)            | String             |                                   | string     |                                    |
| local       | [rdfs:langString](https://www.w3.org/TR/rdf-schema/#ch_langstring) | Local<String>      |                                   |            | single-valued localized text       |
|             |                                                                    | Local<Set<String>> |                                   |            | multi-valued localized text        |
| enumeration |                                                                    | Enum<T>            |                                   |            | closed value set                   |
| entry       |                                                                    |                    | `{ id: “{id}”, … }`               |            |                                    |